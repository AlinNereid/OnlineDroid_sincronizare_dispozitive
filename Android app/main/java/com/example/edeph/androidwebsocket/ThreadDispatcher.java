package com.example.edeph.androidwebsocket;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class ThreadDispatcher extends Service
{
    private String TAG ="SERVICE";
    private Thread forwardSms = null;
    private Thread sendSms = null;
    private Thread findMyPhone = null;
    private Thread ringMyPhone = null;
    private Thread syncAgenda = null;
    private Socket mSocket;
    private final IBinder mBinder = new LocalBinder();
    private Context ctx;
    private boolean fwd_on, snd_on, psh_on, fmd_on, fmr_on;
    private NotificationReceiver pushReceiver;
    private Thread batteryStatus = null;
    private String myAndroidDeviceId;
    private String deviceName;
    private IncomingSms smsReceiver;
    private PhonecallReceiver callReceiver;

    @Override
    public void onCreate()
    {
        //send back the phone type
        myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceName = Devices.getDeviceName();
        this.ctx = this;
        fwd_on = false;
        snd_on = false;
        psh_on = false;
        fmd_on = false;
        fmr_on = false;
        Log.d(TAG, "SERVICE CREATE");
        //creates connection with server
        try {
            mSocket = IO.socket("http://limitless-savannah-8511.herokuapp.com");
        } catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
//        Intent loginActivity = new Intent("SERVICE_CREATED");
//
//        LocalBroadcastManager.getInstance(this).sendBroadcast(loginActivity);
//

        SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putBoolean("fwd", false);
        settingsEditor.putBoolean("snd", false);
        settingsEditor.putBoolean("psh", false);
        settingsEditor.putBoolean("fmd", false);
        settingsEditor.putBoolean("fmr", false);
        settingsEditor.commit();

        Log.d(TAG, "Socket created!");

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.android_full)
                        .setContentTitle("OnlineDroid")
                        .setContentText("You are connected!");
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        Intent notificationIntent = new Intent(this, MainScreenActivity.class);
        Notification notification = mBuilder.build();
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notification.setLatestEventInfo(this, "OnlineDroid", "open me!", intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        startForeground(1337, notification);

        Log.d(TAG, "SERVICE CREATE END");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bind requested");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        ThreadDispatcher getService() {
            Log.d(TAG, "Activity bound to service");
            return ThreadDispatcher.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "ThreadDispatcher onStartCommand");

        //Standard events for connection/disconnection/error
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Connected successfully!");
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener()
        {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Disconnected!");
                checkAndStopRunningThreads();
                //intent back to login page ?
                stopSelf();
            }
        }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "ERR: " + args[0]);
            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "CONERR " + args[0]);
            }
        });

        mSocket.on("loginSucceeded", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "RECEIVED LOGIN SUCCESS");
                //broadcast loginOK
                Intent loginActivity = new Intent("LOGIN_SUCCESS");
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(loginActivity);

                //sync settings
                mSocket.on("androidsyncSettings", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d(TAG, "I received the synced settings from server");
                        JSONObject data = (JSONObject) args[0];
                        SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
                        SharedPreferences.Editor settingsEditor = settings.edit();
                        try {
                            settingsEditor.putBoolean("fwd", data.getBoolean("fwd"));
                            settingsEditor.putBoolean("snd", data.getBoolean("snd"));
                            settingsEditor.putBoolean("psh", data.getBoolean("psh"));
                            settingsEditor.putBoolean("fmd", data.getBoolean("fmd"));
                            settingsEditor.putBoolean("fmr", data.getBoolean("fmr"));
                            settingsEditor.commit();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, "ERR sync settings");
                            Toast.makeText(ctx, "Could not sync settings!", Toast.LENGTH_SHORT).show();
                        }
                        reloadThreads();
                    }
                });

                //Send initial settings
                SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
                JSONObject jsonSettings = new JSONObject();
                try {
                    jsonSettings.put("fwd", settings.getBoolean("fwd", false));
                    jsonSettings.put("snd", settings.getBoolean("snd", false));
                    jsonSettings.put("psh", settings.getBoolean("psh", false));
                    jsonSettings.put("fmd", settings.getBoolean("fmd", false));
                    jsonSettings.put("fmr", settings.getBoolean("fmr", false));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Err sending settings");
                    Toast.makeText(ctx, "Error saving settings", Toast.LENGTH_SHORT).show();
                }
                mSocket.emit("syncSettings", jsonSettings.toString());

                mSocket.on("getHistoryLog", new Emitter.Listener()
                {
                    @Override
                    public void call(Object... args) {
                        Log.d(TAG, String.valueOf(args[0]));
                    }
                });

                //sync battery status
                batteryStatus = new Thread(new BatteryStatusThread(mSocket, ctx));
                batteryStatus.run();

                //send agenda
                syncAgenda = new Thread(new syncAddressBook(mSocket, ctx));
                syncAgenda.run();
            }
        });
        mSocket.on("loginFailed", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "RECEIVED LOGIN FAIL");
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent("LOGIN_FAIL"));
            }
        });
        mSocket.connect();

        return START_STICKY;
    }

    public void destroy()
    {
        String myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        mSocket.emit("removeDevice", myAndroidDeviceId);

        checkAndStopRunningThreads();

        //reset settings
        SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        SharedPreferences.Editor mPrefsEditor = settings.edit();
        mPrefsEditor.putBoolean("fwd", false);
        mPrefsEditor.putBoolean("snd", false);
        mPrefsEditor.putBoolean("psh", false);
        mPrefsEditor.putBoolean("fmd", false);
        mPrefsEditor.putBoolean("fmr", false);
        mPrefsEditor.commit();

        mSocket.disconnect();
        stopForeground(true);
        Log.d(TAG, "SELF STOPPED");
        stopSelf();
    }

    public void makeLogin(String user, String pass)
    {
        Log.d(TAG, "LOGIN USER: " + user + " PASS: " + pass);
        mSocket.emit("login",  user + "|" + pass  + "|" + myAndroidDeviceId + "|" + deviceName);
    }

    private void reloadThreads()
    {
        SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        if(settings.getBoolean("fwd", false) && !fwd_on)
        {
            //init SMS related threads
            Log.d(TAG, "fwd started");
            smsReceiver = new IncomingSms(mSocket);
//            forwardSms = new Thread(new ForwardSmsThread(ctx, mSocket, batteryStatus, smsReceiver));
            batteryStatus.run();
            fwd_on = true;
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");

            this.registerReceiver(smsReceiver, mIntentFilter);
//            forwardSms.run();
        }
        else if(!settings.getBoolean("fwd", false) && fwd_on)
        {
            Log.d(TAG, "fwd stopped");
            batteryStatus.run();
            fwd_on = false;
//            forwardSms.interrupt();
            this.unregisterReceiver(smsReceiver);
        }
        if(settings.getBoolean("snd", false) && !snd_on)
        {
            Log.d(TAG, "snd started");
            snd_on = true;
            sendSms = new Thread(new SendSmsThread(mSocket, ctx, batteryStatus));
            sendSms.run();
        }
        else if(!settings.getBoolean("snd", false) && snd_on)
        {
            Log.d(TAG, "snd stopped");
            snd_on = false;
            sendSms.interrupt();
        }
        if(settings.getBoolean("psh", false) && !psh_on)
        {
            Log.d(TAG, "psh started");
            //init push notifications
            pushReceiver = new NotificationReceiver(mSocket, batteryStatus);
            Intent pushListener = new Intent(this, NotificationListener.class);
            startService(pushListener);
            psh_on = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("notification");
            LocalBroadcastManager.getInstance(this).registerReceiver(pushReceiver, filter);
        }
        else if(!settings.getBoolean("psh", false) && psh_on)
        {
            Log.d(TAG, "psh stopped");
            psh_on = false;
            Intent pushListener = new Intent(this, NotificationListener.class);
            stopService(pushListener);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pushReceiver);
        }
        if(settings.getBoolean("fmd", false) && !fmd_on)
        {
            fmd_on = true;
            mSocket.on("fetchLoc", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (fmd_on) {
                        Log.d(TAG, "AM PRIMIT PT LOCATION DEVICE-UL" + args[0]);
                        findMyPhone = new Thread(new LocationThread(mSocket, ctx, batteryStatus));
                        findMyPhone.run();
                    } else {
                        Log.d(TAG, "NU MEGE LOCATIONU");
                    }
                }
            });
        }
        else if(!settings.getBoolean("fmd", false) && fmd_on)
        {
            Log.d(TAG, "fmd stopped");
            fmd_on = false;
            if(findMyPhone != null)
            {
                findMyPhone.interrupt();
            }
        }
        if(settings.getBoolean("fmr", false) && !fmr_on)
        {
            Log.d(TAG, "fmr started");
            fmr_on = true;
            IntentFilter mIntentFilter = new IntentFilter();
            mIntentFilter.addAction("android.intent.action.PHONE_STATE");
            batteryStatus.run();
            callReceiver = new PhonecallReceiver(mSocket);
            this.registerReceiver(callReceiver, mIntentFilter);
        }
        else if(!settings.getBoolean("fmr", false) && fmr_on)
        {
            Log.d(TAG, "fmr stopped");
            fmr_on = false;
//            ringMyPhone.interrupt();
            this.unregisterReceiver(callReceiver);
        }
    }

    public void optionsChanged()
    {
        batteryStatus.run();
        //here we fiddle with the threads after the dispatcher is notified of the changes
        //call thread_name.interrupt(); whenever needed

        reloadThreads();

        SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        JSONObject jsonSettings = new JSONObject();
        try {
            jsonSettings.put("fwd", settings.getBoolean("fwd", false));
            jsonSettings.put("snd", settings.getBoolean("snd", false));
            jsonSettings.put("psh", settings.getBoolean("psh", false));
            jsonSettings.put("fmd", settings.getBoolean("fmd", false));
            jsonSettings.put("fmr", settings.getBoolean("fmr", false));
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Err sending settings");
            Toast.makeText(ctx, "Error saving settings", Toast.LENGTH_SHORT).show();
        }
        mSocket.emit("updateSettings", jsonSettings.toString());
    }

    private void checkAndStopRunningThreads()
    {
        if(fwd_on)
        {
            forwardSms.interrupt();
        }
        if(snd_on)
        {
            sendSms.interrupt();
        }
        if(psh_on)
        {
            Intent pushListener = new Intent(this, NotificationListener.class);
            stopService(pushListener);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(pushReceiver);
        }
        if(fmd_on)
        {
            findMyPhone.interrupt();
        }
        if(fmr_on)
        {
//            ringMyPhone.interrupt();
            ///AICI MODIFFF
            this.unregisterReceiver(callReceiver);
        }
    }
}
