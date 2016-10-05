package uk.co.thomasrynne.swisher;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class YouTubeApi {

    public static final String youTubeBase = "https://www.googleapis.com/youtube/v3";
    private Activity activityOrNull;
    private Context baseContext;

    public YouTubeApi(Context baseContext, Activity activityOrNull) {
        this.baseContext = baseContext;
        this.activityOrNull = activityOrNull;
    }

    interface YouTubeSearchItem {
        JSONObject toJSON() throws JSONException;
        String name();
        Uri thumbnail();
    }
    public static class YouTubeVideo implements YouTubeSearchItem {
        public final String videoID;
        public final String title;
        public final Uri thumbnail;
        public final Uri highQualityThumbnail;

        public YouTubeVideo(String videoID, String title, Uri thumbnail, Uri highQualityThumbnail) {
            this.videoID = videoID;
            this.title = title;
            this.thumbnail = thumbnail;
            this.highQualityThumbnail = highQualityThumbnail;
        }
        public String name() { return title; }
        public Uri thumbnail() { return thumbnail; }
        public JSONObject toJSON() {
            try {
                JSONObject json = new JSONObject();
                json.put("youtube_video", videoID);
                json.put("youtube_title", title);
                json.put("youtube_thumbnail", highQualityThumbnail.toString());
                return json;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class YouTubePlaylist implements YouTubeSearchItem {
        private final String playlistID;
        private final String title;
        private final Uri thumbnail;
        private final Uri highQualityThumbnail;

        public YouTubePlaylist(String playlistID, String title, Uri thumbnail, Uri highQualityThumbnail) {
            this.playlistID = playlistID;
            this.title = title;
            this.thumbnail = thumbnail;
            this.highQualityThumbnail = highQualityThumbnail;
        }
        public String name() { return title; }
        public Uri thumbnail() { return thumbnail; }
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("youtube_playlist", playlistID);
            json.put("youtube_title", title);
            json.put("youtube_thumbnail", highQualityThumbnail.toString());
            return json;
        }
    }

    private String email() {
        AccountManager am = AccountManager.get(baseContext);
        Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        return accounts[0].name;
    }

    public List<YouTubeVideo> userChannel(final String channel) {
        return invoke( new YouTubeProcessor<List<YouTubeVideo>>() {
            public List<YouTubeVideo> work(Caller caller) throws Exception {
                JSONObject response1 = caller.invoke("/channels?part=contentDetails&mine=true");
                JSONObject relatedPlaylists = response1.getJSONArray("items").getJSONObject(0).
                        getJSONObject("contentDetails").getJSONObject("relatedPlaylists");
                String playlistID = relatedPlaylists.getString(channel);
                return playlistItems(caller, playlistID);
            }
            @Override
            public List<YouTubeVideo> empty() {
                return Collections.emptyList();
            }
        });
    }
    interface Caller {
        JSONObject invoke(String url) throws Exception;
    }
    interface YouTubeProcessor<R> {
        public <R> R work(Caller caller) throws Exception;
        public <R> R empty();
    }

    private <R> R invoke(YouTubeProcessor<R> processor) {
        try {
            final String token = getTokenWithRetry();
            return processor.work(new Caller() {
                public JSONObject invoke(String url) throws Exception {
                    Log.i("SWISHER", "url: " + url);
                    String json = Utils.readURL(YouTubeApi.youTubeBase + url, token);
                    if (json == null) {
                        GoogleAuthUtil.invalidateToken(baseContext, token);
                        return null;
                    } else {
                        return new JSONObject(json);
                    }
                }
            });
        } catch (final GooglePlayServicesAvailabilityException playEx) {
            if (activityOrNull != null) {
                activityOrNull.runOnUiThread(new Runnable() { public void run() {
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                            playEx.getConnectionStatusCode(),
                            activityOrNull,
                            123);
                    dialog.show();
                } });
            } else {
                Log.e("SWISHER", playEx.getConnectionStatusCode() + " " + playEx.getLocalizedMessage());
            }
        } catch (UserRecoverableAuthException recoverableException) {
            if (activityOrNull != null) {
                Intent recoveryIntent = recoverableException.getIntent();
                activityOrNull.startActivityForResult(recoveryIntent, 1234);
            }
        } catch (GoogleAuthException authEx) {
            Log.e("SWISHER", "Unrecoverable authentication exception: " + authEx.getMessage(), authEx);
        } catch (Exception e) {
            Log.e("SWISHER", "error", e);
        }
        return processor.empty();
    }

    private String getTokenWithRetry() throws Exception {
        String scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly";
        try {
            return GoogleAuthUtil.getToken(baseContext, email(), scope);
        } catch (IOException e) {
            Thread.sleep(200);
            return GoogleAuthUtil.getToken(baseContext, email(), scope);
        }
    }

//    public List<YouTubeSearchItem> search(String trackText, String token) {
//        try {
//            JSONObject result = new JSONObject(Utils.readURL(
//                    "https://gdata.youtube.com/feeds/api/videos?&v=2&alt=jsonc&q="+ URLEncoder.encode(trackText, "UTF-8")));
//            JSONArray entries = result.getJSONObject("data").getJSONArray("items");
//            List<YouTubeSearchItem> items = new LinkedList<YouTubeSearchItem>();
//            for (int i = 0; i< entries.length(); i++) {
//                JSONObject entry = entries.getJSONObject(i);
//                String id = entry.getString("id");
//                String title = entry.getString("title");
//                items.add( new YouTubeVideo(id, title));
//            }
//            Log.i("SWISHER", "search " + trackText + " " + items.size());
//            return items;
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return Collections.EMPTY_LIST;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return Collections.EMPTY_LIST;
//        }
//
//    }

    public List<YouTubeVideo> playlistItems(final String playlistID) {
        return invoke(new YouTubeProcessor<List<YouTubeVideo>>() {
            @Override
            public List<YouTubeVideo> work(Caller caller) throws Exception {
                return playlistItems(caller, playlistID);
            }
            @Override
            public List<YouTubeVideo> empty() {
                return Collections.emptyList();
            }
        });
    }

    private List<YouTubeVideo> playlistItems(Caller caller, String playlistID)
            throws Exception {
        return extractPlaylistItems(caller.invoke("/playlistItems?" +
                "part=contentDetails,snippet&" +
                "playlistId=" + playlistID + "&" +
                "maxResults=20"));
    }

    public List<YouTubeSearchItem> search(final String text) throws IOException,JSONException {
        return invoke(new YouTubeProcessor<List<YouTubeSearchItem>>() {
            public List<YouTubeSearchItem> work(Caller caller) throws Exception {
                return extractItems(caller.invoke("/search?" +
                        "part=snippet&" +
                        "q=" + URLEncoder.encode(text, "UTF8") + "&" +
                        "maxResults=20"));
            }
            @Override
            public List<YouTubeSearchItem> empty() {
                return Collections.emptyList();
            }
        });
    }

    private List<YouTubeVideo> extractPlaylistItems(JSONObject response)
            throws IOException,JSONException {
        JSONArray entries = response.getJSONArray("items");
        List<YouTubeVideo> items = new LinkedList<YouTubeVideo>();
        for (int i = 0; i< entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String videoID = entry.getJSONObject("contentDetails").getString("videoId");
            JSONObject snippet = entry.getJSONObject("snippet");
            String title = snippet.getString("title");
            String thumbnail = snippet.getJSONObject("thumbnails").
                    getJSONObject("high").getString("url");
            String highThumbnail = snippet.getJSONObject("thumbnails").
                    getJSONObject("high").getString("url");
            items.add( new YouTubeVideo(videoID, title, Uri.parse(thumbnail), Uri.parse(highThumbnail)) );
        }
        return items;
    }

    private List<YouTubeSearchItem> extractItems(JSONObject response)
            throws IOException,JSONException {
        JSONArray entries = response.getJSONArray("items");
        List<YouTubeSearchItem> items = new LinkedList<YouTubeSearchItem>();
        for (int i = 0; i< entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            JSONObject id = entry.getJSONObject("id");
            String kind = id.getString("kind");
            JSONObject snippet = entry.getJSONObject("snippet");
            String title = snippet.getString("title");
            String thumbnail = snippet.getJSONObject("thumbnails").
                    getJSONObject("default").getString("url");
            String highThumbnail = snippet.getJSONObject("thumbnails").
                    getJSONObject("high").getString("url");
            if (kind.equals("youtube#video")) {
                String videoID = id.getString("videoId");
                items.add( new YouTubeVideo(videoID, title, Uri.parse(thumbnail), Uri.parse(highThumbnail)) );
            } else if (kind.equals("youtube#playlist")) {
                String playlistID = id.getString("playlistId");
                items.add( new YouTubePlaylist(playlistID, title + " (playlist)", Uri.parse(thumbnail), Uri.parse(highThumbnail)) );
            }
        }
        return items;
    }
}
