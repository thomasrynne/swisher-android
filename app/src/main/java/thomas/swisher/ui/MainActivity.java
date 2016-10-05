package thomas.swisher.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;


import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import lombok.val;
import thomas.swisher.R;
import thomas.swisher.SwisherService;
import thomas.swisher.ui.youtube.YouTubePlayerStateChangeListener;
import static thomas.swisher.ui.view.AnvilExtras.*;
import trikita.anvil.Anvil;
import thomas.swisher.ui.view.ControlView;
import uk.co.thomasrynne.swisher.Events;
import thomas.swisher.ui.model.Backend;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.model.UIModel;
import thomas.swisher.ui.view.TracksView;
import thomas.swisher.ui.view.TreeMenuView;

import static trikita.anvil.DSL.*;

public class MainActivity extends AppCompatActivity {

    private static final String YOUTUBE_DEVELOPER_KEY = "AIzaSyCLhKjwrLP6FtlQ8GYBcuelPGZ8hMwokYU";
    private static final int youTubeFragmentId = 1;
    private static final int tracksFragmentId = 2;
    private static final int controlFragmentId = 3;

    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private EventBus eventBus = EventBus.getDefault();
    private YouTubePlayer youTubePlayer;
    private OnKeyCardReader activityCardReader = new OnKeyCardReader(EventBus.getDefault());
    private final Handler handler = new Handler();

    private Backend backend;
    private UIModel.Core coreUI;
    private TreeMenuView treeMenuView;
    private ControlView controlView;
    private TracksView tracksView;

    private Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onEventMainThread(Events.RecordMode event) {
            RecordDialog.show(eventBus, MainActivity.this, event.name);
        }
//        public void onEventMainThread(Events.YouTubeControlEvent event) {
//            if (youTubePlayer != null) {
//                Log.i("SWISHER", "youtube event " + event.action + " " + event.videoID);
//                switch (event.action) {
//                    case Clear:
//                        youTubePlayer.pause();
//                        //imageArea.setVisibility(View.VISIBLE);
//                        //youTubeFrame.setVisibility(View.GONE);
//                        break;
//                    case PlayFromStart:
//                        //imageArea.setVisibility(View.GONE);
//                        //youTubeFrame.setVisibility(View.VISIBLE);
////                        if (event.videoID.equals(youTubeVideoID)) {
////                            Log.i("SWISHER", "playfromstart");
////                            youTubePlayer.seekToMillis(0);
////                            youTubePlayer.play();
////                        } else {
////                            youTubePlayer.loadVideo(event.videoID);
////                            youTubeVideoID = event.videoID;
////                        }
//                        break;
//                    case CueFromStart:
////                        imageArea.setVisibility(View.VISIBLE);
////                        youTubeFrame.setVisibility(View.GONE);
////                        Log.i("SWISHER", "cueBeginning video " + event.videoID);
////                        if (event.videoID.equals(youTubeVideoID)) {
////                            youTubePlayer.seekToMillis(0);
////                        } else {
////                            youTubePlayer.cueVideo(event.videoID);
////                            youTubeVideoID = event.videoID;
////                        }
//                        break;
//                    case Pause:
//                        youTubePlayer.pause();
//                        break;
//                    case PausePlay:
////                        imageArea.setVisibility(View.GONE);
////                        youTubeFrame.setVisibility(View.VISIBLE);
////                        if (youTubePlayer.isPlaying()) {
////                            youTubePlayer.pause();
////                        } else {
////                            youTubePlayer.play();
////                        }
//                        break;
//                }
//            }
//        }

    };

    private Anvil.Renderable mainView() {
        return () -> {
            Log.i("X", "Main view");
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

        Log.i("S", "MainActivity onCreate");

        backend = new Backend(eventBus);
        coreUI = backend.coreUI();
        treeMenuView = new TreeMenuView(coreUI.menu());
        controlView = new ControlView(coreUI.controls());
        tracksView = new TracksView(coreUI.tracks());
        backend.start();
        coreUI.menu().goToMenu(Core.MenuPath.Root);


        Anvil.mount(findViewById(android.R.id.content), mainView());

        youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance();
        if (bundle == null) {
            YouTubePlayerFragment fragment = new YouTubePlayerFragment(); //check this
            FragmentManager manager = getSupportFragmentManager();
            //noinspection ResourceType
//            manager.beginTransaction()
//                    .replace(youTubeFragmentId, youTubePlayerFragment)
//                    .addToBackStack(null)
//                    .commit();
        }

        youTubePlayerFragment.initialize(YOUTUBE_DEVELOPER_KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
                Log.i("YOUTUBE", "Initialized ");
                youTubePlayer.cueVideo("yWB3bYK3-E4");
                youTubePlayer.setPlayerStateChangeListener(new YouTubePlayerStateChangeListener());
                eventBus.post(new Events.ActivityReadyEvent(true, true));
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                Log.i("YOUTUBE", "Error " + youTubeInitializationResult);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backend.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return activityCardReader.onKey(keyCode) || super.onKeyUp(keyCode, event);
    }

    @Override protected void onResume() {
        super.onResume();
        eventBus.register(listener);
        eventBus.post(new Events.ActivityReadyEvent(true, youTubePlayer != null));
        keepScreenOnFor5Seconds();
        Anvil.render();
    }

    private void keepScreenOnFor5Seconds() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                val window = getWindow();
                if (window != null) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        }, 5000);
    }

    @Override protected void onPause() {
        super.onPause();
        eventBus.unregister(listener);
        eventBus.post(new Events.ActivityReadyEvent(false, false));
    }


}
