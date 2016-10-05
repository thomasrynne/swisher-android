package uk.co.thomasrynne.swisher.model

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import org.json.{JSONObject, JSONArray}
import uk.co.thomasrynne.swisher.JsonEventHandler

import scala.collection.mutable.ArrayBuffer

/**
 */
class BuiltInHandler(screenOn:ScreenOn, mediaHandlers:Array[MediaHandler]) {
  val musicPlayer = new MusicPlayer()

  def playlistHandler: Actions.Handler = {
    return new Actions.Handler {
      def handle(json: JSONObject): Boolean = {
        if (json.has("playlist")) {
          val list: JSONArray = json.getJSONArray("playlist")
          val tracks = new ArrayBuffer[PlaylistEntry](list.length())
          var i = 0; while (i < list.length) {
            val entry: JSONObject = list.getJSONObject(i)
            tracksFor(entry).foreach(tracks.append(_))
            i+=1
          }
          play(tracks.toList)
          screenOn.wakeUp
          return true
        }
        else {
          return false
        }
      }
    }
  }

  def playHandler: Actions.Handler = {
    return new Actions.Handler {
      def handle(json: JSONObject): Boolean = {
        tracksFor(json) match {
          case Some(tracks) => {
            play(List(tracks))
            screenOn.wakeUp
            return true
          };
          case None => return false
        }
      }
    }
  }

  private def play(tracks:List[PlaylistEntry]) {
    musicPlayer.play(tracks)
    //wakeup
  }

  def actionsHandler: Actions.Handler = {
    return new Actions.Handler {
      def handle(json: JSONObject): Boolean = {
        if (json.has("action")) {
          val action = json.getString("action")
          action match {
            case "stop" => musicPlayer.stop()
            case "next" => musicPlayer.next()
            case "previous" => musicPlayer.previous()
            case "pause" => musicPlayer.pausePlay()
          }
          return true
        }
        else {
          return false
        }
      }
    }
  }

  def add(json:JSONObject) {
    tracksFor(json).foreach(musicPlayer.add(_))
  }

  private def tracksFor(json:JSONObject):Option[PlaylistEntry] = {
    mediaHandlers.foreach { handler =>
      handler.handle(json) match {
        case NotApplicableHandleResult =>
        case NotAvailableHandleResult => return None //toast
        case SuccessHandleResult(tracks) => return Some(PlaylistEntry(json,tracks))
      }
    }
    return None
  }
}
