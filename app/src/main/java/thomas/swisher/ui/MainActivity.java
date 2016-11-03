package thomas.swisher.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import thomas.swisher.service.SwisherService;

import thomas.swisher.ui.youtube.YouTubeUI;
import thomas.swisher.youtube.YouTubeSource;
import trikita.anvil.Anvil;
import thomas.swisher.ui.model.Backend;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.model.UIModel;

public class MainActivity extends AppCompatActivity {

    public static final int YOUTUBE_AUTH_REQUEST_CODE = 1234;

    private EventBus eventBus = EventBus.getDefault();
    private OnKeyCardReader activityCardReader = new OnKeyCardReader(EventBus.getDefault());
    private final Handler handler = new Handler();

    private Backend backend;
    private UIModel.CoreModel coreUI;
    private YouTubeUI youTubeUI;

    private Object recordCardDialogListener = new Object() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEventMainThread(UIBackendEvents.RecordMode event) {
            RecordDialog.show(eventBus, MainActivity.this, event.name);
        }
    };

    @Override
    public void onBackPressed() {
        if (!coreUI.back()) {
            finish();
        }
    }

    @SuppressWarnings("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startService(new Intent(this, SwisherService.class));
        backend = new Backend(eventBus);
        coreUI = backend.coreUI();
        MainActivityLayout mainActivityLayout = new MainActivityLayout(backend.coreUI());
        backend.start();
        coreUI.menu().goToMenu(Core.MenuPath.Root);
        youTubeUI = new YouTubeUI(this, coreUI);

        Anvil.mount(findViewById(android.R.id.content), mainActivityLayout.view());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backend.stop();
        youTubeUI.destroy();
        Anvil.unmount(findViewById(android.R.id.content));
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return activityCardReader.onKey(keyCode) || super.onKeyUp(keyCode, event);
    }

    @Override protected void onResume() {
        super.onResume();
        eventBus.register(recordCardDialogListener);
        eventBus.post(new UIBackendEvents.ActivityReadyEvent(true));
        backend.resume();
        keepScreenOnFor5Seconds();
        Anvil.render();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == YOUTUBE_AUTH_REQUEST_CODE) {
            // go back to youtube menu if the user has come back from auth/play services install
            coreUI.menu().goToMenu(YouTubeSource.MENU_PATH);
        }
    }


    private void keepScreenOnFor5Seconds() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.postDelayed(() -> {
            Window window = getWindow();
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }, 5000);
    }

    @Override protected void onPause() {
        super.onPause();
        backend.pause();
        eventBus.unregister(recordCardDialogListener);
        eventBus.post(new UIBackendEvents.ActivityReadyEvent(false));
    }
}
