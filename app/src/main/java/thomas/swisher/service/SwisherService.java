package thomas.swisher.service;

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
import thomas.swisher.JsonEventHandler;
import thomas.swisher.MediaHandler;
import thomas.swisher.todo.CardStore;
import thomas.swisher.todo.Songs;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.tree.MainMenuTree;
import thomas.swisher.service.player.Player;
import thomas.swisher.service.localmedia.MediaStoreSource;


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
                val menuItems = menuTree.menuFor(event.menuPath);
                eventBus.post(new UIBackendEvents.MenuResponse(event.menuPath, Optional.of(menuItems)));
            } catch (Exception e) {
                Log.e("SWISHER", "Menu failure: " + event.menuPath, e);
                eventBus.post(new UIBackendEvents.MenuResponse(event.menuPath, Optional.absent()));
            }
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.CardWaveEvent cardEvent) {
            cardManager.handleCard(cardEvent.cardNumber);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.RecordPlayListEvent event) {
            cardManager.record("Playlist", jsonEventHandler.playListJson());
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.RecordCardEvent event) {
            cardManager.record(event.name, event.json);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.DoEvent event) {
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
        public void onEventBackgroundThread(UIBackendEvents.CancelRecordEvent event) {
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
        public void onEventBackgroundThread(UIBackendEvents.AutoPlayNextEvent event) {
            player.updateAutoPlayNext(event.playNext);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.PlayTrackByIndexEvent event) {
            player.playTrack(event.group, event.track);
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventBackgroundThread(UIBackendEvents.RefreshAlbumCoversEvent event) {
            Songs.init(getContentResolver());
        }
        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEventMainThread(UIBackendEvents.ToastEvent event) {
            Toast.makeText(getApplicationContext(), event.message, Toast.LENGTH_LONG).show();
        }
    };

    public void onCreate() {
        Songs.init(getContentResolver());
        val mediaStore = new MediaStoreSource(getBaseContext());
        menuTree = new MainMenuTree(eventBus,
            mediaStore.albumMenu(),
            mediaStore.tracksMenu()
        );
        jsonEventHandler = new JsonEventHandler(this, player, new MediaHandler[] {
            mediaStore.albumHandler(),
            mediaStore.trackHandler()
        });
		cardStore = new CardStore(this);
        this.cardManager = new CardManager(eventBus, cardStore, jsonEventHandler.jsonHandler);
        eventBus.register(listener);

        UIBackendEvents.RequestMenuEvent requestMenuEvent = eventBus.getStickyEvent(UIBackendEvents.RequestMenuEvent.class);
        if (requestMenuEvent != null) { //resend if there was already a request
            eventBus.postSticky(requestMenuEvent);
        }
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
        player.destroy();
	}
}
