package uk.co.thomasrynne.swisher.util

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import android.media.MediaPlayer.OnCompletionListener
import android.media.{AudioManager, MediaPlayer}
import uk.co.thomasrynne.swisher.model.{PlayerProgress, UnknownPlayerProgress, KnownPlayerProgress}

/**
 * A wrapper around MediaPlayer which return from play more quickly
 *
 */
class SafeMediaPlayer(onComplete:()=>Unit = ()=>{}, onProgressChange:(PlayerProgress=>Unit)=(_)=>{}) {
  private val mediaPlayer = new MediaPlayer
  mediaPlayer.setOnCompletionListener(new OnCompletionListener {
    override def onCompletion(mediaPlayer: MediaPlayer) { onComplete() }
  })
  private val ready = new AtomicBoolean(false)
  private val executor = Executors.newSingleThreadExecutor()
  def play(pathOrUrl:String, millis:Int=0, playNow:Boolean=true) {
    executor.submit(new Runnable() { def run() {
      ready.set(false)
      mediaPlayer.reset
      mediaPlayer.setDataSource(pathOrUrl)
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
      mediaPlayer.prepare
      mediaPlayer.seekTo(millis)
      if (playNow) {
        mediaPlayer.start
      }
      ready.set(true)
      onProgressChange(progress)
    } })
  }
  def currentPosition = mediaPlayer.getCurrentPosition
  def seekTo(millis:Int) { mediaPlayer.seekTo(millis) }
  def stop { mediaPlayer.stop }
  def pause { mediaPlayer.pause }
  def pausePlay { if (mediaPlayer.isPlaying) mediaPlayer.pause else mediaPlayer.start }
  def progress = {
    if (ready.get) {
      KnownPlayerProgress(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition())
    } else {
      UnknownPlayerProgress
    }
  }
  def release { mediaPlayer.stop; mediaPlayer.release; executor.shutdown }
}
