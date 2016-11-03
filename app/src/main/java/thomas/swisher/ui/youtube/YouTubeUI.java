package thomas.swisher.ui.youtube;


import android.util.Log;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.List;

import thomas.swisher.BuildConfig;
import thomas.swisher.ui.MainActivity;
import thomas.swisher.ui.MainActivityLayout;
import thomas.swisher.ui.model.UIModel;
import thomas.swisher.youtube.YouTubeEventBus;

public class YouTubeUI {

    public static final int YOUTUBE_RECOVERY_REQUEST = 999;

    private MainActivity activity;
    private UIModel.CoreModel model;
    private boolean initialising = false;
    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private YouTubePlayer youTubePlayer = null;
    private List<WithPlayer> pending = new LinkedList<>();

    private final Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEvent(YouTubeEventBus.YouTubeControlCommand command) {
            switch(command.action) {
                case Pause:
                    withYouTubePlayer(player -> player.pause());
                    break;
                case PausePlay:
                    withYouTubePlayer(player -> {
                        if (player.isPlaying()) {
                            player.pause();
                        } else {
                            player.play();
                        }
                    });
                    break;
                case CueStart:
                    withYouTubePlayer(player -> {
                        player.pause();
                        player.seekToMillis(0);
                    });
                    break;
                case Clear:
                    if (youTubePlayer != null) {
                        youTubePlayer.pause();
                    }
                    model.updateYouTubeVisible(false);
                    break;
            }
        }
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEvent(YouTubeEventBus.YouTubePlayCommand command) {
            switch (command.action) {
                case CueFromStart:
                    withYouTubePlayer(player -> {
                        player.cueVideo(command.videoID, 0);
                    });
                    break;
                case PlayFromStart:
                    withYouTubePlayer(player -> {
                        player.loadVideo(command.videoID, 0);
                    });
                    break;
            }
        }
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEvent(YouTubeEventBus.YouTubeSeekCommand command) {
            withYouTubePlayer(player -> player.seekToMillis(command.toMillis));
        }
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEvent(YouTubeEventBus.YouTubeProgressUpdateCommand command) {
            withYouTubePlayer(player -> sendProgress(player));
        }
    };

    public YouTubeUI(MainActivity activity, UIModel.CoreModel model) {
        this.activity = activity;
        this.model = model;
        YouTubeEventBus.eventBus.register(listener);
    }

    public void destroy() {
        YouTubeEventBus.eventBus.unregister(listener);
    }

    interface WithPlayer {
        void invoke(YouTubePlayer player);
    }

    @SuppressWarnings("ResourceType")
    private void withYouTubePlayer(WithPlayer action) {
        if (initialising) {
            pending.add(action);
        } else if (youTubePlayer == null) {
            initialising = true;
            youTubePlayerFragment = new YouTubePlayerSupportFragment();
            youTubePlayerFragment.initialize(
                BuildConfig.YOU_TUBE_PLAYER_API_KEY,
                onInitialiseListener);
            activity.getSupportFragmentManager().beginTransaction().add(
                MainActivityLayout.YOUTUBE_FRAME_LAYOUT_ID,
                youTubePlayerFragment).commit();
            pending.add(action);
        } else {
            model.updateYouTubeVisible(true);
            action.invoke(youTubePlayer);
        }
    }

    private void send(Object update) {
        YouTubeEventBus.eventBus.post(update);
    }

    private void sendProgress(YouTubePlayer player) {
        int duration = player.getDurationMillis();
        int position = player.getCurrentTimeMillis();
        send(new YouTubeEventBus.YouTubeProgressUpdate(duration, position));
    }

    private OnInitializedListener onInitialiseListener = new OnInitializedListener() {
        @Override public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult error) {
            initialising = false;
            Log.e("SWISHER", "youtube failed : " + error.toString() + " " + error.isUserRecoverableError());
            if (error.isUserRecoverableError()) {
                error.getErrorDialog(activity, YOUTUBE_RECOVERY_REQUEST).show();
            } else {
                Toast.makeText(activity, "Oh no! " + error.toString(), Toast.LENGTH_LONG).show();
            }
        }
        @Override public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
            initialising = false;
            youTubePlayer = player;
            player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
            player.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                @Override public void onBuffering(boolean arg0) { }
                @Override public void onPaused() { send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.YouTubeStatusUpdate.Status.Paused)); }
                @Override public void onPlaying() { send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.YouTubeStatusUpdate.Status.Playing)); }
                @Override public void onSeekTo(int seekTo) { sendProgress(player); }
                @Override public void onStopped() {}
            });
            player.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
                @Override public void onAdStarted() { }
                @Override public void onError(YouTubePlayer.ErrorReason reason) {
                    Log.e("SWISHER", "YouTube error: " + reason.name());
                }
                @Override public void onLoaded(String videoID) { sendProgress(player); }
                @Override public void onLoading() { }
                @Override public void onVideoEnded() {
                    send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.YouTubeStatusUpdate.Status.Ended));
                }
                @Override public void onVideoStarted() {  }
            });
            for (WithPlayer action: pending) {
                action.invoke(youTubePlayer);
            }
            pending.clear();
        }
    };

}
