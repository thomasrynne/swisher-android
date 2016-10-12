package thomas.swisher.ui.model;

import android.util.Log;

/**
 */
public class UIControls {

    public enum ButtonState { STOP, PAUSE, PLAY, NONE }

    public static class Core {
        private final UIModel.CoreModel uiRoot;

        public Core(UIModel.CoreModel uiRoot) {
            this.uiRoot = uiRoot;
        }

        public boolean isPlaying() {
            return uiRoot.isPlaying();
        }

        public boolean isPlayNext() {
            return uiRoot.isPlayNext();
        }

        public void pausePlay() {
            uiRoot.pausePlay();
        }

        public ButtonState buttonState() {
            if (uiRoot.hasPlaylist()) {
                Log.i("X", "Is playing " + isPlaying());
                return isPlaying() ? ButtonState.PAUSE : ButtonState.PLAY;
            } else {
                return ButtonState.NONE;
            }

        }

        public void toggleMenu() {
            uiRoot.toggleShowMenu();
        }

        public void fullScreen() {
            uiRoot.fullScreen();
        }

        public void sendAutoPlayNext(boolean playNext) {
            uiRoot.sendAutoPlayNext(playNext);
        }

        public void seekTo(int toMillis) {
            uiRoot.seekTo(toMillis);
        }

        public thomas.swisher.shared.Core.PlayerProgress progress() {
            return uiRoot.progress();
        }

        public String duration() {
            if (uiRoot.progress().enabled) {
                int totalSeconds = uiRoot.progress().totalMillis / 1000;
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                if (seconds < 10) {
                    return minutes + ":0" + seconds;
                } else {
                    return minutes + ":" + seconds;
                }
            } else {
                return "";
            }
        }
    }
}
