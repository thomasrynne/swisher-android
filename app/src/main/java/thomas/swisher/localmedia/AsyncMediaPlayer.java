package thomas.swisher.localmedia;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The media player methods can be slow, so to avoid holding up the UI updates
 * the calls are run on its own thread.
 */
public class AsyncMediaPlayer {
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private AtomicBoolean ready = new AtomicBoolean(false);
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    public AsyncMediaPlayer(Runnable onComplete) {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onComplete.run();
            }
        });
    }
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
            } catch (IOException e) {
                Log.e("SWISHER", "Play failed: " + pathOrUrl, e);
            }
        });
    }
    public int currentPosition() {
        return mediaPlayer.getCurrentPosition();
    }
    public void seekTo(int millis) {
        mediaPlayer.seekTo(millis);
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
    public int progress() {
        if (ready.get()) {
            //KnownPlayerProgress(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition())
        } else {
            //UnknownPlayerProgress
        }
        return 0;
    }
}
