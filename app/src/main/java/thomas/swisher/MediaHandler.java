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

    public interface PlayerListener {
        public void finished();
        public void  onTrack(int position, boolean isPlaying, Core.PlayerProgress progress);
    }

    public interface ThePlayer {
        public TracksPlayer create(boolean playNow, boolean playNext, PlayerListener listener);
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
