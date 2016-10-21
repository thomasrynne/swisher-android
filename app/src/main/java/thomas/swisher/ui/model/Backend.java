package thomas.swisher.ui.model;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import trikita.anvil.Anvil;
import thomas.swisher.utils.Utils;
import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;

/**
 * This handles all the communication with the service (using EventBus)
 */
public class Backend {

    private final EventBus eventBus;
    private final UIModel.CoreModel core = new UIModel.CoreModel(this);

    public Backend(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public UIModel.CoreModel coreUI() {
        return core;
    }

    public void start() {
        eventBus.register(listener);
        UIBackendEvents.TracksLatest tracks = eventBus.getStickyEvent(UIBackendEvents.TracksLatest.class);
        if (tracks != null) {
            update(tracks);
        }
    }

    private void update(UIBackendEvents.TracksLatest tracks) {
        core.update(tracks);
    }

    public void stop() {
        eventBus.unregister(listener);
    }

    private final Object listener = new Object() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onMenuResponse(UIBackendEvents.MenuResponse response) {
            core.menu().onMenuResponse(response.menuPath, response.result);
        }
        @Subscribe(threadMode = ThreadMode.MAIN)
        public void latestTracks(UIBackendEvents.TracksLatest tracks) {
            update(tracks);
            Anvil.render();
        }
    };

    public void menuFor(Core.MenuPath path) {
        eventBus.postSticky(new UIBackendEvents.RequestMenuEvent(path));
    }

    public void pausePlay() {
        eventBus.post(new UIBackendEvents.PausePlayEvent());
    }

    public void addToPlaylist(Utils.FlatJson json) {
        eventBus.post(new UIBackendEvents.AddTrackEvent(json));
    }

    public void play(Utils.FlatJson json) {
        eventBus.post(new UIBackendEvents.PlayTrackEvent(json));
    }

    public void record(String name, Utils.FlatJson json) {
        eventBus.post(new UIBackendEvents.RecordCardEvent(name, json));
    }

    public void playTrackAt(int group, int track) {
        eventBus.post(new UIBackendEvents.PlayTrackByIndexEvent(group, track));
    }

    public void sendAutoPlayNext(boolean playNext) {
        eventBus.post(new UIBackendEvents.AutoPlayNextEvent(playNext));
    }

    public void removePlaylistItem(int position) {
        eventBus.post(new UIBackendEvents.RemoveTrackEvent(position));
    }

    public void swapPlaylistItems(int a, int b) {
        eventBus.post(new UIBackendEvents.SwapTracksEvent(a, b));
    }

    public void seekTo(int toMillis) {
        eventBus.post(new UIBackendEvents.SeekToEvent(toMillis));
    }

    public void pause() {
        core.updatePaused(true);
    }

    public void resume() {
        core.updatePaused(false);
    }
}
