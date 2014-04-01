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
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import java.util.*;

public class Chat extends Activity {
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
    protected String user_id = "";

    private Button sendMessageButton;
    private EditText messageText;
    private TextView num_viewers;
    private ListView messageHistoryView;
    private ArrayList<String> messageHistory = new ArrayList<String>();
    private ArrayAdapter<String> messageHistoryAdapter;

    private MultiUserChat muc;
    private Connection muc_conn;
    private String muc_room = "";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        progress = new ProgressDialog(this);
        progress.setTitle("Scanning");
        progress.show();
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
            results = wifi.getScanResults();

            String room = getStrongestResult() + "@conference.ejabberd.ro.lt";
            if(!muc_room.equals(room)){
                muc_room = room;

                if(user_id.isEmpty()) {
                    progress.setMessage("Connecting to server...");
                    new XMPPConnect().execute();
                } else {
                    MUCConnect(3);
                }
            }
        }

        private String getStrongestResult() {
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {
                    return (lhs.level > rhs.level ? -1 : (lhs.level == rhs.level ? 0 : 1));
                }
            });

            return results.get(0).BSSID.replaceAll(":", "");
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
            MUCConnect(3);
        }
    }

    protected void MUCConnect(int tries) {
        muc_conn = conn;
        muc = new MultiUserChat(muc_conn, muc_room);

        try {
            muc.join(user_id);
        } catch (XMPPException e) {
            try {
                muc.create(user_id);
                muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
            } catch (XMPPException e1) {
                if(tries > 0) {
                    MUCConnect(tries - 1);
                } else {
                    System.err.println("Failed to join/create room");
                }
            }
        }

        sendMessageButton.setOnClickListener(new SendMessageClick());
        muc.addParticipantListener(new ParticipantListener());
        muc.addMessageListener(new MessageListener());

        // This fails when it shouldn't.
        // We should only need to check for an XMPPException but I can't figure out how to stop it from failing.
        // So I just catch it for now.
        RoomInfo roomInfo = null;
        try {
            roomInfo = MultiUserChat.getRoomInfo(muc_conn, muc_room);
        } catch (Exception e) {
            System.err.println("FAILED TO GET ROOM INFO");
        }
        num_viewers = (TextView) findViewById(R.id.num_viewers);
        num_viewers.setText(roomInfo != null ? Integer.toString(roomInfo.getOccupantsCount()) : "1");

        progress.dismiss();
    }

    class SendMessageClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String message = messageText.getText().toString();
            if(!message.isEmpty()) {
                messageText.setText("");
                try {
                    muc.sendMessage(message);
                } catch (XMPPException e) {
                    System.err.println("FAILED TO SEND MESSAGE");
                }
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
            messageHistoryAdapter.notifyDataSetChanged();
            messageHistoryView.invalidateViews();
        }
    }
}
