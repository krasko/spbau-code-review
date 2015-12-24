package net.ldvsoft.warofviruses;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by ldvsoft on 21.10.15.
 */
public class WoVRegistrationIntentService extends IntentService {
    protected static final String TAG = WoVRegistrationIntentService.class.getName();

    public WoVRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Token: " + token);

            boolean sent = sendTokenToServer(token);
            prefs.edit().putBoolean(WoVPreferences.GCM_SENT_TOKEN_TO_SERVER, sent).apply();
        } catch (IOException e) {
            e.printStackTrace();
            prefs.edit().putBoolean(WoVPreferences.GCM_SENT_TOKEN_TO_SERVER, false).apply();
        }

        Intent registrationComplete = new Intent(WoVPreferences.GCM_REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private boolean sendTokenToServer(String token) {
        try {
            URL url = new URL(getString(R.string.server_cgi));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject toSend = new JSONObject()
                    .put("action", "test")
                    .put("token", token);

            OutputStream os = connection.getOutputStream();
            os.write(toSend.toString().getBytes(Charset.forName("UTF-8")));
            os.close();

            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.wtf(TAG, "Server code: " + connection.getResponseCode() + ", not 200");
                return false;
            }
            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder builder = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                builder.append(line);
                builder.append('\n');
            }
            JSONObject answer = new JSONObject(builder.toString());

            if (answer.optString("result", "").equals("success")) {
                Log.i(TAG, "Token sent!");
                return true;
            } else {
                Log.wtf(TAG, "Token not accepted!");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.wtf(TAG, "Malformed URL?!");
        } catch (IOException e) {
            e.printStackTrace();
            Log.wtf(TAG, "Failed to send token!");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.wtf(TAG, "WTF is with JSON?!");
        }
        return false;
    }
}
