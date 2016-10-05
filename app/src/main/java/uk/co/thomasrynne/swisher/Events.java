package uk.co.thomasrynne.swisher;

import android.net.Uri;

import org.json.JSONObject;

import java.util.List;

import lombok.Value;
import lombok.val;
import uk.co.thomasrynne.swisher.core.Player;

/**
 */
public class Events {

    @Value // Used by the service to display messages
    public static class ToastEvent {
        public final String message;
    }

    @Value
    public static class CardWaveEvent {
        public final String cardNumber;
    }

    @Value
    public static class RecordMode {
        public final String name;
    }

    public static class RecordCompleteEvent {}

    public static class CancelRecordEvent {}

    public static class RecordPlayListEvent { }

    public static class RefreshAlbumCoversEvent { }

    @Value
    public static class ActivityReadyEvent {
        public final boolean isActivityReady;
        public final boolean isYouTubeReady;
    }

    @Value
    public static class RecordCardEvent {
        public final String name;
        public final Utils.FlatJson json;
    }

    @Value
    public static class DoEvent {
        public final Utils.FlatJson json;
    }


    public static class ServiceStarted {}
}

