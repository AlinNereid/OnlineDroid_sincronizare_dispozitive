package com.example.edeph.androidwebsocket;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

public class SettingsActivity extends Activity
{
    private String TAG = "SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
    }

    public void onStart()
    {
        super.onStart();
        //verifica cartela SIM pt a da enable/disable
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        CheckBox check;
        Log.d(TAG, String.valueOf(simState));
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                Log.d(TAG, "SIM ABSENT");
                check = (CheckBox) findViewById(R.id.forwardSmsCheckbox);
                check.setEnabled(false);
                check = (CheckBox) findViewById(R.id.sendsmsCheckbox);
                check.setEnabled(false);
                check = (CheckBox) findViewById(R.id.findMyDeviceRingCheckbox);
                check.setEnabled(false);
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                // do something
                Log.d(TAG, "SIM UNKNOWN");
                check = (CheckBox) findViewById(R.id.forwardSmsCheckbox);
                check.setEnabled(false);
                check = (CheckBox) findViewById(R.id.sendsmsCheckbox);
                check.setEnabled(false);
                check = (CheckBox) findViewById(R.id.findMyDeviceRingCheckbox);
                check.setEnabled(false);
                break;
        }

        SharedPreferences savedSettings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        CheckBox fwd_chk = (CheckBox) findViewById(R.id.forwardSmsCheckbox);
        fwd_chk.setChecked(savedSettings.getBoolean("fwd", false));
        CheckBox snd_chk = (CheckBox) findViewById(R.id.sendsmsCheckbox);
        snd_chk.setChecked(savedSettings.getBoolean("snd", false));
        CheckBox psh_chk = (CheckBox) findViewById(R.id.pushNotificationsCheckbox);
        psh_chk.setChecked(savedSettings.getBoolean("psh", false));
        CheckBox fmd_chk = (CheckBox) findViewById(R.id.findMyDeviceCheckbox);
        fmd_chk.setChecked(savedSettings.getBoolean("fmd", false));
        CheckBox fmr_chk = (CheckBox) findViewById(R.id.findMyDeviceRingCheckbox);
        fmr_chk.setChecked(savedSettings.getBoolean("fmr", false));
    }

    public void saveSettings(View v)
    {
        Boolean fwd = false;
        Boolean snd = false;
        Boolean psh = false;
        Boolean fmd = false;
        Boolean fmr = false;
        CheckBox mCheckBox = (CheckBox) findViewById(R.id.forwardSmsCheckbox);
        if(mCheckBox.isChecked())
        {
            fwd = true;
        }
        mCheckBox = (CheckBox) findViewById(R.id.sendsmsCheckbox);
        if(mCheckBox.isChecked())
        {
            snd = true;
        }
        mCheckBox = (CheckBox) findViewById(R.id.pushNotificationsCheckbox);
        if(mCheckBox.isChecked())
        {
            psh = true;
        }
        mCheckBox = (CheckBox) findViewById(R.id.findMyDeviceCheckbox);
        if(mCheckBox.isChecked())
        {
            fmd = true;
        }
        mCheckBox = (CheckBox) findViewById(R.id.findMyDeviceRingCheckbox);
        if(mCheckBox.isChecked())
        {
            fmr = true;
        }

        //Save the settings locally
        SharedPreferences settings = getSharedPreferences("SETTINGS", MODE_PRIVATE);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putBoolean("fwd", fwd);
        settingsEditor.putBoolean("snd", snd);
        settingsEditor.putBoolean("psh", psh);
        settingsEditor.putBoolean("fmd", fmd);
        settingsEditor.putBoolean("fmr", fmr);
        settingsEditor.commit();

        Intent mIntent = new Intent(this, MainScreenActivity.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mIntent);
        finish();
    }
}
