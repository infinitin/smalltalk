package com.example.SmallTalk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created with IntelliJ IDEA.
 * User: Nitin
 * Date: 11/1/13
 * Time: 5:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Chat extends Activity {
    private Activity activity;
    protected String signals;

    private Button sendMessageButton;
    private EditText messageText;
    private TextView num_viewers;
    private ListView messageHistoryView;
    private ArrayList<String> messageHistory = new ArrayList<String>();
    private ArrayAdapter<String> messageHistoryAdapter;

    private String host = "ejabberd.ro.lt";
    private int port = 5222;
    private boolean SASLAuth = true;
    protected Connection conn;
    protected String user_id;

    private MultiUserChat muc;
    private Connection muc_conn;
    private String muc_room;
    //We can allow the user to set all of these in their own settings if we want.
    private int historyTime = 60;
    private int historyLength = 10;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        SmackAndroid.init(this);

        Intent intent = getIntent();
        signals = intent.getStringExtra("JSONSignals");
        activity = this;

        messageHistoryView = (ListView) findViewById(R.id.messageHistoryView);
        messageHistoryAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, messageHistory);
        messageHistoryView.setAdapter(messageHistoryAdapter);

        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        messageText = (EditText) findViewById(R.id.message);
        num_viewers = (TextView) findViewById(R.id.num_viewers);

        new XMPPConnect().execute();
    }

    protected void MUCConnect() {
        MultiUserChat.addInvitationListener(conn, new InvitationListener() {
            @Override
            public void invitationReceived(Connection connection, String s, String s2, String s3, String s4, Message message) {
                //s is the room, s2 is the inviter, s3 is the reason, s4 is the password
                muc_conn = connection;
                muc_room = s;
                muc = new MultiUserChat(muc_conn, muc_room);
                DiscussionHistory history = new DiscussionHistory();
                history.setMaxStanzas(historyLength);
                history.setSeconds(historyTime);
                try {
                    muc.join(user_id, s4, history, SmackConfiguration.getPacketReplyTimeout());
                    //TODO: Fill screen with the messages found from history
                } catch (XMPPException e) {
                    System.err.println("FAILED TO JOIN ROOM");
                }

                sendMessageButton.setOnClickListener(new View.OnClickListener() {
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
                });

                muc.addParticipantListener(new PacketListener() {
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
                });

                muc.addMessageListener(new PacketListener() {
                    @Override
                    public void processPacket(Packet packet) {
                        final Message message = (Message) packet;
                        messageHistory.add(message.getBody());
                    }
                });

            }
        });
    }

    private class MUCManagerListener implements ChatManagerListener {
        @Override
        public void chatCreated(org.jivesoftware.smack.Chat chat, boolean b) {
            chat.addMessageListener(new MessageListener(){

                @Override
                public void processMessage(org.jivesoftware.smack.Chat chat, Message message) {
                    messageHistoryView = (ListView) findViewById(R.id.messageHistoryView);
                    messageHistoryAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, messageHistory);
                    messageHistoryView.setAdapter(messageHistoryAdapter);
                    messageHistory.add(message.getBody());
                }
            });
        }
    }


    private class XMPPConnect extends AsyncTask<Void, Integer, Connection> {
        private ProgressDialog progress;

        XMPPConnect() {
            progress = new ProgressDialog(activity);
        }

        protected void onPreExecute() {
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.setMessage("Joining epic conversation");
            progress.show();
        }

        protected Connection doInBackground(Void... nothing) {

            ConnectionConfiguration config = new ConnectionConfiguration(host, port);
            config.setSASLAuthenticationEnabled(SASLAuth);
            Connection xmppConn = new XMPPConnection(config);
            try {
                xmppConn.connect();
            } catch (XMPPException e) {
                e.printStackTrace();
            }
            try {
                //xmppConn.login("smalltalk", "jabber");
                xmppConn.loginAnonymously();
            } catch (XMPPException e) {
                e.printStackTrace();
            }

            user_id = xmppConn.getUser().substring(0, xmppConn.getUser().indexOf('@'));

            return xmppConn;
        }

        protected void onPostExecute(Connection result) {
            if (progress.isShowing()) {
                progress.dismiss();
            }

            conn = result;
            MUCConnect();
            new SendWifiDataTask().execute();
        }
    }

    class SendWifiDataTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress;

        SendWifiDataTask() {
            progress = new ProgressDialog(activity);
        }

        protected void onPreExecute() {
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.setMessage("Getting json signals");
            progress.show();
        }

        protected Void doInBackground(Void... x) {
            try {
                JSONArray signals_only = new JSONArray(signals);
                JSONObject json_signals = new JSONObject();
                json_signals.put(user_id, signals_only);

                String url = "http://matphillips.com/st/send.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");

                String urlParameters = "data=" + json_signals.toString();

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            } catch (Exception e) {
                Log.e("HTTP", "Error in http connection " + e.toString());
            }

            return null;
        }

        protected void onPostExecute(Void x) {
            progress.dismiss();
        }
    }
}

