package thomas.swisher.ui.model;

import android.net.Uri;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Value;
import lombok.val;
import thomas.swisher.shared.Core;

/**
 */
public class UITracks {

    @Value
    public static class PlaylistEntry {
        public final long itemID;
        public final int group;
        public final int track;
        public final Optional<String> topText;
        public final Optional<Uri> thumbnail;
        public final Optional<String> trackName;
        public final boolean isCurrentTrack;
    }

    public interface TracksChangeListener {
        public void trackChanged();
    }

    public static class Model {

        private UIModel.Core core;
        private List<PlaylistEntry> playlist = new ArrayList<>();
        private Optional<Uri> currentTrackImage = Optional.absent();
        private TracksChangeListener listener;
        private boolean collapsed = false;
        private List<Core.PlaylistEntry> tracks = Collections.emptyList();

        public Model(UIModel.Core core) {
            this.core = core;
        }

        public boolean nonEmpty() {
            return !playlist.isEmpty();
        }

        public void playTrackAt(int index) {
            PlaylistEntry entry = trackAt(index).get();
            core.playTrackAt(entry.group, entry.track);
        }

        public Optional<PlaylistEntry> trackAt(int index) {
            if (index < playlist.size()) {
                return Optional.of(playlist.get(index));
            } else {
                return Optional.absent();
            }
        }

        public int trackCount() {
            return playlist.size();
        }

        public void latest(List<Core.PlaylistEntry> tracks) {
            this.tracks = tracks;
            rebuild();
        }

        public void rebuild() {
            collapsed = core.showMenu() && tracks.size() > 1;
            playlist.clear();
            if (collapsed) {
                int group = 0;
                for (Core.PlaylistEntry entry: tracks) {
                    playlist.add(new PlaylistEntry(
                        entry.id * 1000, group, 0, Optional.of(entry.getName()), entry.thumbnail,
                        Optional.absent(), false));
                    for (int i = 0; i < entry.tracks.size(); i++) {
                        val track = entry.tracks.get(i);
                        if (track.isCurrentTrack) {
                            currentTrackImage = track.image;
                        }
                    }
                    group++;
                }
            } else {
                int group = 0;
                for (Core.PlaylistEntry entry : tracks) {
                    val firstTrack = entry.tracks.get(0);
                    playlist.add(new PlaylistEntry(
                            entry.id * 1000, group, 0, Optional.of(entry.getName()), entry.thumbnail,
                            Optional.of(firstTrack.name), firstTrack.isCurrentTrack));
                    if (firstTrack.isCurrentTrack) {
                        currentTrackImage = firstTrack.image;
                    }
                    for (int i = 1; i < entry.tracks.size(); i++) {
                        val track = entry.tracks.get(i);
                        playlist.add(new PlaylistEntry(
                                entry.id + i, group, i, Optional.absent(), Optional.absent(),
                                Optional.of(track.name), track.isCurrentTrack));
                        if (track.isCurrentTrack) {
                            currentTrackImage = track.image;
                        }
                    }
                    group++;
                }
            }
            listener.trackChanged();
        }

        public void onTracksChange(TracksChangeListener listener) {
            this.listener = listener;
        }

        public Optional<Uri> currentTrackImage() {
            return currentTrackImage;
        }

        public boolean enableDragAndDrop() {
            return collapsed;
        }

        public void remove(int position) {
            playlist.remove(position); //removing before backend update to prevent flicker
            listener.trackChanged();
            core.backend().removePlaylistItem(position);
        }

        public void swap(int a, int b) {
            val valueAtA = playlist.get(a);
            val valueAtB = playlist.get(b);
            playlist.set(b, valueAtA);
            playlist.set(a, valueAtB);
            listener.trackChanged();
            core.backend().swapPlaylistItems(a, b);
        }
    }
}
