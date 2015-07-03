package com.example.edeph.androidwebsocket;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class LocationThread implements Runnable, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private GoogleApiClient mGoogleApiClient;
    private Socket mSocket;
    private Context ctx;
    private String TAG = "LOCATION_THREAD";
    private Thread batteryThread;

    LocationThread(Socket socket, Context context, Thread battery)
    {
        this.batteryThread = battery;
        this.mSocket = socket;
        this.ctx = context;
        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "connectedToPlayServices");
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d(TAG, String.valueOf(mLastLocation.getLatitude()));
            Log.d(TAG, String.valueOf(mLastLocation.getLongitude()));
            batteryThread.run();
            mSocket.emit("geoloc",String.valueOf(mLastLocation.getLatitude()) + "|" +
                    String.valueOf(mLastLocation.getLongitude()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Location services suspended with code: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        Toast.makeText(ctx, "Location failed!", Toast.LENGTH_SHORT);
        //ar trebui emit cu fail aici failGeoloc
    }

    @Override
    public void run()
    {
        mGoogleApiClient.connect();
        Log.d(TAG, "I'm running :D");
    }
}
