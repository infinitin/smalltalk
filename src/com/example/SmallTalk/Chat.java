package com.example.SmallTalk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;


/**
 * Created with IntelliJ IDEA.
 * User: Nitin
 * Date: 11/1/13
 * Time: 5:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Chat extends Activity {
    private Button sendMessageButton;
    private EditText messageText;
    private TextView num_viewers;

    private String host = "ejabberd.ro.lt";
    private int port = 5222;
    private boolean SASLAuth = true;
    protected Connection conn;

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

        new XMPPConnect(this).execute();
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
                    muc.join("sm", s4, history, SmackConfiguration.getPacketReplyTimeout());
                    //TODO: Fill screen with the messages found from history
                } catch (XMPPException e) {
                    System.err.println("FAILED TO JOIN ROOM");
                }

                sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
                messageText = (EditText) findViewById(R.id.message);

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

                RoomInfo roomInfo = null;
                try {
                    roomInfo = MultiUserChat.getRoomInfo(muc_conn, muc_room);
                } catch (XMPPException e) {
                    System.err.println("FAILED TO GET ROOM INFO");
                }
                num_viewers = (TextView) findViewById(R.id.num_viewers);
                num_viewers.setText(roomInfo != null ? Integer.toString(roomInfo.getOccupantsCount()) : "0");

                muc_conn.getChatManager().addChatListener(new MUCManagerListener());

            }
        });
    }

    private class MUCManagerListener implements ChatManagerListener {
        @Override
        public void chatCreated(org.jivesoftware.smack.Chat chat, boolean b) {
            chat.addMessageListener(new MessageListener(){

                @Override
                public void processMessage(org.jivesoftware.smack.Chat chat, Message message) {
                    System.out.println("NEW MESSAGE: " + message.getBody());
                }
            });
        }
    }


    private class XMPPConnect extends AsyncTask<Void, Integer, Connection> {
        private ProgressDialog progress;

        XMPPConnect(Activity activity) {
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
                xmppConn.login("smalltalk", "jabber");
                //conn.loginAnonymously();
            } catch (XMPPException e) {
                e.printStackTrace();
            }



            return xmppConn;
        }

        protected void onPostExecute(Connection result) {
            if (progress.isShowing()) {
                progress.dismiss();
            }

            conn = result;
            MUCConnect();
        }
    }
}

