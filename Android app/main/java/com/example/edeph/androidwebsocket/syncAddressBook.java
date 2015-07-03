package com.example.edeph.androidwebsocket;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class syncAddressBook implements Runnable
{
    private String TAG = "SYNC_AGENDA";
    private Socket mSocket;
    private Context context;

    public syncAddressBook(Socket sock, Context ctx)
    {
        this.context = ctx;
        this.mSocket = sock;
    }

    @Override
    public void run()
    {
        JSONObject agenda = new JSONObject();
        Map<String, String> contactMap = getAddressBook(context);
        Iterator it = contactMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            try {
                String number = pair.getKey().toString().replace(" ", "");
                number = number.replace("-","");
                agenda.put(number, pair.getValue().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        mSocket.emit("agenda", agenda.toString());
    }

    private Map<String, String> getAddressBook(Context context)
    {
        Map<String, String> result = new HashMap<String, String>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while(cursor.moveToNext())
        {
            int phone_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int name_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            String phone = cursor.getString(phone_idx);
            String name = cursor.getString(name_idx);
            result.put(phone, name);
        }
        return result;
    }
}