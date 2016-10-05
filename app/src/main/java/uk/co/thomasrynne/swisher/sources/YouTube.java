package uk.co.thomasrynne.swisher.sources;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import com.annimon.stream.Stream;
import lombok.Value;
import lombok.val;
import uk.co.thomasrynne.swisher.Utils;
import uk.co.thomasrynne.swisher.YouTubeApi;
import uk.co.thomasrynne.swisher.tree.MenuTree;

/**
 */
public class YouTube {
    private Context baseContext;

    public YouTube(Context baseContext) {
        this.baseContext = baseContext;
    }

    MenuTree.MenuItem rootMenu = MenuTree.MenuItem.tree("You Tube", "YouTube",
        new YouTubeChannelMenu("Watch History", "watchHistory"),
        new YouTubeChannelMenu("Favorites", "favorites"),
        new YouTubeChannelMenu("Likes", "likes"),
        new YouTubeChannelMenu("Watch Later", "watchLater")
    );

    @Value
    class YouTubeChannelMenu implements MenuTree.MenuItem {
        public String name;
        public String channel;

        @Override
        public String name() {
            return name;
        }

        @Override
        public Uri image() {
            return null;
        }

        @Override
        public List<MenuTree.MenuItem> children() {

            YouTubeApi youTubeApi = new YouTubeApi(baseContext, null);

            return Utils.list(Stream.of(youTubeApi.userChannel(channel)).map( (video) -> {
                return new MenuTree.PlaylistItemMenuItem(video.name(), video.thumbnail, video.toJSON());
            }));
        }
    }

//    def handler = new MediaHandler {
//        override def handle(json: JSONObject): HandleResult = {
//        if (json.has("youtube_video") && json.has("youtube_title")) {
//            val videoID = json.getString("youtube_video")
//            val title = json.getString("youtube_title")
//            val thumbnail = Uri.parse(json.getString("youtube_thumbnail"))
//            SuccessHandleResult(new YouTubeVideoTracks(new YouTubeVideo(videoID, title, Some(thumbnail))))
//        } else if (json.has("youtube_playlist") && json.has("youtube_title") && json.has("youtube_thumbnail")) {
//            val playlistID = json.getString("youtube_playlist")
//            val name: String = json.getString("youtube_title")
//            val thumbnail = Uri.parse(json.getString("youtube_thumbnail"))
//            val videos = new YouTubeApi(context, null).playlistItems(playlistID).toArray(Array[YouTubeVideo]())
//            SuccessHandleResult(new YouTubePlaylistTracks(name, thumbnail, videos.toList.map(v => YouTubeVideo(v.videoID, v.name, v.image))))
//        } else {
//            NotApplicableHandleResult
//        }
//        }
//    }

    @Value
    static class YouTubeVideo { //TrackDescription
        String videoID;
        String name;
        Uri image;
    }

//    class YouTubeVideoTracks(video:YouTubeVideo) extends Tracks {
//        def name = video.name
//        def tracks = List(video)
//        def thumbnail: Option[Uri] = video.image
//        def player(init:PlayerInit, listener: PlayerListener) = new YouTubeTracksPlayer(init, listener, List(video))
//    }
//
//    class YouTubePlaylistTracks(val name:String, thumbnailX:Uri, videos:List[YouTubeVideo]) extends Tracks {
//        override def tracks = videos
//        override def thumbnail: Option[Uri] = Some(thumbnailX)
//        def player(init:PlayerInit, listener: PlayerListener) = new YouTubeTracksPlayer(init, listener, videos)
//    }
//
//    class YouTubeTracksPlayer(init:PlayerInit, listener:PlayerListener, videos:List[YouTubeVideo]) extends TracksPlayer {
//        val eventBus = new EventBusScala(EventBus.getDefault)
//        var current = 0
//        val eventBusListener = new {
//            def onEventBackgroundThread(event: Events.YouTubeStatusEvent) {
//                event.status match {
//                    case YouTubeStatusEvent.Status.Paused => listener.paused()
//                    case YouTubeStatusEvent.Status.Playing => listener.playing()
//                    case YouTubeStatusEvent.Status.Ended =>
//                        if ((current + 1) < videos.length) {
//                            jumpTo(current+1)
//                            listener.onTrack(current+1)
//                        } else {
//                            listener.finished()
//                        }
//                }
//            }
//        }
//        init match {
//            case CuePlayerInit => cueBeginning()
//            case PlayTrackInit(track) => jumpTo(track)
//            case PlayPlayerInit => jumpTo(0); listener.onTrack(0)
//        }
//
//        eventBus.register(eventBusListener)
//        def stop() {
//            eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.Pause, null))
//        }
//        def jumpTo(track: Int) {
//            current = track
//            val videoID = videos(track).videoID
//            eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.PlayFromStart, videoID))
//        }
//        def pausePlay() {
//            eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.PausePlay, null))
//        }
//        def cueBeginning() {
//            current = 0
//            listener.onTrack(0)
//            val videoID = videos(0).videoID
//            eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.CueFromStart, videoID))
//        }
//        def seekTo(millis: Int) {}
//        def clear() {
//            eventBus.unregister(eventBusListener)
//            eventBus.post(new Events.YouTubeControlEvent(Events.YouTubeControlEvent.Action.Clear, null))
//        }
//    }

}
