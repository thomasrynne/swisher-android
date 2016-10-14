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

    public interface TracksChangeListener {
        public void trackChanged();
    }

    public static class Model {

        private UIModel.CoreModel core;
        private List<PlaylistEntry> playlist = new ArrayList<>();
        private Optional<Uri> currentTrackImage = Optional.absent();
        private TracksChangeListener listener;
        private boolean collapsed = false;
        private List<Core.PlaylistEntry> tracks = Collections.emptyList();
        private int currentGroup = -1;
        private int currentTrackInGroup = -1;

        @Value
        public class PlaylistEntry {
            public final long itemID;
            public final int group;
            public final int track;
            public final Optional<String> topText;
            public final Optional<Uri> thumbnail;
            public final Optional<String> trackName;
            public boolean isCurrentTrack() {
                return group==currentGroup && track == currentTrackInGroup;
            }
        }

        public Model(UIModel.CoreModel core) {
            this.core = core;
        }

        public boolean nonEmpty() {
            return !playlist.isEmpty();
        }

        public void playTrackAt(int index) {
            PlaylistEntry entry = trackAt(index).get();
            //updating the UI model so that the track is marked as playing straight away
            //without this there is a 20ms - 300ms delay
            updateCurrentTrack(entry.group, entry.track);
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

        public void latest(List<Core.PlaylistEntry> tracks, int currentGroup, int currentTrackInGroup) {
            this.tracks = tracks;
            updateCurrentTrack(currentGroup, currentTrackInGroup);
            rebuild();
        }

        private void updateCurrentTrack(int currentGroup, int currentTrackInGroup) {
            this.currentGroup = currentGroup;
            this.currentTrackInGroup = currentTrackInGroup;
            this.currentTrackImage = tracks.get(currentGroup).getTracks().get(currentTrackInGroup).image;
        }

        public void rebuild() {
            collapsed = core.showMenu() && tracks.size() > 1;
            playlist.clear();
            if (collapsed) {
                int group = 0;
                for (Core.PlaylistEntry entry: tracks) {
                    playlist.add(new PlaylistEntry(
                        entry.id * 1000, group, 0, Optional.of(entry.getName()), entry.thumbnail,
                        Optional.absent()));
                    group++;
                }
            } else {
                int group = 0;
                for (Core.PlaylistEntry entry : tracks) {
                    val firstTrack = entry.tracks.get(0);
                    playlist.add(new PlaylistEntry(
                            entry.id * 1000, group, 0, Optional.of(entry.getName()), entry.thumbnail,
                            Optional.of(firstTrack.name)));
                    for (int i = 1; i < entry.tracks.size(); i++) {
                        val track = entry.tracks.get(i);
                        playlist.add(new PlaylistEntry(
                                entry.id + i, group, i, Optional.absent(), Optional.absent(),
                                Optional.of(track.name)));
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
