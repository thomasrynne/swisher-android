package thomas.swisher.ui.model;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import trikita.anvil.Anvil;
import uk.co.thomasrynne.swisher.Events;
import uk.co.thomasrynne.swisher.Utils;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;

/**
 * This handles all the communication with the service (using EventBus)
 */
public class Backend {

    private final EventBus eventBus;
    private final UIModel.Core core = new UIModel.Core(this);

    public Backend(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public UIModel.Core coreUI() {
        return core;
    }

    public void start() {
        eventBus.register(listener);
    }

    public void stop() {
        eventBus.unregister(listener);
    }

    private final Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onMenuResponse(UIBackendEvents.MenuResponse response) {
            core.menu().onMenuResponse(response.menuPath, response.menuItemList);
        }
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void latestTracks(UIBackendEvents.TracksLatest tracks) {
            Log.i("X", "Latest tracks " + tracks.isPlaying);
            core.updateIsPlaying(tracks.isPlaying);
            core.tracks().latest(tracks.tracks);
            Anvil.render();
        }
    };

    public void menuFor(Core.MenuPath path) {
        eventBus.postSticky(new UIBackendEvents.RequestMenuEvent(path));
    }

    public void cancelMenu(Core.MenuPath path) {
        eventBus.post(new UIBackendEvents.CancelMenuEvent(path));
    }

    public void pausePlay() {
        eventBus.post(new UIBackendEvents.PausePlayEvent());
    }

    public void addToPlaylist(Utils.FlatJson json) {
        Log.i("C", "add json");
        eventBus.post(new UIBackendEvents.AddTrackEvent(json));
    }

    public void play(Utils.FlatJson json) {
        Log.i("C", "play json");
        eventBus.post(new UIBackendEvents.PlayTrackEvent(json));
    }

    public void record(String name, Utils.FlatJson json) {
        eventBus.post(new Events.RecordCardEvent(name, json));
    }

    public void playTrackAt(int group, int track) {
        eventBus.post(new UIBackendEvents.PlayTrackByIndexEvent(group, track));
    }
}
