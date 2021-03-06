package thomas.swisher;

import android.app.Service;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

import lombok.val;
import thomas.swisher.utils.Utils;
import thomas.swisher.service.player.Player;
import thomas.swisher.service.ScreenOn;

/**
 */
public class JsonEventHandler {

    public interface Handler {
        boolean handle(Utils.FlatJson json);
    }

    private final Player player;
    private final FluentIterable<MediaHandler> mediaHandlers;
    private final ScreenOn screenOn;

    public JsonEventHandler(Service service, Player player, MediaHandler[] handlers) {
        this.screenOn = new ScreenOn(service);
        this.player = player;
        this.mediaHandlers = FluentIterable.of(handlers);
    }

    public void add(Utils.FlatJson json) {
        val playlist = tracksFor(json);
        if (playlist.isPresent()) {
            player.add(playlist.get());
        }
    }

    public List<Player.PlaylistEntry> playlistEntries(List<Utils.FlatJson> entries) {
        return FluentIterable.from(entries).
                transformAndConcat((item) -> tracksFor(item).asSet()).toList();
    }

    public void play(Utils.FlatJson json) {
        val playlist = tracksFor(json);
        if (playlist.isPresent()) {
            player.play(Collections.singletonList(playlist.get()));
        }
    }

    public final Handler jsonHandler = new Handler() {
        @Override
        public boolean handle(Utils.FlatJson json) {
            for (MiniHandler handler: handlers) {
                if (handler.handle(json)) {
                    return true;
                }
            }
            return false;
        }
    };

    public Utils.FlatJson playListJson() {
        List<Utils.FlatJson> items = player.playlistJson();
        return Utils.json().add("playlist", items).build();
    }

    private void play(List<Player.PlaylistEntry> tracks) {
        player.play(tracks);
    }

    private Optional<Player.PlaylistEntry> tracksFor(Utils.FlatJson json) {
        val x = mediaHandlers.transformAndConcat( (MediaHandler handler) -> handler.handle(json).asSet() ).first();
        return x.transform( (e) -> Player.PlaylistEntry.create(json, e));
    }

    private interface MiniHandler {
        boolean handle(Utils.FlatJson json);
    }

    private final MiniHandler actionsHandler = new MiniHandler()  {
        @Override
        public boolean handle(Utils.FlatJson json) {
            if (json.has("action")) {
                val action = json.get("action");
                switch (action) {
                    case "stop":     player.stop();      return true;
                    case "pause":    player.pausePlay(); return true;
                }
            }
            return false;
        }
    };

    private final MiniHandler playlistHandler = new MiniHandler() {
        @Override
        public boolean handle(Utils.FlatJson json) {
            if (json.has("playlist")) {
                List<Player.PlaylistEntry> playlistItems = playlistEntries(json.getList("playlist"));
                player.play(playlistItems);
                screenOn.wakeUp();
                return true;
            } else {
                return false;
            }
        }
    };

    private final MiniHandler playHandler = new MiniHandler() {
        @Override
        public boolean handle(Utils.FlatJson json) {
            val playlist = tracksFor(json);
            if (playlist.isPresent()) {
                play(Collections.singletonList(playlist.get()));
                screenOn.wakeUp();
                return true;
            } else {
                return false;
            }
        }
    };

    private final MiniHandler[] handlers = new MiniHandler[] {
        actionsHandler,
        playlistHandler,
        playHandler
    };
}
