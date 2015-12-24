package net.ldvsoft.warofviruses;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class WoVGcmListenerService extends GcmListenerService {

    private static final String TAG = "WoVGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i(TAG, "From: " + from);
        Log.i(TAG, "Message: " + data.toString());
        String action = (String) data.get(WoVProtocol.ACTION);
        Intent intent = new Intent();
        if (action.equals(WoVProtocol.ACTION_TURN)) {
            intent.putExtra(WoVPreferences.TURN_BUNDLE, data);
            intent.setAction(WoVPreferences.TURN_BROADCAST);
            sendBroadcast(intent);
        } else if (action.equals(WoVProtocol.GAME_LOADED)) {
            intent.putExtra(WoVPreferences.GAME_BUNDLE, data);
            intent.setAction(WoVPreferences.GAME_LOADED_FROM_SERVER_BROADCAST);
            sendBroadcast(intent);
        }
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */
    }
}