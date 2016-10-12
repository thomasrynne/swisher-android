package thomas.swisher.service.player;

import com.google.common.collect.FluentIterable;

import java.util.ArrayList;
import java.util.List;

import lombok.val;
import thomas.swisher.utils.Utils;

/**
 * Holds the current playlist and persists changes so that when the app is
 * restarted the previous playlist is still present
 */
public class CurrentPlaylist {

    private Store store;

    public interface Store {
        public void store(List<Utils.FlatJson> playlist);
    }

    public CurrentPlaylist(Store store) {
        this.store = store;
    }

    private void doStore() {
        val json = FluentIterable.from(playlist).transform(item -> item.json).toList();
        store.store(json);
    }

    private final List<Player.PlaylistEntry> playlist = new ArrayList<>();

    public int size() {
        return playlist.size();
    }

    public Player.PlaylistEntry get(int currentGroup) {
        return playlist.get(currentGroup);
    }

    public boolean isEmpty() {
        return playlist.isEmpty();
    }

    public List<Player.PlaylistEntry> toList() {
        return new ArrayList(playlist);
    }

    /**
     * Adds tracks to the playlist without storing it (used on startup)
     */
    public void initialise(List<Player.PlaylistEntry> tracks) {
        playlist.clear();
        playlist.addAll(tracks);
    }

    public void clear() {
        playlist.clear();
        doStore();
    }

    public void replace(List<Player.PlaylistEntry> tracks) {
        playlist.clear();
        playlist.addAll(tracks);
        doStore();
    }

    public void add(Player.PlaylistEntry tracks) {
        playlist.add(tracks);
        doStore();
    }

    public void remove(int groupPosition) {
        playlist.remove(groupPosition);
        doStore();
    }

    public void swap(int a, int b) {
        val aa = playlist.get(a);
        val bb = playlist.get(b);
        playlist.set(a, bb);
        playlist.set(b, aa);
        doStore();
    }
}
