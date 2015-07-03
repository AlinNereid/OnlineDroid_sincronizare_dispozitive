package com.example.edeph.androidwebsocket;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.github.nkzawa.socketio.client.Socket;

public class NotificationReceiver extends BroadcastReceiver {

    private String TAG = "NotificationBroadcastReceiver";
    private Socket mSocket;
    private Thread batteryThread;

    public NotificationReceiver(Socket sock, Thread battery)
    {
        this.batteryThread = battery;
        this.mSocket = sock;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String temp = intent.getStringExtra("notification_event");
        if(temp != null || temp != "")
        {
            batteryThread.run();
            Log.d(TAG, "SOMETHING SOMETHING " + temp);
            mSocket.emit("pushNotification", temp);

//            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.cancelAll();
        }
    }
}
