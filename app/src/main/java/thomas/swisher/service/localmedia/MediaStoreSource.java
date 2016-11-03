package thomas.swisher.service.localmedia;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.getbase.android.db.cursors.FluentCursor;
import com.getbase.android.db.provider.ProviderAction;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

import lombok.Value;
import lombok.val;
import thomas.swisher.MediaHandler;
import thomas.swisher.tree.MenuUtils;
import thomas.swisher.tree.Menus;
import thomas.swisher.todo.Songs;
import thomas.swisher.utils.Utils;
import thomas.swisher.shared.Core;
import thomas.swisher.service.player.TracksPlayer;

/**
 */
public class MediaStoreSource {

    private Context context;

    public MediaStoreSource(Context context) {
        this.context = context;
    }

    public Menus.Menu albumMenu() {
        return new Menus.Menu() {
            @Override
            public String name() {
                return "Albums";
            }

            @Override
            public List<Menus.MenuEntry> items() {
                return MenuUtils.alphabet(albums());
            }
        };
    }

    public Menus.Menu tracksMenu() {
        return new Menus.Menu() {
            @Override
            public String name() {
                return "Tracks";
            }

            @Override
            public List<Menus.MenuEntry> items() {
                return MenuUtils.alphabet(tracks());
            }
        };
    }

    public MediaHandler albumHandler() {
        return new MediaHandler() {
            @Override
            public Optional<PlaylistEntry> handle(Utils.FlatJson json) {
                if (json.has("artist") && json.has("album")) {
                    val album = json.get("album");
                    val artist = json.get("artist");
                    val albumId = albumIdFor(album, artist);
                    val name = album + " (" + artist + ")";
                    if (albumId.isPresent()) {
                        val tracks = albumTracks(albumId.get());
                        return Optional.of(new MediaHandler.PlaylistEntry(
                                name,
                                Songs.imageForAlbum(albumId.get()),
                                tracks,
                                (playNow, currentTrack, playNext, listener) -> new LocalMediaPlayer(tracks, playNow, currentTrack, playNext, listener)
                        ));
                    } else {
                        return Optional.absent();
                    }
                } else {
                    return Optional.absent();
                }
            }
        };
    }


    public MediaHandler trackHandler() {
        return new MediaHandler() {
            @Override
            public Optional<PlaylistEntry> handle(Utils.FlatJson json) {
                if (json.has("track_name") && json.has("artist")) {
                    val trackName = json.get("track_name");
                    val artist = json.get("artist");
                    val maybeTrack = findTrack(trackName, artist);
                    if (maybeTrack.isPresent()) {
                        val track = maybeTrack.get();
                        val singleTrack = Collections.singletonList(track);
                        return Optional.of(new MediaHandler.PlaylistEntry(
                                track.fullAlbumName(),
                                track.image,
                                singleTrack,
                                (playNow, currentTrack, playNext, listener) -> new LocalMediaPlayer(
                                        singleTrack, playNow, currentTrack, playNext, listener)
                        ));
                    } else {
                        return Optional.absent();
                    }
                } else {
                    return Optional.absent();
                }
            }
        };
    }

    private List<Core.PlaylistItemUIMenuItem> albums() {
        val action = ProviderAction.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI)
            .projection(BaseColumns._ID, MediaStore.Audio.AlbumColumns.ALBUM, MediaStore.Audio.AudioColumns.ARTIST)
            .orderBy(MediaStore.Audio.AudioColumns.ALBUM)
            .perform(context.getContentResolver());
        return action.toFluentIterable( (cursor) -> {

            int albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            int albumIdIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);

            String albumName = cursor.getString(albumIndex);
            String artistName = cursor.getString(artistIndex);
            int albumId = cursor.getInt(albumIdIndex);
            return new Core.PlaylistItemUIMenuItem(
                    cursor.getString(albumIndex),
                    Utils.json().add("album", albumName).add("artist", artistName).build(),
                    Songs.imageForAlbum(albumId));
        }).toList();
    }

    private Optional<Integer> albumIdFor(String album, String artist) {
        val selection = MediaStore.Audio.AudioColumns.ALBUM + "=? " + " and " + MediaStore.Audio.AudioColumns.ARTIST + "=?";
        val action = ProviderAction.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI)
                .projection(BaseColumns._ID, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ARTIST)
                .where(selection, album, artist)
                .perform(context.getContentResolver());
        return action.toFluentIterable( (cursor) -> {
            int albumIdIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            return cursor.getInt(albumIdIndex);
        }).first();
    }

    @Value
    static class Track implements MediaHandler.TrackDescription {
        public final String trackTitle;
        public final String albumName;
        public final String artistName;
        public final int track;
        public final Uri path;
        public final Optional<Uri> image;

        @Override
        public String name() {
            return trackTitle;
        }

        public String fullAlbumName() {
            return albumName + " (" + artistName + ")";
        }

        @Override
        public Optional<Uri> image() {
            return image;
        }
    }

    private ImmutableList<Track> albumTracks(int albumId) {
        val action = ProviderAction.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .projection(
                        MediaStore.MediaColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AlbumColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.TRACK,
                        MediaStore.Audio.AlbumColumns.ALBUM_ID,
                        MediaStore.MediaColumns.DATA)
                .where(MediaStore.Audio.AlbumColumns.ALBUM_ID + " = ?", albumId)
                .orderBy(android.provider.MediaStore.Audio.AudioColumns.TRACK)
                .perform(context.getContentResolver());
        return FluentIterable.from(tracksFromAction(action).toSortedSet( (a,b) -> a.track - b.track)).toList();
    }

    private Optional<Track> findTrack(String trackName, String artist) {
        val action = ProviderAction.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .projection(
                        MediaStore.MediaColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AlbumColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.TRACK,
                        MediaStore.Audio.AlbumColumns.ALBUM_ID,
                        MediaStore.MediaColumns.DATA)
                .where(MediaStore.MediaColumns.TITLE + " =? and " + MediaStore.Audio.AudioColumns.ARTIST + " =?",
                        trackName, artist
                )
                .perform(context.getContentResolver());
        return tracksFromAction(action).first();
    }

    private FluentIterable<Track> tracksFromAction(FluentCursor action) {
        return action.toFluentIterable((cursor) -> {
            int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            int trackIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TRACK);
            int albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM_ID);
            int albumNameIndex = cursor.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM);
            int dataIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

            String title = cursor.getString(titleIndex);
            String artistName = cursor.getString(artistIndex);
            String albumName = cursor.getString(albumNameIndex);
            int index = cursor.getInt(trackIndex);
            int albumId = cursor.getInt(albumIdIndex);
            String path = cursor.getString(dataIndex);

            return new Track(
                    title,
                    artistName,
                    albumName,
                    index,
                    Uri.parse(path),
                    Songs.imageForAlbum(albumId));
        });
    }

    private ImmutableList<Core.PlaylistItemUIMenuItem> tracks() {
        val action = ProviderAction.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .projection(
                        MediaStore.MediaColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AlbumColumns.ALBUM_ID)
                .orderBy(MediaStore.MediaColumns.TITLE)
                .perform(context.getContentResolver());
        return action.toFluentIterable((cursor) -> {

            int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.TITLE);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            int albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM_ID);

            String title = cursor.getString(titleIndex);
            String artistName = cursor.getString(artistIndex);
            int albumId = cursor.getInt(albumIdIndex);
            String name = title + " (" + artistName + ")";

            return new Core.PlaylistItemUIMenuItem(
                    name,
                    Utils.json().add("track_name", title).add("artist", artistName).build(),
                    Songs.imageForAlbum(albumId));
        }).toList();
    }

    private class LocalMediaPlayer implements TracksPlayer {
        private List<Track> tracks;
        private MediaHandler.PlayerListener listener;
        private int currentTrack = 0;
        private boolean playNext;
        private AsyncMediaPlayer mediaPlayer = new AsyncMediaPlayer() {
            public void onFinished() {
                if ((currentTrack + 1) < tracks.size()) {
                    currentTrack++;
                    playCurrent(playNext);
                    listener.onTrack(playNext, currentTrack);
                } else {
                    listener.notify(MediaHandler.PlayerNotification.Finished);
                }
            }
            public void onReady() {
                broadcastPosition();
            }
        };

        private void broadcastPosition() {
            listener.currentProgress(mediaPlayer.progress());
        }

        private void playCurrent(boolean playNow) {
            mediaPlayer.play(tracks.get(currentTrack).path.getPath(), 0, playNow);
            broadcastPosition();
        }

        LocalMediaPlayer(List<Track> tracks, boolean playNow, int currentTrack, boolean playNext, MediaHandler.PlayerListener listener) {
            this.tracks = tracks;
            this.currentTrack = currentTrack;
            this.playNext = playNext;
            this.listener = listener;
            playCurrent(playNow);
        }

        @Override
        public void cueBeginning() {
            currentTrack = 0;
            playCurrent(false);
        }

        @Override
        public void stop() {
            mediaPlayer.stop();
            broadcastPosition();
        }

        @Override
        public void pausePlay() {
            mediaPlayer.pausePlay();
            broadcastPosition();
        }

        @Override
        public void clear() {
            mediaPlayer.release();
        }

        @Override
        public void playNext(boolean playNext) {
            this.playNext = playNext;
        }

        @Override
        public void jumpTo(int track) {
            currentTrack=track;
            playCurrent(true);
        }

        @Override
        public void seekTo(int toMillis) {
            mediaPlayer.seekTo(toMillis);
            broadcastPosition();
        }

        @Override
        public void requestProgressUpdate() {
            broadcastPosition();
        }
    }
}
