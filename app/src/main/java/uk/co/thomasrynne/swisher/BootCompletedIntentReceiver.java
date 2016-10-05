package uk.co.thomasrynne.swisher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import thomas.swisher.SwisherService;

public class BootCompletedIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, SwisherService.class);
            context.startService(pushIntent);
        }
    }
}
