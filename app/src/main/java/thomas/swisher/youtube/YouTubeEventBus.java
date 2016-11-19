package thomas.swisher.youtube;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import lombok.Value;

/**
 *
 */
public class YouTubeEventBus {

    public static EventBus eventBus = EventBus.builder().build();

    public enum Action { Pause, Play, CueStart, Clear }

    public interface YouTubePlayerRemote {
        void actionCommand(YouTubeEventBus.Action action);
        void playCommand(String videoID, boolean playNow);
        void seekTo(int toMillis);
        void progressUpdate();
    }


    public enum Status { Paused, Playing, Ended, Error }

    @Value
    public static class YouTubeStatusUpdate {
        public final Status status;
    }

    @Value
    public static class YouTubeProgressUpdate {
        public boolean isPlaying;
        public final int durationMillis;
        public final int positionMillis;
    }
}
