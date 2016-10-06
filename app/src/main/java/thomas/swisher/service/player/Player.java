package thomas.swisher.service.player;

import android.net.Uri;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Value;
import lombok.val;

import thomas.swisher.MediaHandler;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.utils.Utils;

/**
 * This class doesn't actually make sounds it just creates and controls the current TracksPlayer
 *  -manages the list of tracks
 *  -manages the play/paused state
 *  -broadcasts updates to playlist + pause/play status
 *  -manages the current 'active' player
 */
public class Player {

    @Value
    public static class PlaylistEntry {
        private static AtomicInteger sequence = new AtomicInteger(0); //a unique id is needed to support drag'n'drop reordering
        private int id;
        MediaHandler.PlaylistEntry entry;
        public Utils.FlatJson json;

        public String name() {
            return entry.getName();
        }

        public static PlaylistEntry create(Utils.FlatJson json, MediaHandler.PlaylistEntry entry) {
            return new PlaylistEntry(sequence.getAndIncrement(), entry, json);
        }

        public Optional<Uri> thumbnail() {
            return entry.thumbnail;
        }
    }

    private EventBus eventBus = EventBus.getDefault();
    private final AtomicInteger sequence = new AtomicInteger();
    private ArrayList<PlaylistEntry> tracksList = new ArrayList<>();
    private int currentPlayerInstance = -1;
    private int currentGroup = -1;
    private int currentTrackInGroup = -1;
    private boolean isPlaying = false;
    private boolean playNext = true;
    private TracksPlayer currentPlayer = new NullTracksPlayer();

    private class ListenerForPlayer implements MediaHandler.PlayerListener {
        private int instance;

        ListenerForPlayer(int instance) {
            this.instance = instance;
        }

        @Override
        public void finished() {
            if (Player.this.currentPlayerInstance == instance) {
                toNext();
            }
        }

        @Override
        public void onTrack(int position, boolean isPlaying) {
            if (Player.this.currentPlayerInstance == instance) {
                Player.this.currentTrackInGroup = position;
                Player.this.isPlaying = isPlaying;
                broadcastTrackList();
            }
        }
    }

    private void toNext() {
        if (tracksList.size() == 1) { //only one group, just cueBeginning start
            currentTrackInGroup = 0;
            isPlaying = false;
            currentPlayer.cueBeginning();
        } else if ((currentGroup+1) < tracksList.size()) { //there is a next group, play/cue it
            currentGroup++;
            currentTrackInGroup=0;
            initPlayer(tracksList.get(currentGroup).entry.player, playNext);
        } else { //we got to the end, cueBeginning the start group
            currentPlayer.clear();
            currentTrackInGroup = 0;
            currentGroup = 0;
            initPlayer(tracksList.get(0).entry.player, false);
        }
        broadcastTrackList();
    }

    private void initPlayer(MediaHandler.ThePlayer player, boolean playNow) {
        currentPlayer.clear();
        currentPlayerInstance = sequence.incrementAndGet();
        currentPlayer = player.create(playNow, playNext, new ListenerForPlayer(currentPlayerInstance));

    }

    public void play(List<Player.PlaylistEntry> tracks) {
        tracksList.clear();
        if (!tracks.isEmpty()) {
            initPlayer(tracks.get(0).entry.player, true);
            tracksList.addAll(tracks);
            isPlaying = true;
            currentGroup = 0;
            currentTrackInGroup = 0;
        } else {
            nullOutPlayer();
        }
        broadcastTrackList();
    }

    public void add(Player.PlaylistEntry tracks) {
        if (tracksList.isEmpty()) {
            currentGroup = 1;
            currentTrackInGroup = 1;
            initPlayer(tracks.entry.player, false);
        }
        tracksList.add(tracks);
        broadcastTrackList();
    }

    public void remove(int groupPosition) {
        tracksList.remove(groupPosition);
        if (currentGroup == groupPosition) {
            if (!tracksList.isEmpty()) {
                initPlayer(tracksList.get(0).entry.player, false);
                currentGroup = 0;
                currentTrackInGroup = 0;
                isPlaying = false;
            } else {
                nullOutPlayer();
            }
        }
        broadcastTrackList();
    }

    public void swap(int a, int b) {
        val aa = tracksList.get(a);
        val bb = tracksList.get(b);
        tracksList.set(a, bb);
        tracksList.set(b, aa);
        if (currentGroup == a) {
            currentGroup = b;
        } else if (currentGroup == b) {
            currentGroup = a;
        }
        broadcastTrackList();
    }

    public void pausePlay() {
        isPlaying = !isPlaying;
        currentPlayer.pausePlay();
        broadcastTrackList();
    }
    public void stop() {
        currentPlayer.stop();
        isPlaying = false;
        broadcastTrackList();
    }

    public void updateAutoPlayNext(boolean playNext) {
        this.playNext = playNext;
        currentPlayer.playNext(playNext);
        broadcastTrackList();
    }

    public void playTrack(int group, int track) {
        if (currentGroup != group) {
            initPlayer(tracksList.get(group).entry.player, false);
            currentPlayer.jumpTo(track);
            currentGroup = group;
            currentTrackInGroup = track;
        } else {
            currentTrackInGroup = track;
            currentPlayer.jumpTo(track);
        }
        isPlaying = true;
        broadcastTrackList();
    }

    private void nullOutPlayer() {
        initPlayer((playNow, playNext, listener) -> new NullTracksPlayer(), false);
        currentGroup = -1;
        currentTrackInGroup = -1;
        isPlaying = false;
    }

    public void clearPlaylist() {
        tracksList.clear();
        nullOutPlayer();
        broadcastTrackList();
    }

    public Utils.FlatJson playlistJson() {
        val items = FluentIterable.from(tracksList).transform(x -> x.json).toList();
        return Utils.json().add("playlist", items).build();
    }

    private void broadcastTrackList() {
        val trackGroups = new LinkedList<Core.PlaylistEntry>();
        int group = 0;
        for (Player.PlaylistEntry entry: tracksList) {
            List<Core.Track> tracks = new LinkedList<>();
            int trackInGroup = 0;
            for (MediaHandler.TrackDescription track: entry.entry.getTracks()) {
                tracks.add(new Core.Track(track.name(), track.image(), group==currentGroup && trackInGroup==currentTrackInGroup));
                trackInGroup++;
            }
            trackGroups.add(new Core.PlaylistEntry(entry.name(), entry.thumbnail(), tracks));
            group++;
        }
        Log.i("X", "Broadcast tracklist " + trackGroups);
        eventBus.postSticky(new UIBackendEvents.TracksLatest(isPlaying, playNext, trackGroups));
    }
}
