package com.example.edeph.androidwebsocket;

import android.content.Context;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SendSmsThread implements Runnable
{
    private String TAG = "FORWARD_THREAD";
    private int MAX_SMS_MESSAGE_LENGTH = 160;
    private String SmsMessage;
    private String phoneNumber;
    private Socket mSocket;
    private Context context;

    public SendSmsThread(Socket sock, Context ctx, Thread battery)
    {
        this.context = ctx;
        this.mSocket = sock;
        battery.run();
    }

    @Override
    public void run()
    {
        Log.d(TAG, "IMMA HERE");
        final SmsManager manager = SmsManager.getDefault();
        mSocket.on("smsSend",new Emitter.Listener()
        {
            @Override
            public void call(Object... args) {
                Log.d(TAG, String.valueOf(args[0]));
                JSONObject data = (JSONObject) args[0];
                try {
                    SmsMessage = data.getString("message");
                    phoneNumber = data.getString("phone");
                    Log.d(TAG, "Phone: " + phoneNumber + " message: " + SmsMessage);
                    if(SmsMessage.length() > MAX_SMS_MESSAGE_LENGTH)
                    {
                        Log.d(TAG, "A DEPASIT MAX LENGTH!");
                        ArrayList<String> messageList = manager.divideMessage(SmsMessage);
                        manager.sendMultipartTextMessage(phoneNumber,null,messageList,null,null);
                    }
                    else
                    {
                        Log.d(TAG,"Single SMS!");
                        try
                        {
                            manager.sendTextMessage(phoneNumber,null,SmsMessage,null,null);
                        }
                        catch(Exception e)
                        {
                            Log.d(TAG, e.toString());
                        }
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "JSON_OBJ crashed" + e);
                    return;
                }

            }
        });
    }
}
