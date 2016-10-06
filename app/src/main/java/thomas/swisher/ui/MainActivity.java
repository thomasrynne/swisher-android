package thomas.swisher.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import lombok.val;
import thomas.swisher.service.SwisherService;
import static thomas.swisher.ui.view.AnvilExtras.*;
import trikita.anvil.Anvil;
import thomas.swisher.ui.view.ControlView;
import thomas.swisher.ui.model.Backend;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.model.UIModel;
import thomas.swisher.ui.view.TracksView;
import thomas.swisher.ui.view.TreeMenuView;

import static trikita.anvil.DSL.*;

public class MainActivity extends AppCompatActivity {

    private EventBus eventBus = EventBus.getDefault();
    private OnKeyCardReader activityCardReader = new OnKeyCardReader(EventBus.getDefault());
    private final Handler handler = new Handler();

    private Backend backend;
    private UIModel.Core coreUI;
    private TreeMenuView treeMenuView;
    private ControlView controlView;
    private TracksView tracksView;

    private Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEventMainThread(UIBackendEvents.RecordMode event) {
            RecordDialog.show(eventBus, MainActivity.this, event.name);
        }
    };

    private Anvil.Renderable mainView() {
        return () -> {
            linearLayout(() -> {
                size(MATCH, MATCH);
                padding(0,0,0,0);
                padding(15);
                orientation(LinearLayout.HORIZONTAL);

                frameLayout(() -> { //---------------------------------[Tracks]
                    size(FILL, FILL);
                    weight(1);
                    gravity(Gravity.TOP);
                    tracksView.view();
                });

                linearLayout(() -> { //--------------------------------[Controls]
                    weight(1);
                    gravity(Gravity.RIGHT);
                    orientation(LinearLayout.VERTICAL);
                    height(FILL);

                    frameLayout(() -> { //-----------------------------[Buttons]
                        weight(1);
                        gravity(Gravity.TOP);
                        controlView.view();
                    });

                    linearLayout(() -> { //----------------------------[Main Image]
                        weight(1);
                        gravity(Gravity.BOTTOM);

                        imageView(() -> {
                            weight(1);
                            centerInParent();
                            //padding(dip(15));
                            gravity(Gravity.CENTER);
                            glideURI(coreUI.bigImage());
                        });
                    });
                });

                frameLayout(() -> { //---------------------------------[Menu]
                    visibility(coreUI.showMenu());
                    treeMenuView.view();
                    gravity(Gravity.TOP);
                    size(FILL, FILL);
                    weight(1);
                });
            });
        };
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startService(new Intent(this, SwisherService.class));
        backend = new Backend(eventBus);
        coreUI = backend.coreUI();
        treeMenuView = new TreeMenuView(coreUI.menu());
        controlView = new ControlView(coreUI.controls());
        tracksView = new TracksView(coreUI.tracks());
        backend.start();
        coreUI.menu().goToMenu(Core.MenuPath.Root);

        Anvil.mount(findViewById(android.R.id.content), mainView());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backend.stop();
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return activityCardReader.onKey(keyCode) || super.onKeyUp(keyCode, event);
    }

    @Override protected void onResume() {
        super.onResume();
        eventBus.register(listener);
        eventBus.post(new UIBackendEvents.ActivityReadyEvent(true));
        keepScreenOnFor5Seconds();
        Anvil.render();
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
        eventBus.unregister(listener);
        eventBus.post(new UIBackendEvents.ActivityReadyEvent(false));
    }
}
