package com.example.edeph.androidwebsocket;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.github.nkzawa.socketio.client.Socket;

public class BatteryStatusThread implements Runnable
{
    private String TAG = "BATTERY_THREAD";
    private Socket mSocket;
    private Context ctx;
    private int sleepTime = 3000;

    public BatteryStatusThread(Socket sock, Context context)
    {
        this.mSocket = sock;
        this.ctx = context;
    }

    @Override
    public void run()
    {
        //setup the battery-related things
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = ctx.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float)scale;

        mSocket.emit("battery_status", batteryPct * 100);
    }
}
