package thomas.swisher.youtube;

import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import lombok.val;
import thomas.swisher.MediaHandler;
import thomas.swisher.service.player.TracksPlayer;
import thomas.swisher.shared.Core;

import static thomas.swisher.youtube.YouTubeEventBus.eventBus;

/**
 *
 */

public class YouTubeTracksPlayer implements TracksPlayer {

    private final Object eventBusListener = new Object() {
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(YouTubeEventBus.YouTubeStatusUpdate update) {
            switch(update.status) {
                case Ended: listener.notify(MediaHandler.PlayerNotification.Finished); break;
                case Paused: listener.notify(MediaHandler.PlayerNotification.Paused); break;
                case Playing: listener.notify(MediaHandler.PlayerNotification.Playing); break;
            }
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(YouTubeEventBus.YouTubeProgressUpdate update) {
            listener.currentProgress(new Core.PlayerProgress(update.durationMillis, update.positionMillis, true));
        }
    };

    private MediaHandler.PlayerListener listener;

    public YouTubeTracksPlayer(boolean playNow, String videoID, MediaHandler.PlayerListener listener) {
        this.listener = listener;
        eventBus.register(eventBusListener);
        val action = playNow ? YouTubeEventBus.YouTubePlayCommand.Action.PlayFromStart :
            YouTubeEventBus.YouTubePlayCommand.Action.CueFromStart;
        eventBus.post(new YouTubeEventBus.YouTubePlayCommand(action, videoID));
    }

    @Override
    public void cueBeginning() {
        eventBus.post(new YouTubeEventBus.YouTubeControlCommand(YouTubeEventBus.YouTubeControlCommand.Action.CueStart));
    }

    @Override
    public void stop() {
        eventBus.post(new YouTubeEventBus.YouTubeControlCommand(YouTubeEventBus.YouTubeControlCommand.Action.Pause));
    }

    @Override
    public void pausePlay() {
        eventBus.post(new YouTubeEventBus.YouTubeControlCommand(YouTubeEventBus.YouTubeControlCommand.Action.PausePlay));
    }

    @Override
    public void clear() {
        eventBus.post(new YouTubeEventBus.YouTubeControlCommand(YouTubeEventBus.YouTubeControlCommand.Action.Clear));
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
        eventBus.post(new YouTubeEventBus.YouTubeSeekCommand(toMillis));
    }

    @Override
    public void requestProgressUpdate() {
        eventBus.post(new YouTubeEventBus.YouTubeProgressUpdateCommand());
    }
}
