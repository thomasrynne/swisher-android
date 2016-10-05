package thomas.swisher.ui.model;

import android.net.Uri;
import android.util.Log;

import com.google.common.base.Optional;

import uk.co.thomasrynne.swisher.Utils;

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

    public static class Core {
        private final UIMenuModel.Core menu = new UIMenuModel.Core(this);
        private final UIControls.Core controls = new UIControls.Core(this);
        private final UITracks.Model tracks = new UITracks.Model(this);
        private final Backend backend;

        private boolean isFullScreen = false;
        private boolean showMenu = false;
        private boolean isPlaying;

        public Core(Backend backend) {
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

        public void updateIsPlaying(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }

        public void toggleShowMenu() {
            showMenu = !showMenu;
        }

        public void fullScreen() {
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
    }
}
