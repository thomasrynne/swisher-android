package uk.co.thomasrynne.swisher.model

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import android.app.Service
import android.content.{Context, Intent}
import android.os.PowerManager
import de.greenrobot.event.EventBus
import uk.co.thomasrynne.swisher.{Events, PlayerActivity}

class ScreenOn(service:Service) {
  private val eventBus = EventBus.getDefault
  private val powerManager = service.getBaseContext.getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
  private val screenLock = powerManager.newWakeLock(
    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG")
  private val activityReady = new AtomicBoolean(false)
  private val subscriber = new {
    def onEventBackgroundThread(event: Events.ActivityReadyEvent) {
      activityReady.set(event.ready)
    }
  }
  eventBus.register(subscriber)
  val executor = Executors.newSingleThreadExecutor()

  def wakeUp {
    if (!powerManager.isScreenOn || !activityReady.get) {
      executor.execute(wakeUpRunnable)
    }
  }

  private val wakeUpRunnable = new Runnable { def run() {
    if (!powerManager.isScreenOn) {
      screenLock.acquire

      var i = 0; while (i < 20 && !powerManager.isScreenOn) { Thread.sleep(50); i += 1 }

      showMainActivity

      var j = 0; while (i < 100 && !activityReady.get) { Thread.sleep(50); i += 1 }
      Thread.sleep(1000)
      screenLock.release
    }
    else {
      showMainActivity
    }
  } }

  private def showMainActivity {
    if (!activityReady.get) {
      val intent = new Intent
      intent.setClass(service, classOf[PlayerActivity])
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.setAction(Intent.ACTION_MAIN)
      intent.addCategory(Intent.CATEGORY_LAUNCHER)
      service.startActivity(intent)
    }
  }
}
