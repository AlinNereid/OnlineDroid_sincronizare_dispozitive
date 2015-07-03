package com.example.edeph.androidwebsocket;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

public class MainScreenActivity extends Activity
{
    private String TAG = "MAIN_ACTIVITY";
    private ThreadDispatcher mDispatcher;

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ThreadDispatcher.LocalBinder binder = (ThreadDispatcher.LocalBinder) service;
            mDispatcher = binder.getService();
            Log.d(TAG, "Am bindat dispatcherul");
            mDispatcher.optionsChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Am un-bindat dispatcherul");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        //Binding to dispatcher
        Intent mIntent = new Intent(getApplicationContext(), ThreadDispatcher.class);
        bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void logOut(View v)
    {

        SharedPreferences credentials = getSharedPreferences("CREDENTIALS", MODE_PRIVATE);
        SharedPreferences.Editor credentialsEditor = credentials.edit();
        credentialsEditor.putString("user", "");
        credentialsEditor.putString("pass", "");
        credentialsEditor.commit();

        mDispatcher.destroy();

        Intent login = new Intent(this, MainActivity.class);
        login.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(login);
        finish();
    }

    public void settings(View v)
    {
        //load previous saved data to show in settings
        Intent settings = new Intent(this, SettingsActivity.class);
        startActivity(settings);
    }

    public void onStop()
    {
        super.onStop();
        unbindService(mServiceConnection);
    }
}
