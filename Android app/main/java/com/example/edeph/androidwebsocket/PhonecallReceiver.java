package com.example.edeph.androidwebsocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Date;

public class PhonecallReceiver extends BroadcastReceiver {

    private Socket mSocket;
    private String TAG = "PHONECALL_RECEIVER";
    private String number;
    public PhonecallReceiver() {}
    public boolean status;

    PhonecallReceiver(Socket sock)
    {
        this.mSocket = sock;
        status = true;

        Log.d(TAG, "socket bound to bcast");
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            // This code will execute when the phone has an incoming call
            // get the phone number
            number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Log.d(TAG, "status is " + status);
            Log.d(TAG, "getting called: " + number);
            if(status)
            {
                status = false;
                Log.d(TAG, "I PRINT THIS ONCE PER CALL");
                mSocket.emit("phoneCallStart", number);

                mSocket.on("disconnectCall", new Emitter.Listener()
                {

                    @Override
                    public void call(Object... args) {
                        Log.d(TAG, "disconnect number: " + args[0]);
                        disconnectCall();
                    }
                });
            }

        } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                TelephonyManager.EXTRA_STATE_IDLE))
        {
            if(!status)
            {
                Log.d(TAG, "I AM IDLE NOW");
                status = true;
                if(mSocket != null)
                {
                    mSocket.emit("phoneCallStop", number);
                }
            }
        }
    }

    public void disconnectCall(){
        try {

            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";
            Class<?> telephonyClass;
            Class<?> telephonyStubClass;
            Class<?> serviceManagerClass;
            Class<?> serviceManagerNativeClass;
            Method telephonyEndCall;
            Object telephonyObject;
            Object serviceManagerObject;
            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);
            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);
            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod("asInterface", IBinder.class);
            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);
            telephonyObject = serviceMethod.invoke(null, retbinder);
            telephonyEndCall = telephonyClass.getMethod("endCall");
            telephonyEndCall.invoke(telephonyObject);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    "FATAL ERROR: could not connect to telephony subsystem");
            Log.e(TAG, "Exception object: " + e);
        }
    }

}