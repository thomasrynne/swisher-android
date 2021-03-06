package thomas.swisher.ui.model;

/**
 */
public class UIControls {

    public enum ButtonState { STOP, PAUSE, PLAY, NONE }

    public static class Core {
        private final UIModel.CoreModel uiRoot;

        public Core(UIModel.CoreModel uiRoot) {
            this.uiRoot = uiRoot;
        }

        public boolean isPlayNext() {
            return uiRoot.isPlayNext();
        }

        public void pausePlay() {
            uiRoot.pausePlay();
        }

        public ButtonState buttonState() {
            return uiRoot.buttonState();
        }

        public void toFullScreen() {
            uiRoot.toFullScreen();
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

        public UIModel.CoreModel uiRoot() {
            return uiRoot;
        }
    }
}
