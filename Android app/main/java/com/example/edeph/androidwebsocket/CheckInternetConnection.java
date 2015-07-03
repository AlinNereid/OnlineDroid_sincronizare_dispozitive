package com.example.edeph.androidwebsocket;

import android.os.AsyncTask;
import android.util.Log;

import java.net.InetAddress;

public class CheckInternetConnection extends AsyncTask<Void, Void, Boolean>
{
    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            InetAddress ipAddr = InetAddress.getByName("www.google.com");
            if (ipAddr.equals("")) {
                return  false;
            } else {
                return true;
            }
        } catch (Exception e) {
            Log.d("ASYNC_TASK", "Am crapat " + e);
            return false;
        }
    }
}
