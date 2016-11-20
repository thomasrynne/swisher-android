package thomas.swisher.service.player;

import android.net.Uri;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
 *
 *  All interaction with this class should be from the EventBus Background thread
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

    private final Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(TracksPlayerNotificationEvent event) {
            if (Player.this.currentPlayerInstance == event.instance) {
                switch (event.notification) {
                    case Finished: toNext(); break;
                    case Paused:
                        isPlaying = false; shouldBePlaying = false;
                        break;
                    case Playing:
                        isPlaying = true; shouldBePlaying = true;
                        break;
                    case Failed:
                        gotError = true; isPlaying = false; shouldBePlaying = false;
                        break;
                }
                broadcastTrackList();
            }
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(TracksPlayerOnTrackEvent event) {
            if (Player.this.currentPlayerInstance == event.instance) {
                Player.this.currentTrackInGroup = event.track;
                Player.this.shouldBePlaying = event.autoPlayedThisTrack;
                Player.this.isPlaying = event.autoPlayedThisTrack;
                broadcastTrackList();
            }
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(TracksPlayerProgressEvent event) {
            if (Player.this.currentPlayerInstance == event.instance) {
                Player.this.progress = event.progress;
                Player.this.isPlaying = event.isPlaying;
                broadcastTrackList();
            }
        }
    };

    private CurrentPlaylist playlist;
    private Core.PlayerProgress progress = Core.PlayerProgress.Null;
    private int currentPlayerInstance = -1;
    private int currentGroup = -1;
    private int currentTrackInGroup = -1;
    private boolean gotError = false;
    private boolean isPlaying = false;
    private boolean shouldBePlaying = false;
    private boolean playNext = true;
    private TracksPlayer currentPlayer = new NullTracksPlayer();

    @Value
    private static class TracksPlayerNotificationEvent {
        public final int instance;
        public final MediaHandler.PlayerNotification notification;
    }

    @Value
    private static class TracksPlayerOnTrackEvent {
        public final int instance;
        public final boolean autoPlayedThisTrack;
        public final int track;
    }

    @Value
    private static class TracksPlayerProgressEvent {
        public final int instance;
        public final boolean isPlaying;
        public final Core.PlayerProgress progress;
    }

    /**
     * TrackPlayers interact with the Player using this class
     * The Player is single threaded so these methods post events
     * to be consumed on the EventBus background thread
     */
    private class ListenerForPlayer implements MediaHandler.PlayerListener {
        private int instance;
        ListenerForPlayer(int instance) {
            this.instance = instance;
        }

        @Override
        public void notify(MediaHandler.PlayerNotification notification) {
            eventBus.post(new TracksPlayerNotificationEvent(instance, notification));
        }
        @Override
        public void onTrack(boolean autoPlayedThisTrack, int track) {
            eventBus.post(new TracksPlayerOnTrackEvent(instance, autoPlayedThisTrack, track));
        }
        @Override
        public void currentProgress(boolean isPlaying, Core.PlayerProgress progress) {
            eventBus.post(new TracksPlayerProgressEvent(instance, isPlaying, progress));
        }
    }

    public Player(CurrentPlaylist currentPlaylist) {
        this.eventBus.register(listener);
        this.playlist = currentPlaylist;
    }

    private void toNext() {
        if (playlist.size() == 1) { //only one group, just cueBeginning start
            currentTrackInGroup = 0;
            isPlaying = false;
            shouldBePlaying = false;
            currentPlayer.cueBeginning();
        } else if ((currentGroup+1) < playlist.size()) { //there is a next group, play/cue it
            currentGroup++;
            currentTrackInGroup = 0;
            initPlayer(playlist.get(currentGroup).entry.player, playNext, 0);
        } else { //we got to the end, create and cue the start group
            currentPlayer.clear();
            currentTrackInGroup = 0;
            currentGroup = 0;
            initPlayer(playlist.get(0).entry.player, false, 0);
        }
    }

    private void initPlayer(MediaHandler.ThePlayer player, boolean playNow, int currentTrack) {
        gotError = false;
        currentPlayer.clear();
        currentPlayerInstance = sequence.incrementAndGet();
        currentPlayer = player.create(playNow, currentTrack, playNext, new ListenerForPlayer(currentPlayerInstance));
        progress = Core.PlayerProgress.Null;
        isPlaying = false;
        shouldBePlaying = playNow;
    }

    public void play(List<PlaylistEntry> playlistEntries) {
        updatePlaylist(playlistEntries, true);
    }

    public void initialisePlaylist(List<PlaylistEntry> entries) {
        playlist.initialise(entries);
        restartPlaylist(false);
    }

    public void updatePlaylist(List<Player.PlaylistEntry> tracks, boolean playNow) {
        playlist.replace(tracks);
        restartPlaylist(playNow);
    }
    private void restartPlaylist(boolean playNow) {
        if (!playlist.isEmpty()) {
            initPlayer(playlist.get(0).entry.player, playNow, 0);
            isPlaying = playNow;
            currentGroup = 0;
            currentTrackInGroup = 0;
        } else {
            nullOutPlayer();
        }
        broadcastTrackList();
    }

    public void add(Player.PlaylistEntry tracks) {
        if (playlist.isEmpty()) {
            currentGroup = 0;
            currentTrackInGroup = 0;
            initPlayer(tracks.entry.player, false, 0);
        }
        playlist.add(tracks);
        broadcastTrackList();
    }

    public void remove(int groupPosition) {
        playlist.remove(groupPosition);
        if (currentGroup == groupPosition) {
            if (!playlist.isEmpty()) {
                initPlayer(playlist.get(0).entry.player, false, 0);
                currentGroup = 0;
                currentTrackInGroup = 0;
            } else {
                nullOutPlayer();
            }
        }
        broadcastTrackList();
    }

    public void swap(int a, int b) {
        playlist.swap(a, b);
        if (currentGroup == a) {
            currentGroup = b;
        } else if (currentGroup == b) {
            currentGroup = a;
        }
        broadcastTrackList();
    }

    public void pausePlay() {
        shouldBePlaying = !shouldBePlaying;
        currentPlayer.pausePlay(shouldBePlaying);
        broadcastTrackList();
    }
    public void stop() {
        currentPlayer.stop();
        isPlaying = false;
        shouldBePlaying = false;
        broadcastTrackList();
    }

    public void seekTo(int toMillis) {
        currentPlayer.seekTo(toMillis);
    }

    public void updateAutoPlayNext(boolean playNext) {
        this.playNext = playNext;
        currentPlayer.playNext(playNext);
        broadcastTrackList();
    }

    public void playTrackAt(int group, int track) {
        if (currentGroup != group || gotError) {
            currentGroup = group;
            currentTrackInGroup = track;
            initPlayer(playlist.get(group).entry.player, true, track);
        } else {
            currentTrackInGroup = track;
            currentPlayer.jumpTo(track);
            isPlaying = false;
            progress = Core.PlayerProgress.Null;
        }
        broadcastTrackList();
    }

    public void sendStatusUpdate() {
        currentPlayer.requestProgressUpdate();
    }

    private void nullOutPlayer() {
        initPlayer((playNow, currentTrack, playNext, listener) -> new NullTracksPlayer(), false, 0);
        currentGroup = -1;
        currentTrackInGroup = -1;
        isPlaying = false;
        shouldBePlaying = false;
    }

    public void clearPlaylist() {
        playlist.clear();
        nullOutPlayer();
        broadcastTrackList();
    }

    public List<Utils.FlatJson> playlistJson() {
        return FluentIterable.from(playlist.toList()).transform(x -> x.json).toList();
    }

    private void broadcastTrackList() {
        val trackGroups = new LinkedList<Core.PlaylistEntry>();
        for (Player.PlaylistEntry entry: playlist.toList()) {
            List<Core.Track> tracks = new LinkedList<>();
            for (MediaHandler.TrackDescription track: entry.entry.getTracks()) {
                tracks.add(new Core.Track(track.name(), track.image()));
            }
            trackGroups.add(new Core.PlaylistEntry(entry.id, entry.name(), entry.thumbnail(), tracks));
        }
        val playState = gotError ?
            UIBackendEvents.PlayState.Failed :
            (playlist.isEmpty() ? UIBackendEvents.PlayState.Empty :
                (shouldBePlaying ?
                    isPlaying ?
                        UIBackendEvents.PlayState.Playing:
                        UIBackendEvents.PlayState.TryingToPlay :
                    UIBackendEvents.PlayState.Paused));
        eventBus.postSticky(new UIBackendEvents.TracksLatest(
                playState, playNext, currentGroup, currentTrackInGroup,
                trackGroups, progress));
    }

    public void destroy() {
        nullOutPlayer();
        eventBus.unregister(listener);
    }
}