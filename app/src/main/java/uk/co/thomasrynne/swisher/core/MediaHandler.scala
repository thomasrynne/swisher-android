package uk.co.thomasrynne.swisher.model

import android.net.Uri
import org.json.JSONObject

/**
 */
trait MediaHandler {
  def handle(json: JSONObject):HandleResult
}

trait HandleResult
case object NotApplicableHandleResult extends HandleResult
case object NotAvailableHandleResult extends HandleResult
case class SuccessHandleResult(tracks:Tracks) extends HandleResult

trait PlayerProgress
case object UnknownPlayerProgress extends PlayerProgress
case class KnownPlayerProgress(total:Int, progress:Int) extends PlayerProgress

trait PlayerListener {
  def finished()
  def playing()
  def paused()
  def stopped()
  def onTrack(position:Int)
  def updateProgress(progress:PlayerProgress)
}
trait PlayerInit
case object CuePlayerInit extends PlayerInit
case object PlayPlayerInit extends PlayerInit
case class PlayTrackInit(track:Int) extends PlayerInit

trait TrackDescription {
  def name: String
  def image: Option[Uri]
}
trait Tracks {
  def name: String
  def thumbnail: Option[Uri]
  def tracks: List[TrackDescription]
  def player(init:PlayerInit, listener:PlayerListener):TracksPlayer
}
