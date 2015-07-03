package com.example.edeph.androidwebsocket;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity
{
    private String TAG = "LOGIN_ACTIVITY";
    private ThreadDispatcher mDispatcher;
    private confirmLoginReceiver bCast;

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ThreadDispatcher.LocalBinder binder = (ThreadDispatcher.LocalBinder) service;
            mDispatcher = binder.getService();
            Log.d(TAG, "ThreadDispatcher is bound now!");

            //extrag credentialele introduse de user
            EditText user = (EditText) findViewById(R.id.username);
            EditText pass = (EditText) findViewById(R.id.password);

            //lansez verificarea cu web serverul
            mDispatcher.makeLogin(user.getText().toString(), pass.getText().toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            Log.d(TAG, "Service unbinded now!");
        }
    };

    public class confirmLoginReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals("LOGIN_SUCCESS"))
            {
                Toast.makeText(getApplicationContext(), "Login success!", Toast.LENGTH_SHORT);
                //save the credentials
                SharedPreferences credentials = getSharedPreferences("CREDENTIALS", MODE_PRIVATE);
                SharedPreferences.Editor credentialsEditor = credentials.edit();
                EditText currUser = (EditText) findViewById(R.id.username);
                EditText currPass = (EditText) findViewById(R.id.password);
                credentialsEditor.putString("user", currUser.getText().toString());
                credentialsEditor.putString("pass", currPass.getText().toString());
                credentialsEditor.commit();

                //lanseaza main screen activity
                Intent mainScreen = new Intent(getApplicationContext(), MainScreenActivity.class);
                mainScreen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainScreen);
                finish();
            }
            else if(intent.getAction().equals("LOGIN_FAIL"))
            {
                //golesc campul passowrd
                EditText edit = (EditText) findViewById(R.id.password);
                edit.setText("");
                Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
                //mDispatcher.destroy();
            }
            else if(intent.getAction().equals("SERVICE_CREATED"))
            {

            }
            else
            {
                Toast.makeText(getApplicationContext(), "UNKNOWN ERR", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences mPrefs = getSharedPreferences("CREDENTIALS", MODE_PRIVATE);
        Log.d(TAG, "CREDENTIALELE " + mPrefs.getString("user", ""));
        if("".equals(mPrefs.getString("user", "")))
        {
            //credentialele nu sunt setate
            setContentView(R.layout.login_signup);
            //reset settings
            SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
            SharedPreferences.Editor mPrefsEditor = settings.edit();
            mPrefsEditor.putBoolean("fwd", false);
            mPrefsEditor.putBoolean("snd", false);
            mPrefsEditor.putBoolean("psh", false);
            mPrefsEditor.putBoolean("fmd", false);
            mPrefsEditor.putBoolean("fmr", false);
            mPrefsEditor.commit();
        }
        else
        {
            //credentialele sunt salvate
            //serviciul deja ruleaza, dar activity probabil a fost omorat de sistem sau de user
            //afisez direct main screen

            Intent mainScreen = new Intent(getApplicationContext(), MainScreenActivity.class);
            mainScreen.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainScreen);
            finish();
        }
        Log.d(TAG, "ThreadDispatcher created, activity onCreate ending");
    }

    @Override
    public void onStart()
    {
        super.onStart();
        //register broadcastReceiver
        bCast = new confirmLoginReceiver();
        IntentFilter mIntentFilter;
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("LOGIN_SUCCESS");
        mIntentFilter.addAction("LOGIN_FAIL");
        mIntentFilter.addAction("SERVICE_CREATED");
        LocalBroadcastManager.getInstance(this).registerReceiver(bCast, mIntentFilter);
    }

    public void onStop()
    {
        super.onStop();
        //unregister broadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bCast);
    }

    //Button onClick event functions
    public void login(View v)
    {
        Log.d(TAG, "Login step started");
        if(!isInternetAvailable())
        {
            Toast.makeText(this, "Not connected to the internet!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            //extrag credentialele introduse de user
            EditText user = (EditText) findViewById(R.id.username);
            EditText pass = (EditText) findViewById(R.id.password);

            //verific sa fie nenule
            if (user.getText().length() > 0 && pass.getText().length() > 0)
            {
                //pornesc serviciul
                Intent mIntent = new Intent(getApplicationContext(), ThreadDispatcher.class);
                startService(mIntent);

//                //Bind to ThreadDispatcher
                Intent intent = new Intent(getApplicationContext(), ThreadDispatcher.class);
                Log.d(TAG, "Call to bind to ThreadDispatcher");
                bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            }
            else
            {
                Toast.makeText(this,"Complete the fields!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isInternetAvailable()
    {
        Boolean res = false;
        try {
            res  = new CheckInternetConnection().execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG, "CRASHED interrupt : " + e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.d(TAG, "CRASHED exec: " + e);
        }
        return res;
    }
}
