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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
    WifiReceiver wifiReceiver = new WifiReceiver();

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

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        signals.clear();
        wifi.startScan();

        t.setText("Scanning...");
    }

    class WifiReceiver extends BroadcastReceiver {
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
    }

    class SendWifiDataTask extends AsyncTask<JSONArray, Void, String> {

        protected String doInBackground(JSONArray... json_signals) {
            String resp = null;
            try {
                String url = "http://matphillips.com/st/send.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");

                String urlParameters = "data=" + json_signals[0].toString();

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.err.println("RESPONSE:" + response.toString());



            } catch (Exception e) {
                Log.e("HTTP", "Error in http connection " + e.toString());
            }

            // 11. return result
            return resp;
        }

        protected void onPostExecute(String result) {
            Intent intent = new Intent(RetrieveLocation.this, Chat.class);
            unregisterReceiver(wifiReceiver);
            startActivity(intent);
            finish();
        }
    }

}
