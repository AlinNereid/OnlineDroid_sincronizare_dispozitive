package com.example.edeph.androidwebsocket;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import com.github.nkzawa.socketio.client.Socket;


public class ForwardSmsThread implements Runnable
{
    private String TAG = "FORWARD_THREAD";
    private Context ctx;
    private IncomingSms receiver;
    private IntentFilter mIntentFilter;
    private Socket mSocket;

    public ForwardSmsThread(Context context, Socket sock, Thread battery, IncomingSms smsReceiver)
    {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        this.ctx = context;
        mSocket = sock;
        battery.run();
        this.receiver = smsReceiver;
    }

    @Override
    public void run()
    {
        Log.d(TAG, "Start run");
//        this.receiver = new IncomingSms(mSocket);
        ctx.registerReceiver(receiver, mIntentFilter);
    }

    public void unregisterReceiver()
    {
        Log.d(TAG, "Stop run");
        ctx.unregisterReceiver(receiver);
    }
}
