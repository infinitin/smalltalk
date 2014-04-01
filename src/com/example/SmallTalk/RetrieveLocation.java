package com.example.SmallTalk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RetrieveLocation extends Activity {
    WifiManager wifi;
    ProgressDialog progress;
    int size = 0;
    List<ScanResult> results;
    ArrayList<HashMap<String, Integer>> signals = new ArrayList<HashMap<String, Integer>>();
    WifiReceiver wifiReceiver = new WifiReceiver();

    private String host = "ejabberd.ro.lt";
    private int port = 5222;
    private boolean SASLAuth = true;
    protected Connection conn;
    protected String user_id;

    private Button sendMessageButton;
    private EditText messageText;
    private TextView num_viewers;
    private ListView messageHistoryView;
    private ArrayList<String> messageHistory = new ArrayList<String>();
    private ArrayAdapter<String> messageHistoryAdapter;

    private MultiUserChat muc;
    private Connection muc_conn;
    private String muc_room;

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

        messageHistoryView = (ListView) findViewById(R.id.messageHistoryView);
        messageHistoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messageHistory);
        messageHistoryView.setAdapter(messageHistoryAdapter);

        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        messageText = (EditText) findViewById(R.id.message);
        num_viewers = (TextView) findViewById(R.id.num_viewers);

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

            progress.setMessage("Connecting to server...");

            new XMPPConnect().execute();

        }
    }

    class XMPPConnect extends AsyncTask<Void, Integer, Connection> {

        protected Connection doInBackground(Void... nothing) {

            ConnectionConfiguration config = new ConnectionConfiguration(host, port);
            config.setSASLAuthenticationEnabled(SASLAuth);
            Connection xmppConn = new XMPPConnection(config);
            try {
                xmppConn.connect();
            } catch (XMPPException e) {
                System.err.println("Failed to connect to smalltalk server");
            }
            try {
                xmppConn.loginAnonymously();
            } catch (XMPPException e) {
                System.err.println("Failed to login to smalltalk server anonymously");
            }

            user_id = xmppConn.getUser().substring(0, xmppConn.getUser().indexOf('@'));

            return xmppConn;
        }

        protected void onPostExecute(Connection result) {
            conn = result;
            progress.setMessage("Joining epic conversation");
            MUCConnect();
        }
    }

    protected void MUCConnect() {
        muc_room = signals.get(0).keySet().toArray()[0].toString() + "@conference.ejabberd.ro.lt";
        muc_conn = conn;
        muc = new MultiUserChat(muc_conn, muc_room);

        try {
            muc.join(user_id);
        } catch (XMPPException e) {
            System.err.println("Failed to join room");
        }

        sendMessageButton.setOnClickListener(new SendMessageClick());
        muc.addParticipantListener(new ParticipantListener());
        muc.addMessageListener(new MessageListener());
    }

    class SendMessageClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String message = messageText.getText().toString();
            messageText.setText("");
            try {
                muc.sendMessage(message);
            } catch (XMPPException e) {
                System.err.println("FAILED TO SEND MESSAGE");
            }
        }
    }

    class ParticipantListener implements PacketListener {
        @Override
        public void processPacket(Packet packet) {
            RoomInfo roomInfo = null;
            try {
                roomInfo = MultiUserChat.getRoomInfo(muc_conn, muc_room);
            } catch (XMPPException e) {
                System.err.println("FAILED TO GET ROOM INFO");
            }
            num_viewers = (TextView) findViewById(R.id.num_viewers);
            num_viewers.setText(roomInfo != null ? Integer.toString(roomInfo.getOccupantsCount()) : "0");
        }
    }

    class MessageListener implements PacketListener {
        @Override
        public void processPacket(Packet packet) {
            final Message message = (Message) packet;
            messageHistory.add(message.getBody());
        }
    }
}
