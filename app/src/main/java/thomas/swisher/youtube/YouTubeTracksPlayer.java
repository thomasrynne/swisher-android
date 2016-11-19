package thomas.swisher.youtube;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import thomas.swisher.MediaHandler;
import thomas.swisher.service.player.TracksPlayer;
import thomas.swisher.shared.Core;
import thomas.swisher.utils.AsyncProxy;

import static thomas.swisher.youtube.YouTubeEventBus.eventBus;

/**
 *
 */

public class YouTubeTracksPlayer implements TracksPlayer {

    private final Object eventBusListener = new Object() {
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(YouTubeEventBus.YouTubeStatusUpdate update) {
            switch(update.status) {
                case Ended:   listener.notify(MediaHandler.PlayerNotification.Finished); break;
                case Paused:  listener.notify(MediaHandler.PlayerNotification.Paused);   break;
                case Playing: listener.notify(MediaHandler.PlayerNotification.Playing);  break;
                case Error:   listener.notify(MediaHandler.PlayerNotification.Failed);   break;
            }
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(YouTubeEventBus.YouTubeProgressUpdate update) {
            listener.currentProgress(update.isPlaying, new Core.PlayerProgress(update.durationMillis, update.positionMillis, true));
        }
    };

    private final MediaHandler.PlayerListener listener;
    private final YouTubeEventBus.YouTubePlayerRemote youTubeRemote;
    public YouTubeTracksPlayer(boolean playNow, String videoID, MediaHandler.PlayerListener listener) {
        this.listener = listener;
        this.youTubeRemote = AsyncProxy.create(YouTubeEventBus.YouTubePlayerRemote.class, eventBus);
        eventBus.register(eventBusListener);
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
        eventBus.unregister(eventBusListener);
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
