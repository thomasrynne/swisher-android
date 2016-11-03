package thomas.swisher.youtube;

import org.greenrobot.eventbus.EventBus;

import lombok.Value;

/**
 *
 */

public class YouTubeEventBus {

    public static EventBus eventBus = EventBus.builder().build();

    @Value
    public static class YouTubeStatusUpdate {
        public enum Status { Paused, Playing, Ended }
        public final Status status;
    }

    @Value
    public static class YouTubeProgressUpdate {
        public final int durationMillis;
        public final int positionMillis;
    }

    @Value
    public static class YouTubeControlCommand {
        public enum Action { Pause, PausePlay, CueStart, Clear }
        public final Action action;
    }

    @Value
    public static class YouTubePlayCommand {
        public enum Action { PlayFromStart,CueFromStart }
        public final Action action;
        public final String videoID;
    }

    @Value
    public static class YouTubeSeekCommand {
        public final int toMillis;
    }

    public static class YouTubeProgressUpdateCommand {
    }
}
