package thomas.swisher.ui;

import android.view.KeyEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Captures key presses and sends a CardEvent when enough numeric keys are pressed
 */
public class OnKeyCardReader {

    private final EventBus eventBus;

    public OnKeyCardReader(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private StringBuilder numbers = new StringBuilder();

    public boolean onKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (numbers.length() >= 7) { //??? && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                String cardNumber = numbers.toString().substring(numbers.length() - 7);
                eventBus.post(new UIBackendEvents.CardWaveEvent(cardNumber));
            }
            numbers = new StringBuilder();
            return true;
        } else {
            //??? fakeFocus.requestFocus();
            int digit = keyCode - 7;
            if (digit >= 0 && digit <= 9) {
                numbers.append(digit);
                return true;
            } else {
                return false;
            }
        }
    }

}
