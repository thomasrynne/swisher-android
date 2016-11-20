package thomas.swisher.youtube;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import thomas.swisher.MediaHandler;
import thomas.swisher.service.player.TracksPlayer;
import thomas.swisher.shared.Core;
import thomas.swisher.utils.AsyncProxy;

/**
 *
 */

public class YouTubeTracksPlayer implements TracksPlayer {

    private final AsyncProxy.MethodInvocationListenerForBackground<YouTubeEventBus.YouTubePlayerCallback> handler = YouTubeEventBus.createYouTubePlayerCallbackListener(
        new YouTubeEventBus.YouTubePlayerCallback() {
            @Override
            public void status(YouTubeEventBus.Status status) {
                switch(status) {
                    case Ended:   listener.notify(MediaHandler.PlayerNotification.Finished); break;
                    case Paused:  listener.notify(MediaHandler.PlayerNotification.Paused);   break;
                    case Playing: listener.notify(MediaHandler.PlayerNotification.Playing);  break;
                    case Error:   listener.notify(MediaHandler.PlayerNotification.Failed);   break;
                }
            }

            @Override
            public void progress(boolean isPlaying, int durationMillis, int positionMillis) {
                listener.currentProgress(isPlaying, new Core.PlayerProgress(durationMillis, positionMillis, true));
            }
        });

    private final MediaHandler.PlayerListener listener;
    private final YouTubeEventBus.YouTubePlayerRemote youTubeRemote;

    public YouTubeTracksPlayer(boolean playNow, String videoID, MediaHandler.PlayerListener listener) {
        this.listener = listener;
        this.youTubeRemote = YouTubeEventBus.createYouTubeRemoteProxy();
        this.youTubeRemote.playCommand(videoID, playNow);
    }

    @Override
    public void cueBeginning() {
        youTubeRemote.actionCommand(YouTubeEventBus.Action.CueStart);
    }

    @Override
    public void stop() {
        youTubeRemote.actionCommand(YouTubeEventBus.Action.Pause);
    }

    @Override
    public void pausePlay(boolean play) {
        youTubeRemote.actionCommand(play ?
            YouTubeEventBus.Action.Play:
            YouTubeEventBus.Action.Pause);
    }

    @Override
    public void clear() {
        youTubeRemote.actionCommand(YouTubeEventBus.Action.Clear);
        handler.finish();
    }

    @Override
    public void playNext(boolean play) {

    }

    @Override
    public void jumpTo(int track) {

    }

    @Override
    public void seekTo(int toMillis) {
        youTubeRemote.seekTo(toMillis);
    }

    @Override
    public void requestProgressUpdate() {
        youTubeRemote.progressUpdate();
    }
}
