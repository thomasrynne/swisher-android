package uk.co.thomasrynne.swisher.sources

import android.media.{AudioManager, MediaPlayer}
import android.media.MediaPlayer.{OnPreparedListener, OnCompletionListener}
import android.net.Uri
import android.support.v4.app.Fragment
import android.util.Log
import org.json.JSONObject
import uk.co.thomasrynne.swisher.model._
import uk.co.thomasrynne.swisher.tree.{ItemMenuItem, MenuItem, Menu}
import uk.co.thomasrynne.swisher.util.{SafeMediaPlayer, MediaStore}

/**
 */
class MediaStoreSource(mediaStore:MediaStore) {

  val songsMenu = new Menu {
    def name = "Songs"
    def path = "Songs"
    def items: List[MenuItem] = songsSearch(None)
    def search: Option[(String) => List[MenuItem]] = Some( (text:String) => songsSearch(Some(text)) )
    private def songsSearch(query:Option[String]) = {
      val tracks = query match {
        case None => mediaStore.allSongs
        case Some(q) => mediaStore.searchTracks(q)
      }
      tracks.toList.map { track =>
        val json = new JSONObject
        json.put("track_name", track.name)
        json.put("artist", track.artist)
        ItemMenuItem(track.name + " (" + track.artist + ")", track.image, json, None)
      }
    }
  }

  val albumsMenu = new Menu {
    def name = "Albums"
    def path = "Albums"
    def items: List[MenuItem] = albumsSearch(None)
    def search: Option[(String) => List[MenuItem]] = Some( (text:String) => albumsSearch(Some(text)) )
    private def albumsSearch(query:Option[String]) = {
      val albums = query match {
        case None => mediaStore.allAlbums
        case Some(q) => mediaStore.searchAlbums(q)
      }
      albums.toList.map { album =>
        val json = new JSONObject
        json.put("album", album.name)
        json.put("artist", album.artist)
        ItemMenuItem(album.name + " (" + album.artist + ")", album.image, json, None)
      }
    }
  }

  val handler = new MediaHandler {
    def handle(json: JSONObject) = {
      if (json.has("artist") && json.has("album")) {
        val album = json.getString("album")
        val artist = json.getString("artist")
        mediaStore.albumAndTracks(album, artist) match {
          case None => NotAvailableHandleResult
          case Some((album,tracks)) => SuccessHandleResult(new MediaPlayerAlbumTracks(album, tracks.toList))
        }
      } else if (json.has("track_name") && json.has("artist")) {
        val trackName = json.getString("track_name")
        val artist = json.getString("artist")
        Option(mediaStore.findTrackOrNull(trackName, artist)) match {
          case None => NotAvailableHandleResult
          case Some(track) => SuccessHandleResult(new MediaPlayerSingleTrackTracks(track))
        }
      } else {
        NotApplicableHandleResult
      }
    }
  }

  class MediaPlayerAlbumTracks(album:mediaStore.Album, tracksX:List[mediaStore.Track]) extends Tracks {
    def tracks:List[TrackDescription] = tracksX
    def name = album.name
    def thumbnail: Option[Uri] = album.image
    def player(init:PlayerInit, listener: PlayerListener) = new MediaPlayerTracksPlayer(tracksX.toArray, init, listener)
  }
  class MediaPlayerTracksPlayer(tracks:Array[mediaStore.Track],
                                init:PlayerInit, listener:PlayerListener) extends TracksPlayer {
    private var current = 0
    private val mediaPlayer = new SafeMediaPlayer(
      onComplete = () => {
        Log.i("SWISHER", "onComplete " + current)
        if ((current + 1) < tracks.length) {
          current += 1
          playCurrent()
          listener.onTrack(current)
        } else {
          listener.finished()
        }
      },
      onProgressChange = (progress:PlayerProgress) =>
        listener.updateProgress(progress)
    )
    init match {
      case CuePlayerInit => mediaPlayer.play(tracks(0).path, 0, false); listener.onTrack(0)
      case PlayTrackInit(track) => jumpTo(track)
      case PlayPlayerInit => mediaPlayer.play(tracks(0).path, 0, true); listener.onTrack(0)
    }

    private def playCurrent() { mediaPlayer.play(tracks(current).path) }
    override def progress = mediaPlayer.progress
    def cue() { current = 0; mediaPlayer.play(tracks(0).path, 0, false); listener.onTrack(0) }
    def pausePlay() { mediaPlayer.pausePlay }
    def clear() { mediaPlayer.release }
    def stop() { mediaPlayer.stop }
    def jumpTo(trackIndex: Int) { current=trackIndex; playCurrent() }
    def seekTo(millis: Int) { mediaPlayer.seekTo(millis) }
  }

  class MediaPlayerSingleTrackTracks(track:mediaStore.Track) extends Tracks {
    def name = track.name
    def tracks: List[TrackDescription] = List(track)
    def thumbnail: Option[Uri] = track.image
    def player(init:PlayerInit, listener: PlayerListener) = {
      new MediaPlayerTracksPlayer(Array(track), init, listener)
    }
  }
}
