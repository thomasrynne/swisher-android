package uk.co.thomasrynne.swisher;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import android.util.Log;

import thomas.swisher.JsonEventHandler;

public class CardManager {

    private final EventBus eventBus;
    private final CardStore store;
    private final JsonEventHandler.Handler jsonEventHandler;
    private Utils.FlatJson recordJSON = null;

    public CardManager(EventBus eventBus, CardStore store, JsonEventHandler.Handler jsonEventHandler) {
        this.eventBus = eventBus;
        this.store = store;
        this.jsonEventHandler = jsonEventHandler;
    }

    public void record(String name, Utils.FlatJson json) {
        synchronized (this) {
            recordJSON = json;
        }
        eventBus.post(new Events.RecordMode(name));
        Log.i("SWISHER", "in record mode " + name);
    }

    public void cancelRecord() {
        synchronized (this) {
            recordJSON = null;
        }
        eventBus.post(new Events.RecordCompleteEvent());
        Log.i("SWISHER", "record mode cancelled");
    }

    private void toastMessage(String message) {
        eventBus.post(new Events.ToastEvent(message));
    }

    public void handleCard(String card) {
        synchronized (this) {
            if (recordJSON != null) {
                store.store(card, recordJSON.json);
                toastMessage("Recorded");
                recordJSON = null;
                eventBus.post(new Events.RecordCompleteEvent());
                Log.i("SWISHER", "recorded " + card);
            } else {
                JSONObject entry = store.read(card);
                if (entry != null) {
                    boolean handled = jsonEventHandler.handle(new Utils.FlatJson(entry));
                    if (!handled) {
                        toastMessage("Can not play card: " + card);
                    }
                } else {
                    toastMessage("Unknown card: " + card);
                }
            }
        }
    }
}