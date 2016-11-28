package com.ivan_pc.downloadpicture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SwitchReceiver extends BroadcastReceiver {
    public SwitchReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Receiver", "startDownloading");
        MyIntentService.startDownloading(context);
    }
}
