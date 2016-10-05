package thomas.swisher;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Optional;

import lombok.val;
import uk.co.thomasrynne.swisher.CardManager;
import uk.co.thomasrynne.swisher.CardStore;
import uk.co.thomasrynne.swisher.Events;
import uk.co.thomasrynne.swisher.Songs;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.tree.MainMenuTree;
import uk.co.thomasrynne.swisher.core.Player;
import uk.co.thomasrynne.swisher.sources.MediaStoreSource;


public class SwisherService extends Service {

	private EventBus eventBus = EventBus.getDefault();
    private Player player = new Player();
    private JsonEventHandler jsonEventHandler;
	private CardStore cardStore;
	private CardManager cardManager;
    private MainMenuTree menuTree;

    private final Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.ASYNC)
        public void onMenuRequest(UIBackendEvents.RequestMenuEvent event) {
            try {
                Log.i("X", "Menu requested " + event.menuPath);
                val menuItems = menuTree.menuFor(event.menuPath);
                eventBus.post(new UIBackendEvents.MenuResponse(event.menuPath, Optional.of(menuItems)));
            } catch (Exception e) {
                Log.e("SWISHER", "Menu failure: " + event.menuPath, e);
                eventBus.post(new UIBackendEvents.MenuResponse(event.menuPath, Optional.absent()));
            }
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.CardWaveEvent cardEvent) {
            cardManager.handleCard(cardEvent.cardNumber);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.RecordPlayListEvent event) {
            cardManager.record("Playlist", jsonEventHandler.playListJson());
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.RecordCardEvent event) {
            cardManager.record(event.name, event.json);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.DoEvent event) {
            jsonEventHandler.jsonHandler.handle(event.json);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.AddTrackEvent event) {
            jsonEventHandler.add(event.json);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.PlayTrackEvent event) {
            jsonEventHandler.play(event.json);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.ClearPlayListEvent event) {
            player.clearPlaylist();
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.PausePlayEvent event) {
            player.pausePlay();
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.CancelRecordEvent event) {
            cardManager.cancelRecord();
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.RemoveTrackEvent event) {
            player.remove(event.position);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.SwapTracksEvent event) {
            player.swap(event.fromPosition, event.toPosition);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.PlayTrackByIndexEvent event) {
            player.playTrack(event.group, event.track);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(Events.RefreshAlbumCoversEvent event) {
            Songs.init(getContentResolver());
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventMainThread(Events.ToastEvent event) {
            Toast.makeText(getApplicationContext(), event.message, Toast.LENGTH_LONG).show();
        }
    };

    public void onCreate() {
        Log.i("S", "oncreate service");
        Songs.init(getContentResolver());
//        MediaStore mediaStore = new MediaStore(getBaseContext());
//        menu = new Controls(getBaseContext(), mediaStore);
        val mediaStore = new MediaStoreSource(getBaseContext());
        menuTree = new MainMenuTree(eventBus,
            mediaStore.albumMenu(),
            mediaStore.tracksMenu()
        );
        jsonEventHandler = new JsonEventHandler(this, player, new MediaHandler[] {
            mediaStore.albumHandler(),
            mediaStore.trackHandler()
//                menu.mediaStoreSource().handler(),
//                RadioStations.handler(),
//                new YouTubeSource(getBaseContext()).handler(),
//                new Podcasts(getBaseContext()).handler()
        });
		cardStore = new CardStore(this);
        this.cardManager = new CardManager(eventBus, cardStore, jsonEventHandler.jsonHandler);
        eventBus.register(listener);

        UIBackendEvents.RequestMenuEvent requestMenuEvent = eventBus.getStickyEvent(UIBackendEvents.RequestMenuEvent.class);
        if (requestMenuEvent != null) { //resend if there was already a request
            eventBus.postSticky(requestMenuEvent);
        }
        eventBus.post(new Events.ServiceStarted());
    };
    
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        eventBus.unregister(listener);
	}
}
