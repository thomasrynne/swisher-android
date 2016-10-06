package thomas.swisher.ui.model;

import android.util.Log;

/**
 */
public class UIControls {

    public enum ButtonState { STOP, PAUSE, PLAY, NONE }

    public static class Core {
        private final UIModel.Core uiRoot;

        public Core(UIModel.Core uiRoot) {
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

        public void updatePlayNext(Object isChecked) {

        }
    }
}
