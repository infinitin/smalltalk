package com.example.SmallTalk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveLocation extends Activity {
    WifiManager wifi;
    ProgressDialog progress;
    int size = 0;
    List<ScanResult> results;
    ArrayList<HashMap<String, Integer>> signals = new ArrayList<HashMap<String, Integer>>();
    WifiReceiver wifiReceiver = new WifiReceiver();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        progress = new ProgressDialog(this);
        progress.setTitle("Scanning");
        progress.show();
        progress.getWindow().setGravity(Gravity.BOTTOM);
        // To dismiss the dialog

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            progress.setMessage("Enabling Wifi...");
            wifi.setWifiEnabled(true);
        }

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        signals.clear();
        wifi.startScan();

        progress.setMessage("Scanning");
    }

    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i)
        {
        	progress.setMessage("Found location...");
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

            progress.setMessage("Joining room...");

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

            Intent intent = new Intent(RetrieveLocation.this, Chat.class);
            intent.putExtra("JSONSignals", json_signals.toString());
            unregisterReceiver(wifiReceiver);
            startActivity(intent);
            finish();

        }
    }



}
