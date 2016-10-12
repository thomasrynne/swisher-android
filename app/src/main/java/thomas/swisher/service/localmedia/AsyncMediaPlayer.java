package thomas.swisher.service.localmedia;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import thomas.swisher.shared.Core;

/**
 * The media player methods can be slow, so to avoid holding up the UI updates
 * the calls are run on its own thread.
 */
public abstract class AsyncMediaPlayer {
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private AtomicBoolean ready = new AtomicBoolean(false);
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    public AsyncMediaPlayer() {
        mediaPlayer.setOnCompletionListener((mp) -> onFinished());
    }
    public void onFinished() {}
    public void onReady() {}
    public void play(String pathOrUrl, int seekToMillis, boolean playNow) {
        executor.execute(() -> {
            try {
                ready.set(false);
                mediaPlayer.reset();
                mediaPlayer.setDataSource(pathOrUrl);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(seekToMillis);
                if (playNow) {
                    mediaPlayer.start();
                }
                ready.set(true);
                onReady();
            } catch (IOException e) {
                Log.e("SWISHER", "Play failed: " + pathOrUrl, e);
            }
        });
    }
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
    public void seekTo(int toMillis) {
        mediaPlayer.seekTo(toMillis);
    }
    public void stop() {
        mediaPlayer.stop();
    }
    public void pause() {
        mediaPlayer.pause();
    }
    public void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }
    public void release() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        executor.shutdown();
    }
    public Core.PlayerProgress progress() {
        if (ready.get()) {
            return new Core.PlayerProgress(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(), true);
        } else {
            return Core.PlayerProgress.Null;
        }
    }
}
