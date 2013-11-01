package com.example.SmallTalk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveLocation extends Activity {
    WifiManager wifi;
    int size = 0;
    List<ScanResult> results;
    ArrayList<HashMap<String, Integer>> signals = new ArrayList<HashMap<String, Integer>>();
    TextView t;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        t = (TextView) findViewById(R.id.notif);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            t.setText("Enabling Wifi...");
            wifi.setWifiEnabled(true);
        }

        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                t.setText("Found location...");
                results = wifi.getScanResults();
                size = results.size();

                try
                {
                    size = size - 1;
                    while (size >= 0)
                    {
                        HashMap<String, Integer> item = new HashMap<String, Integer>();
                        item.put(results.get(size).BSSID, results.get(size).level);
                        System.err.println("SSID: " + item.get(results.get(size).BSSID));

                        signals.add(item);
                        size--;
                    }
                }
                catch (Exception e)
                {
                    System.err.println("Shit.");
                }

                t.setText("Joining room...");

                JSONArray json_signals=new JSONArray();
                for (Map<String, Integer> signal : signals) {
                    JSONObject json_obj=new JSONObject();
                    for (Map.Entry<String, Integer> entry : signal.entrySet()) {
                        String ssid = entry.getKey();
                        Integer strength = entry.getValue();
                        try {
                            json_obj.put(ssid,strength);
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    json_signals.put(json_obj);
                }
                new SendWifiDataTask().execute(json_signals);
                t.setText(json_signals.toString());

            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        signals.clear();
        wifi.startScan();

        t.setText("Scanning...");
    }

    class SendWifiDataTask extends AsyncTask<JSONArray, Void, String> {

        protected String doInBackground(JSONArray... json_signals) {
            String resp = null;
            try {
                HttpPost httpPost = new HttpPost("http://matphillips.com/st/send.php");
                StringEntity entity = new StringEntity(json_signals.toString(), HTTP.UTF_8);
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(httpPost);
                resp = response.toString();
            } catch (Exception e) {
                Log.e("HTTP", "Error in http connection " + e.toString());
            }

            // 11. return result
            return resp;
        }

        protected void onPostExecute(String result) {
            Intent intent = new Intent(RetrieveLocation.this, Chat.class);
            startActivity(intent);
            finish();
        }
    }

}
