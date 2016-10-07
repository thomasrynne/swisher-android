package thomas.swisher.service;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import android.util.Log;

import com.google.common.base.Optional;

import thomas.swisher.JsonEventHandler;
import thomas.swisher.todo.CardStore;
import thomas.swisher.ui.UIBackendEvents;
import thomas.swisher.utils.Utils;

/**
 * Decides what to do when a card is read.
 *
 * This can be put into record mode to record a new card.
 *
 * All interation with this class should be from the EventBus Background thread
 */
public class CardManager {

    private final EventBus eventBus;
    private final CardStore store;
    private final JsonEventHandler.Handler jsonEventHandler;

    private Optional<Utils.FlatJson> recordJSON = Optional.absent();

    public CardManager(EventBus eventBus, CardStore store, JsonEventHandler.Handler jsonEventHandler) {
        this.eventBus = eventBus;
        this.store = store;
        this.jsonEventHandler = jsonEventHandler;
    }

    public void record(String name, Utils.FlatJson json) {
        recordJSON = Optional.of(json);
        eventBus.post(new UIBackendEvents.RecordMode(name));
        Log.i("SWISHER", "in record mode for " + name);
    }

    public void cancelRecord() {
        recordJSON = Optional.absent();
        eventBus.post(new UIBackendEvents.RecordCompleteEvent());
        Log.i("SWISHER", "record mode cancelled");
    }

    private void toastMessage(String message) {
        eventBus.post(new UIBackendEvents.ToastEvent(message));
    }

    public void handleCard(String card) {
        if (recordJSON.isPresent()) {
            store.store(card, recordJSON.get().json);
            toastMessage("Recorded");
            recordJSON = Optional.absent();
            eventBus.post(new UIBackendEvents.RecordCompleteEvent());
            Log.i("SWISHER", "recorded: " + card);
        } else {
            Optional<JSONObject> entry = store.read(card);
            if (entry.isPresent()) {
                boolean handled = jsonEventHandler.handle(new Utils.FlatJson(entry.get()));
                if (!handled) {
                    toastMessage("Can not play card: " + card);
                }
            } else {
                toastMessage("Unknown card: " + card);
            }
        }
    }
}