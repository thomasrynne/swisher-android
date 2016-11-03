package thomas.swisher.youtube;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.common.base.Optional;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import lombok.Value;
import lombok.val;
import thomas.swisher.MediaHandler;
import thomas.swisher.utils.Utils;

/**
 * A wrapper around the YouTube http api
 */
public class YouTubeApi {

    private final AuthenticatedYouTubeHttpService authenticatedYouTubeHttpService;

    public YouTubeApi(AuthenticatedYouTubeHttpService authenticatedYouTubeHttpService) {
        this.authenticatedYouTubeHttpService = authenticatedYouTubeHttpService;
    }

    public void checkToken() throws IOException, GoogleAuthException {
        authenticatedYouTubeHttpService.checkToken();
    }

    interface YouTubeSearchItem {
        Utils.FlatJson toJson();
        String name();
        Uri thumbnail();
    }

    @Value
    static class YouTubeVideo implements YouTubeSearchItem, MediaHandler.TrackDescription {
        public final String videoID;
        public final String title;
        public final Uri thumbnail;

        public String name() { return title; }
        public Uri thumbnail() { return thumbnail; }
        public Utils.FlatJson toJson() {
            return Utils.json().
                add("youtube_video", videoID).
                add("youtube_title", title).
                add("youtube_thumbnail", thumbnail.toString()).build();
        }

        @Override
        public Optional<Uri> image() {
            //The youtube player is shown in the image area so we don't need or want an image
            //If there is an image there is a flicker when switching away from you tube where
            //the image is shown just after the youtube player is hidden and just before the image
            //of the next track is shown.
            return Optional.absent();
        }
    }

    @Value
    public static class YouTubePlaylist implements YouTubeSearchItem {
        private final String playlistID;
        private final String title;
        private final Uri thumbnail;
        private final Uri highQualityThumbnail;

        public String name() { return title; }
        public Uri thumbnail() { return thumbnail; }
        public Utils.FlatJson toJson() {
            return Utils.json().
                add("youtube_playlist", playlistID).
                add("youtube_title", title).
                add("youtube_thumbnail", highQualityThumbnail.toString()).build();
        }
    }

    public List<YouTubeSearchItem> userChannel(String name) throws IOException, GoogleAuthException {
        Utils.FlatJson response = authenticatedYouTubeHttpService.read("/channels?part=contentDetails&mine=true");
        val playlistID = extractPlaylistIDForChannel(response, name);
        return playlistItems(playlistID);
    }

    private List<YouTubeSearchItem> playlistItems(String playlistID) throws IOException, GoogleAuthException {
        val response = authenticatedYouTubeHttpService.read("/playlistItems?" +
            "part=contentDetails,snippet&" +
            "playlistId=" + playlistID + "&" +
            "maxResults=30");
        return extractPlaylistItems(response);
    }

    private String extractPlaylistIDForChannel(Utils.FlatJson response, String channelName) {
        val relatedPlaylists = response.getList("items").get(0).
                object("contentDetails").object("relatedPlaylists");
        return relatedPlaylists.get(channelName);
    }

    private List<YouTubeSearchItem> extractPlaylistItems(Utils.FlatJson response) {
        List<YouTubeSearchItem> items = new LinkedList<>();
        for (Utils.FlatJson entry: response.getList("items")) {
            String videoID = entry.object("contentDetails").get("videoId");
            Utils.FlatJson snippet = entry.object("snippet");
            String title = snippet.get("title");
            String thumbnail = snippet.object("thumbnails").object("high").get("url");
            items.add( new YouTubeVideo(videoID, title, Uri.parse(thumbnail)) );
        }
        return items;
    }
}