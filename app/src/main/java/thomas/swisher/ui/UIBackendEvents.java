package thomas.swisher.ui;

import com.google.common.base.Optional;

import java.util.List;

import lombok.Value;
import uk.co.thomasrynne.swisher.Utils;
import thomas.swisher.shared.Core;

/**
 * The events which the UI Backend class sends to the service
 */
public class UIBackendEvents {

    private UIBackendEvents(){}

    @Value
    public static class MenuResponse {
        public final Core.MenuPath menuPath;
        public final Optional<Core.MenuItemList> menuItemList;
    }

    //Server -> GUI (*Latest)

    @Value
    public static class TracksLatest {
        public boolean isPlaying;
        public final List<Core.PlaylistEntry> tracks;
    }


    //GUI -> Service (*Event)

    @Value
    public static class RequestMenuEvent {
        public final Core.MenuPath menuPath;
    }

    @Value
    public static class CancelMenuEvent {
        public final Core.MenuPath menuPath;
    }

    public static class PausePlayEvent {}

    @Value
    public static class PlayTrackEvent {
        public final Utils.FlatJson json;
    }

    @Value
    public static class AddTrackEvent {
        public final Utils.FlatJson json;
    }

    public static class ClearPlayListEvent { }

    @Value
    public static class PlayTrackByIndexEvent {
        public final int group;
        public final int track;
    }

    @Value
    public static class SwapTracksEvent {
        public final int fromPosition;
        public final int toPosition;
    }

    @Value
    public static class RemoveTrackEvent {
        public final int position;
    }

}
