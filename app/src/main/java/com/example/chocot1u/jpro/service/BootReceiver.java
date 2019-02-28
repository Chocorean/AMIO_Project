package com.example.chocot1u.jpro.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver","Device has booted.");
        context.startService(new Intent(context, CheckService.class));
        Log.d("BootReceiver","CheckService has started.");
    }
}
