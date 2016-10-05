package thomas.swisher.ui.youtube;

import android.util.Log;

import com.google.android.youtube.player.YouTubePlayer;

public class YouTubePlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

    @Override
    public void onLoading() { }

    @Override
    public void onLoaded(String s) { }

    @Override
    public void onAdStarted() { }

    @Override
    public void onVideoStarted() { }

    @Override
    public void onVideoEnded() { }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
        Log.e("YOUTUBE", errorReason.toString());
    }
}
