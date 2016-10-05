package uk.co.thomasrynne.swisher.model

import android.net.Uri
import de.greenrobot.event.EventBus
import org.json.{JSONArray, JSONObject}
import uk.co.thomasrynne.swisher.Events
import uk.co.thomasrynne.swisher.Events.Progress

import scala.collection.mutable.ArrayBuffer

/**
 */
class MusicPlayer {

  private val eventBus = EventBus.getDefault
  private val tracksList = ArrayBuffer[PlaylistEntry]()
  private var current:Option[(Int,Option[Int])] = None
  private var isPlaying = false
  private var player:Option[TracksPlayer] = None

  private val listener = new PlayerListener {
    override def stopped() { isPlaying = false; broadcastPlayingStatus() }
    override def playing() { isPlaying = true ; broadcastPlayingStatus() }
    override def finished() { toNext() }
    override def onTrack(position: Int) { current = Some(current.get._1, Some(position)); broadcastTracks(false) }
    override def paused() { isPlaying = false; broadcastPlayingStatus() }
    override def updateProgress(progress: PlayerProgress) { broadcastProgress(progress) }
  }

  private def toNext() {
    current.foreach { case (group,track) => {
      if (tracksList.length == 1) {
        current = Some((0, None))
        isPlaying = false
        onCurrentPlayer(_.cue())
      } else if ((group+1) < tracksList.size) {
        current = Some((group + 1, None))
        onCurrentPlayer(_.clear())
        player = Some(tracksList(group+1).tracks.player(PlayPlayerInit, listener))
      } else {
        current = Some((0,None))
        isPlaying = false
        onCurrentPlayer(_.clear())
        player = Some(tracksList(0).tracks.player(CuePlayerInit, listener))
      }
      broadcastTracks(false)
      broadcastPlayingStatus()
    }}
  }

  def onCurrentPlayer(f:TracksPlayer=>Unit) {
    player.foreach { f(_) }
  }

  def play(tracks:List[PlaylistEntry]) {
    onCurrentPlayer(_.clear())
    tracksList.clear()
    tracks.foreach(tracksList.append(_))
    if (!tracksList.isEmpty) {
      current = Some( (0,None) )
      player = Some(tracksList(0).tracks.player(PlayPlayerInit, listener))
      isPlaying = true
    } else {
      isPlaying = false
    }
    broadcastPlayingStatus()
    broadcastTracks()
  }

  def add(tracks:PlaylistEntry) {
    if (tracksList.isEmpty) {
      current = Some( (0,None) )
      player = Some(tracks.tracks.player(CuePlayerInit, listener))
      onCurrentPlayer(_.cue)
    }
    tracksList.append(tracks)
    broadcastPlayingStatus()
    broadcastTracks()
  }

  def remove(position:Int) = {
    if (Some(position) == current) {
      onCurrentPlayer(_.clear())
      current = None
      player = None
      isPlaying = false
    }
    tracksList.remove(position)
    broadcastTracks()
    broadcastPlayingStatus()
  }

  def swap(a:Int,b:Int) {
    val aa = tracksList(a)
    val bb = tracksList(b)
    tracksList(a) = bb
    tracksList(b) = aa
    if (current == Some(a)) current == Some(b)
    if (current == Some(b)) current == Some(a)
    broadcastTracks()
  }

  def next() { /*TODO*/}
  def previous() { /*TODO*/ }
  def pausePlay() { isPlaying = !isPlaying; onCurrentPlayer(_.pausePlay()); broadcastPlayingStatus }
  def stop() { onCurrentPlayer(_.stop()); isPlaying = false; broadcastPlayingStatus }

  def playTrack(position:Int, track:Int) {
    if (current.isDefined && current.get._1 != position) {
      onCurrentPlayer(_.clear())
      current = Some((position,Some(track)))
      player = Some(tracksList(position).tracks.player(PlayTrackInit(track), listener))
    } else {
      current = Some((position,Some(track)))
      onCurrentPlayer(_.jumpTo(track))
    }
    isPlaying = true
    broadcastTracks(tracksChanged=false)
    broadcastPlayingStatus()
  }

  def seekTo(millis:Int) {
    onCurrentPlayer(_.seekTo(millis))
    broadcastProgress()
  }

  def clearPlaylist() {
    onCurrentPlayer(_.clear())
    current = None
    player = None
    tracksList.clear()
    broadcastTracks()
    broadcastPlayingStatus()
  }

  def playListJson = {
    val json: JSONObject = new JSONObject
    val items: java.util.List[JSONObject] = new java.util.LinkedList[JSONObject]()
    for (entry <- tracksList) {
      items.add(entry.json)
    }
    json.put("playlist", new JSONArray(items))
    json
  }

  def broadcastProgress(progress:PlayerProgress=player.map(_.progress).getOrElse(UnknownPlayerProgress)) {
    progress match {
      case UnknownPlayerProgress => eventBus.postSticky(new Events.ProgressStatus(new Progress(false, false, -1, -1)))
      case KnownPlayerProgress(total, progress) => eventBus.postSticky(new Events.ProgressStatus(new Progress(isPlaying, true, total, progress)))
    }
  }

  private def broadcastTracks(tracksChanged:Boolean=true) {
    val x = tracksList.toArray.map(tracks => TracksLabel(tracks.id, tracks.tracks.name, tracks.tracks.thumbnail, tracks.tracks.tracks.toArray))
    x.foreach(q => println(q.name + "  " + q.tracks.length))
    val (playingGroup, playingTrack) = current match {
      case Some( (group, Some(track)) ) => (group, track)
      case _ => (-1, -1)
    }
    eventBus.postSticky(TracksEvent(x, tracksChanged, playingGroup, playingTrack))
  }

  private def broadcastPlayingStatus() {
    broadcastProgress()
    val status = if (tracksList.isEmpty) Events.BigButtonStatus.Status.Off
      else if (isPlaying) Events.BigButtonStatus.Status.Pause else Events.BigButtonStatus.Status.Play
    eventBus.postSticky(new Events.BigButtonStatus(status))
  }
}
