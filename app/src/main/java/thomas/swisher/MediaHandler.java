package thomas.swisher;

import android.net.Uri;

import com.google.common.base.Optional;

import java.util.List;

import lombok.Value;
import thomas.swisher.shared.Core;
import thomas.swisher.utils.Utils;
import thomas.swisher.service.player.TracksPlayer;

/**
 */
public interface MediaHandler {

    public Optional<PlaylistEntry> handle(Utils.FlatJson json);

    public enum PlayerNotification { Paused, Playing, Finished }
    public interface PlayerListener {
        //Informs the main player that this player has paused/playing/finished all tracks
        public void notify(PlayerNotification notification);
        //Informs the main player that this player has moved onto the next track in its list
        public void onTrack(boolean autoPlayedThisTrack, int position);
        //Updates the main player with the current progress of the track
        //This should be called each time a new track starts and on pause/play events
        //The ui will assume that when a track is playing progress proceeds at 1ms per ms ;)
        public void currentProgress(Core.PlayerProgress progress);
    }

    public interface ThePlayer {
        public TracksPlayer create(boolean playNow, int currentTrack, boolean playNext, PlayerListener listener);
    }

    @Value
    public static class PlaylistEntry {
        public String name;
        public Optional<Uri> thumbnail;
        public List<? extends TrackDescription> tracks;
        public ThePlayer player;
    }

    public interface TrackDescription {
        public String name();
        public Optional<Uri> image();
    }

}
