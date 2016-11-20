package thomas.swisher.youtube;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import lombok.Value;
import thomas.swisher.utils.AsyncProxy;

/**
 *
 */
public class YouTubeEventBus {

    private static final EventBus eventBus = EventBus.builder().build();

    public static YouTubePlayerCallback createYouTubePlayerCallbackProxy() {
        return AsyncProxy.create(YouTubeEventBus.YouTubePlayerCallback.class, eventBus);
    }

    public static AsyncProxy.MethodInvocationListenerForBackground<YouTubePlayerCallback>
            createYouTubePlayerCallbackListener(YouTubePlayerCallback listener) {
        return new AsyncProxy.MethodInvocationListenerForBackground<>(listener, eventBus);
    }

    public static YouTubePlayerRemote createYouTubeRemoteProxy() {
        return AsyncProxy.create(YouTubeEventBus.YouTubePlayerRemote.class, eventBus);
    }

    public static AsyncProxy.MethodInvocationListenerForMain<YouTubePlayerRemote>
            createYouTubeRemoteListener(YouTubePlayerRemote listener) {
        return new AsyncProxy.MethodInvocationListenerForMain<>(listener, eventBus);
    }

    /** For sending messages to the You Tube player */
    public enum Action { Pause, Play, CueStart, Clear }
    public interface YouTubePlayerRemote {
        void actionCommand(YouTubeEventBus.Action action);
        void playCommand(String videoID, boolean playNow);
        void seekTo(int toMillis);
        void progressUpdate();
    }

    /* For the You Tube player to send messages to the YouTubeTracksPlayer */
    public enum Status { Paused, Playing, Ended, Error }
    public interface YouTubePlayerCallback {
        void status(Status status);
        void progress(boolean isPlaying, int durationMillis, int positionMillis);
    }
}
