package uk.co.thomasrynne.swisher.core;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.val;
import thomas.swisher.ui.MainActivity;
import uk.co.thomasrynne.swisher.Events;

public class ScreenOn {
    private EventBus eventBus = EventBus.getDefault();
    private AtomicBoolean activityReady = new AtomicBoolean(false);
    private Service service;
    private PowerManager powerManager;
    private PowerManager.WakeLock screenLock;
    private ExecutorService executor;

    private final Runnable wakeUpRunnable = new Runnable() {
        public void run() {

            if (!powerManager.isScreenOn()) { //use Display insteadut`

                //turn screen on and wait for the main activity to be ready
                //as this keeps the screen on when youtube is playing

                screenLock.acquire();

                int i = 0;
                while (i < 20 && !powerManager.isScreenOn()) {
                    safeSleep(50);
                    i += 1;
                }

                showMainActivity();

                int j = 0;
                while (i < 100 && !activityReady.get()) {
                    safeSleep(50);
                    i += 1;
                }
                safeSleep(1000);
                screenLock.release();
            } else {
                showMainActivity();
            }
        }
    };

    private Object subscriber = new Object() {
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.ActivityReadyEvent event) {
            activityReady.set(event.isActivityReady); //todo use java.concurrent gate
        }
    };
    public ScreenOn(Service service) {
        this.service = service;
        this.powerManager = (PowerManager) service.getBaseContext().getSystemService(Context.POWER_SERVICE);
        this.screenLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        this.eventBus.register(subscriber);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void wakeUp() {
        //Use Display.getState
        if (!powerManager.isScreenOn() || !activityReady.get()) {
            executor.execute(wakeUpRunnable);
        }
    }

    private void showMainActivity() {
        if (!activityReady.get()) {
            val intent = new Intent();
            intent.setClass(service, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            service.startActivity(intent);
        }
    }

    private static void safeSleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {}
    }

}
