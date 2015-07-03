package com.example.edeph.androidwebsocket;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

@SuppressLint("OverrideAbstract")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService
{
    private String TAG = "NOTIFICATION_LISTENER";
    private Context ctx;

    public NotificationListener()
    {
        Log.d(TAG, "DEFAULT CONSTRUCTOR");
    }

    public NotificationListener(Context context)
    {
        this.ctx = context;
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "OnCreate");
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        Log.d(TAG + " notification posted", sbn.getId() + " | " + sbn.getNotification());
        Intent broadcastReceiver = new Intent("notification");
        //edit here to get the relevant informations
        Notification mNotification=sbn.getNotification();
        Bundle extras = mNotification.extras;
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
        CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
        String myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        broadcastReceiver.putExtra("notification_event", myAndroidDeviceId + "|" + sbn.getId() + "|" + sbn.getPackageName() + "|" + notificationTitle + "|" + notificationText);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(broadcastReceiver);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d(TAG, sbn.getPackageName() + " notification removed");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "DESTROYED");
    }
}
