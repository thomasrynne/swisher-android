package thomas.swisher.ui.model;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Optional;

import thomas.swisher.shared.Core;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.utils.Utils;
import trikita.anvil.Anvil;

/**
 * The core ui class which the layouts wrap.
 * Anvil is used for the ui so generally there are no listeners
 * for updates, the view should just be frequently recreated.
 *
 * However, because of list view complications there are listeners
 * for when items are added/removed from a list.
 */
public class UIModel {

    private UIModel() {}

    public static class CoreModel {
        private final UIMenuModel.Core menu = new UIMenuModel.Core(this);
        private final UIControls.Core controls = new UIControls.Core(this);
        private final UITracks.Model tracks = new UITracks.Model(this);
        private final Handler handler = new Handler();
        private final Backend backend;

        private boolean isFullScreen = false;
        private boolean showMenu = false;
        private boolean showYouTube = true;
        private boolean isPlaying;
        private boolean playNext;
        private Core.PlayerProgress progress = Core.PlayerProgress.Null;

        private long lastRealProgressUpdateTime = 0;
        private long progressAtLastRealUpdate = 0;
        private long currentState = 0;
        private boolean activityPaused = true;

        public CoreModel(Backend backend) {
            this.backend = backend;
        }

        public UIMenuModel.Core menu() {
            return menu;
        }

        public UIControls.Core controls() {
            return controls;
        }

        public UITracks.Model tracks() {
            return tracks;
        }

        public void updatePaused(boolean value) {
            this.activityPaused = value;
            currentState++;
        }

        public boolean back() {
            if (menu.canBack()) {
                menu.back();
                return true;
            }
            if (showMenu()) {
                toggleShowMenu();
                Anvil.render();
                return true;
            }
            return false;
        }

        public void runLater(Runnable run, long delay) {
            handler.postDelayed(run, delay);
        }

        public void ensureMenuIsShowing() {
            if (!showMenu) {
                toggleShowMenu();
            }
        }

        public boolean showYouTube() {
            return showYouTube;
        }

        public void updateYouTubeVisible(boolean visible) {
            showYouTube = visible;
            Anvil.render();
        }

        public void updateFullScreen(boolean fullScreen) {
            this.isFullScreen = fullScreen;
            Anvil.render();
        }

        public void toggleFullScreen() {
            this.isFullScreen = !this.isFullScreen;
        }

        private class UpdateProgress implements Runnable {
            private final long state;
            UpdateProgress(long state) {
                this.state = state;
            }
            @Override
            public void run() {
                if (currentState == state) {
                    long timeSinceLastRealUpdate = System.currentTimeMillis() - lastRealProgressUpdateTime;
                    progress = progress.withProgressMillis((int) (progressAtLastRealUpdate + timeSinceLastRealUpdate));
                    Anvil.render();
                    triggerProgressUpdate();
                }
            }
        };

        private void triggerProgressUpdate() {
            runLater(new UpdateProgress(currentState), 1000);
        }

        public void update(UIBackendEvents.TracksLatest tracks) {
            this.isPlaying = tracks.isPlaying;
            this.playNext = tracks.playNext;
            this.progress = tracks.progress;
            this.tracks.latest(tracks.tracks, tracks.currentGroup, tracks.currentTrackInGroup);
            currentState++;
            if (!activityPaused && isPlaying) {
                this.progressAtLastRealUpdate = tracks.progress.progressMillis;
                this.lastRealProgressUpdateTime = System.currentTimeMillis();
                triggerProgressUpdate();
            }
        }

        public void toggleShowMenu() {
            showMenu = !showMenu;
            tracks().rebuild();
        }

        public boolean isFullScreen() {
            return isFullScreen;
        }

        public void toFullScreen() {
            isFullScreen = true;
        }

        public boolean showMenu() {
            return showMenu;
        }

        public boolean hasPlaylist() {
            return tracks.nonEmpty();
        }

        public boolean isPlaying() {
            return isPlaying;
        }

        public void pausePlay() {
            backend.pausePlay();
        }

        public Backend backend() {
            return backend;
        }

        public void addToPlaylist(Utils.FlatJson json) {
            backend.addToPlaylist(json);
        }

        public void play(Utils.FlatJson json) {
            backend.play(json);
        }

        public void record(String name, Utils.FlatJson json) {
            backend.record(name, json);
        }

        public void playTrackAt(int group, int track) {
            backend.playTrackAt(group, track);
        }

        public Optional<Uri> bigImage() {
            if (tracks().nonEmpty()) {
                return tracks().currentTrackImage();
            } else {
                return Optional.absent();
            }
        }

        public boolean isPlayNext() {
            return playNext;
        }

        public void sendAutoPlayNext(boolean playNext) {
            backend.sendAutoPlayNext(playNext);
        }

        public void seekTo(int toMillis) {
            backend.seekTo(toMillis);
        }

        public thomas.swisher.shared.Core.PlayerProgress progress() {
            return progress;
        }
    }
}
