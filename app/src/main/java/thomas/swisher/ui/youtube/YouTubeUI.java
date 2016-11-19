package thomas.swisher.ui.youtube;


import android.util.Log;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import java.util.LinkedList;
import java.util.List;

import thomas.swisher.BuildConfig;
import thomas.swisher.ui.MainActivity;
import thomas.swisher.ui.MainActivityLayout;
import thomas.swisher.ui.model.UIModel;
import thomas.swisher.utils.AsyncProxy;
import thomas.swisher.youtube.YouTubeEventBus;

import static thomas.swisher.ui.MainActivity.YOUTUBE_RECOVERY_REQUEST;

public class YouTubeUI {

    private final AsyncProxy.MethodInvocationListener proxyListener;
    private MainActivity activity;
    private UIModel.CoreModel model;
    private boolean initialising = false;
    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private YouTubePlayer youTubePlayer = null;
    private List<WithPlayer> pending = new LinkedList<>();

    private final YouTubeEventBus.YouTubePlayerRemote listener = new YouTubeEventBus.YouTubePlayerRemote() {
        @Override
        public void actionCommand(YouTubeEventBus.Action action) {
            switch(action) {
                case Pause:
                    withYouTubePlayer(player -> {
                        player.pause();
                        sendProgress();
                    } );
                    break;
                case Play:
                    withYouTubePlayer(player -> player.play());
                    break;
                case CueStart:
                    withYouTubePlayer(player -> {
                        player.pause();
                        player.seekToMillis(0);
                    });
                    break;
                case Clear:
                    safePause();
                    model.updateYouTubeVisible(false);
                    break;
            }
        }

        @Override
        public void playCommand(String videoID, boolean playNow) {
            withYouTubePlayer(player -> {
                if (playNow) {
                    player.loadVideo(videoID, 0);
                } else {
                    player.cueVideo(videoID, 0);
                }
            });
        }

        @Override
        public void seekTo(int toMillis) {
            withYouTubePlayer(player -> player.seekToMillis(toMillis));
        }

        @Override
        public void progressUpdate() {
            sendProgress();
        }
    };

    private void safePause() {
        if (youTubePlayer != null) {
            try {
                youTubePlayer.pause();
            } catch (IllegalStateException e) {
                //handles "YouTubePlayer has been released" error
            }
        }
    }

    private final UIModel.FullScreenListener fullScreenListener = (isFullScreen) -> {
        if (youTubePlayer != null) {
            youTubePlayer.setFullscreen(isFullScreen);
        }
    };

    public YouTubeUI(MainActivity activity, UIModel.CoreModel model) {
        this.activity = activity;
        this.model = model;
        this.proxyListener = new AsyncProxy.MethodInvocationListener(listener, YouTubeEventBus.eventBus);
        model.addFullScreenListener(fullScreenListener);
    }

    public void destroy() {
        proxyListener.finish();
        model.removeFullScreenListener(fullScreenListener);
    }

    public void retryInit() {
        if (youTubePlayer == null) {
            youTubePlayerFragment.initialize(
                BuildConfig.YOU_TUBE_PLAYER_API_KEY,
                onInitialiseListener);
        }
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
            try {
                action.invoke(youTubePlayer);
            } catch (IllegalStateException e) {
                //handles "YouTubePlayer has been released" error
                youTubePlayer = null;
                pending.add(action);
                retryInit();
            }
        }
    }

    private void send(Object update) {
        YouTubeEventBus.eventBus.post(update);
    }

    private void sendProgress() {
        sendProgress(false);
    }
    private void sendProgress(boolean isBuffering) {
        withYouTubePlayer(player -> {
            int duration = player.getDurationMillis();
            int position = player.getCurrentTimeMillis();
            send(new YouTubeEventBus.YouTubeProgressUpdate(!isBuffering && player.isPlaying(), duration, position));
        });
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

            player.setOnFullscreenListener((fullScreen) -> model.updateFullScreen(fullScreen));

            player.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                @Override public void onBuffering(boolean isBuffering) { sendProgress(isBuffering); }
                @Override public void onPaused() { send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.Status.Paused)); }
                @Override public void onPlaying() { send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.Status.Playing)); }
                @Override public void onSeekTo(int seekTo) { sendProgress(); }
                @Override public void onStopped() {}
            });

            player.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
                @Override public void onAdStarted() { }
                @Override public void onError(YouTubePlayer.ErrorReason reason) {
                    send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.Status.Error));
                    Log.e("SWISHER", "YouTube error: " + reason.name());
                }
                @Override public void onLoaded(String videoID) { sendProgress(); }
                @Override public void onLoading() { }
                @Override public void onVideoEnded() {
                    send(new YouTubeEventBus.YouTubeStatusUpdate(YouTubeEventBus.Status.Ended));
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
