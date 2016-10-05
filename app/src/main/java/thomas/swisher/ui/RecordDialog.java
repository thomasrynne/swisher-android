package thomas.swisher.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import thomas.swisher.ui.OnKeyCardReader;
import uk.co.thomasrynne.swisher.Events;

/**
 */
public class RecordDialog {

    public static void show(final EventBus eventBus, Activity activity, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage("Wave card now to record");
        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface arg0, int arg1) {
                eventBus.post(new Events.CancelRecordEvent());
            }});
        final AlertDialog dialog = builder.create();
        final Object[] listener = new Object[] { null };
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (listener[0] != null) { eventBus.unregister(listener[0]); }
            }
        });
        dialog.setCanceledOnTouchOutside(true);
        listener[0] = new Object() {
            @Subscribe(threadMode = ThreadMode.MAIN)
            public void onEventMainThread(Events.RecordCompleteEvent event) {
                dialog.hide();
            }
        };
        eventBus.register(listener[0]);
        final OnKeyCardReader cardReader = new OnKeyCardReader(eventBus);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                return cardReader.onKey(keyCode);
            }
        });
        dialog.show();
    }
}
