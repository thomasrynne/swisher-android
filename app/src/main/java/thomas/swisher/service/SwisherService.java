package thomas.swisher.service;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.util.List;

import thomas.swisher.JsonEventHandler;
import thomas.swisher.MediaHandler;
import thomas.swisher.service.player.CurrentPlaylist;
import thomas.swisher.shared.Core;
import thomas.swisher.todo.CardStore;
import thomas.swisher.todo.Songs;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.tree.MainMenuTree;
import thomas.swisher.service.player.Player;
import thomas.swisher.service.localmedia.MediaStoreSource;
import thomas.swisher.utils.Utils;


public class SwisherService extends Service {

	private EventBus eventBus = EventBus.getDefault();
    private Player player;
    private JsonEventHandler jsonEventHandler;
	private CardStore cardStore;
	private CardManager cardManager;
    private MainMenuTree menuTree;

    private final Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.ASYNC)
        public void onMenuRequest(UIBackendEvents.RequestMenuEvent event) {
            try {
                Core.MenuItemList menuItems = menuTree.menuFor(event.menuPath);
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
        MediaStoreSource mediaStore = new MediaStoreSource(getBaseContext());
        menuTree = new MainMenuTree(eventBus,
            mediaStore.albumMenu(),
            mediaStore.tracksMenu()
        );
        SharedPreferences playlistPreferences = getApplicationContext().getSharedPreferences(
                "current_playlist", Context.MODE_PRIVATE);
        player = new Player(new CurrentPlaylist(storePlaylist(playlistPreferences)));
        jsonEventHandler = new JsonEventHandler(this, player, new MediaHandler[] {
            mediaStore.albumHandler(),
            mediaStore.trackHandler()
        });
        restorePlaylist(playlistPreferences);
        cardStore = new CardStore(this);
        this.cardManager = new CardManager(eventBus, cardStore, jsonEventHandler.jsonHandler);
        eventBus.register(listener);

        UIBackendEvents.RequestMenuEvent requestMenuEvent = eventBus.getStickyEvent(UIBackendEvents.RequestMenuEvent.class);
        if (requestMenuEvent != null) { //resend if there was already a request
            eventBus.postSticky(requestMenuEvent);
        }
    }

    private CurrentPlaylist.Store storePlaylist(SharedPreferences playlistPreferences) {
        return (json) -> {
            SharedPreferences.Editor editor = playlistPreferences.edit();
            String text = Utils.json().add("items", json).build().asString();
            editor.putString("previous-playlist", text);
            editor.commit();
        };
    }

    private void restorePlaylist(SharedPreferences playlistPreferences) {
        try {
            String json = playlistPreferences.getString("previous-playlist", "");
            if (!json.isEmpty()) {
                List<Player.PlaylistEntry> entries = jsonEventHandler.playlistEntries(
                        Utils.FlatJson.parse(json).getList("items"));
                player.initialisePlaylist(entries);
            }
        } catch (Exception e) {
            Log.e("SWISHER", "Previous playlist restore failed", e);
        }
    }

    ;
    
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
