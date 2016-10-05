package uk.co.thomasrynne.swisher.sources

import android.content.Context
import android.net.Uri
import android.support.v4.app.Fragment
import de.greenrobot.event.EventBus
import org.json.JSONObject
import uk.co.thomasrynne.swisher.Events.YouTubeStatusEvent
import uk.co.thomasrynne.swisher.{Events, YouTubeApi}
import uk.co.thomasrynne.swisher.YouTubeApi.YouTubeVideo
import uk.co.thomasrynne.swisher.model._
import uk.co.thomasrynne.swisher.tree.{ItemMenuItem, MenuItem, Menu}

/**
 *
 */
class YouTubeSource(context:Context) {

  object RootMenu extends Menu {
    def name = "You Tube"
    def path = "YouTube"
    def items: List[MenuItem] = List(
      YouTubeChannelMenu("Watch History", "watchHistory"),
      YouTubeChannelMenu("Favorites", "favorites"),
      YouTubeChannelMenu("Likes", "likes"),
      YouTubeChannelMenu("Watch Later", "watchLater"))
    def search: Option[(String) => List[MenuItem]] = Some(
      (text:String) => {
        val youTubeApi: YouTubeApi = new YouTubeApi(context, null)
        import scala.collection.JavaConversions._
        youTubeApi.search(text).toList.map { video =>
          ItemMenuItem(video.name, Option(video.thumbnail), video.toJSON, None)
        }
      }
    )
  }

  case class YouTubeChannelMenu(name:String, channel:String) extends Menu {
    override def path = name.replaceAll("\\s", "")
    override def items: List[MenuItem] = {
      val youTubeApi: YouTubeApi = new YouTubeApi(context, null)
      import scala.collection.JavaConversions._
      youTubeApi.userChannel(channel).toList.map { video =>
        ItemMenuItem(video.name, Option(video.thumbnail), video.toJSON, None)
      }
    }
    override def search: Option[(String) => List[MenuItem]] = None
  }

  def handler = new MediaHandler {
    override def handle(json: JSONObject): HandleResult = {
      if (json.has("youtube_video") && json.has("youtube_title")) {
        val videoID = json.getString("youtube_video")
        val title = json.getString("youtube_title")
        val thumbnail = Uri.parse(json.getString("youtube_thumbnail"))
        SuccessHandleResult(new YouTubeVideoTracks(new YouTubeVideo(videoID, title, Some(thumbnail))))
      } else if (json.has("youtube_playlist") && json.has("youtube_title") && json.has("youtube_thumbnail")) {
        val playlistID = json.getString("youtube_playlist")
        val name: String = json.getString("youtube_title")
        val thumbnail = Uri.parse(json.getString("youtube_thumbnail"))
        val videos = new YouTubeApi(context, null).playlistItems(playlistID).toArray(Array[YouTubeVideo]())
        SuccessHandleResult(new YouTubePlaylistTracks(name, thumbnail, videos.toList.map(v => YouTubeVideo(v.videoID, v.name, v.image))))
      } else {
        NotApplicableHandleResult
      }
    }
  }

  case class YouTubeVideo(videoID:String, val name:String, image:Option[Uri]) extends TrackDescription

  class YouTubeVideoTracks(video:YouTubeVideo) extends Tracks {
    def name = video.name
    def tracks = List(video)
    def thumbnail: Option[Uri] = video.image
    def player(init:PlayerInit, listener: PlayerListener) = new YouTubeTracksPlayer(init, listener, List(video))
  }

  class YouTubePlaylistTracks(val name:String, thumbnailX:Uri, videos:List[YouTubeVideo]) extends Tracks {
    override def tracks = videos
    override def thumbnail: Option[Uri] = Some(thumbnailX)
    def player(init:PlayerInit, listener: PlayerListener) = new YouTubeTracksPlayer(init, listener, videos)
  }

  class YouTubeTracksPlayer(init:PlayerInit, listener:PlayerListener, videos:List[YouTubeVideo]) extends TracksPlayer {
    val eventBus = new EventBusScala(EventBus.getDefault)
    var current = 0
    val eventBusListener = new {
      def onEventBackgroundThread(event: Events.YouTubeStatusEvent) {
        event.status match {
          case YouTubeStatusEvent.Status.Paused => listener.paused()
          case YouTubeStatusEvent.Status.Playing => listener.playing()
          case YouTubeStatusEvent.Status.Ended =>
            if ((current + 1) < videos.length) {
              jumpTo(current+1)
              listener.onTrack(current+1)
            } else {
              listener.finished()
            }
        }
      }
    }
    init match {
      case CuePlayerInit => cue()
      case PlayTrackInit(track) => jumpTo(track)
      case PlayPlayerInit => jumpTo(0); listener.onTrack(0)
    }

    eventBus.register(eventBusListener)
    def stop() {
      eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.Pause, null))
    }
    def jumpTo(track: Int) {
      current = track
      val videoID = videos(track).videoID
      eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.PlayFromStart, videoID))
    }
    def pausePlay() {
      eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.PausePlay, null))
    }
    def cue() {
      current = 0
      listener.onTrack(0)
      val videoID = videos(0).videoID
      eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.CueFromStart, videoID))
    }
    def seekTo(millis: Int) {}
    def clear() {
      eventBus.unregister(eventBusListener)
      eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.Clear, null))
    }
  }
}
