package com.example.SmallTalk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.provider.*;
import org.jivesoftware.smackx.search.UserSearch;

import java.util.*;

public class Chat extends Activity {
    WifiManager wifi;
    ProgressDialog progress;
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

    AlertDialog connectionErrorDialog = null;

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
        if (!wifi.isWifiEnabled())
        {
            progress.setMessage("Enabling Wifi...");
            wifi.setWifiEnabled(true);
        }

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

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
                    configure(ProviderManager.getInstance());
                    progress.setMessage("Connecting to server...");
                    new XMPPConnect().execute();
                } else {
                    MUCConnect(3);
                }
            }
        }

        private void configure(ProviderManager pm) {

            //  Private Data Storage
            pm.addIQProvider("query","jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());


            //  Time
            try {
                pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
            } catch (ClassNotFoundException e) {
                Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
            }

            //  Roster Exchange
            pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());

            //  Message Events
            pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());

            //  Chat State
            pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

            pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

            pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

            pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

            pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

            //  XHTML
            pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

            //  Group Chat Invitations
            pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());

            //  Service Discovery # Items
            pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

            //  Service Discovery # Info
            pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

            //  Data Forms
            pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());

            //  MUC User
            pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());

            //  MUC Admin
            pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());


            //  MUC Owner
            pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());

            //  Delayed Delivery
            pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());

            //  Version
            try {
                pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
            } catch (ClassNotFoundException e) {
                //  Not sure what's happening here.
            }

            //  VCard
            pm.addIQProvider("vCard","vcard-temp", new VCardProvider());

            //  Offline Message Requests
            pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

            //  Offline Message Indicator
            pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

            //  Last Activity
            pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());

            //  User Search
            pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());

            //  SharedGroupsInfo
            pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

            //  JEP-33: Extended Stanza Addressing
            pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());

            //   FileTransfer
            pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());

            pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());

            //  Privacy
            pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());

            pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
            pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
            pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
            pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
            pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
            pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
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
            } catch (Exception e) {
                System.err.println("Failed to connect to smalltalk server");
                return null;
            }

            try {
                xmppConn.loginAnonymously();
            } catch (XMPPException e) {
                System.err.println("Failed to login to smalltalk server anonymously");
                return null;
            }

            user_id = xmppConn.getUser().substring(0, xmppConn.getUser().indexOf('@'));

            return xmppConn;
        }

        protected void onPostExecute(Connection result) {
            if (result == null){
                showConnectionErrorDialog();
            } else {
                conn = result;
                progress.setMessage("Joining epic conversation");
                MUCConnect(3);
            }
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

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(muc_room.substring(0, muc_room.indexOf("@")));
        
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
                } catch (Exception e) {
                    System.err.println("FAILED TO SEND MESSAGE");
                    showConnectionErrorDialog();
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
            final String occupants = roomInfo != null ? Integer.toString(roomInfo.getOccupantsCount()) : "1";
            runOnUiThread(new Runnable(){
                public void run(){
                    num_viewers = (TextView) findViewById(R.id.num_viewers);
                    num_viewers.setText(occupants);
                }
            });
        }
    }

    class MessageListener implements PacketListener {
        @Override
        public void processPacket(Packet packet) {
            final Message message = (Message) packet;
        	runOnUiThread(new Runnable(){
                public void run(){
                    messageHistory.add(message.getBody());
                    messageHistoryAdapter.notifyDataSetChanged();
                    messageHistoryView.invalidateViews();
                    if (messageHistoryView.getLastVisiblePosition() >= messageHistoryAdapter.getCount() - 3 || messageHistoryView.getFirstVisiblePosition() <= 1) {
                        messageHistoryView.setSelection(messageHistoryAdapter.getCount() - 1);
                    }
                }
            });
        }
    }

    BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            if (noConnectivity) {
                showConnectionErrorDialog();
            }
        }
    };

    protected void showConnectionErrorDialog() {
        if(connectionErrorDialog == null) {
            connectionErrorDialog = new AlertDialog.Builder(this)
                    .setTitle("No network connection")
                    .setMessage("Please make sure you are connected to the Internet and try again")
                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            progress.setMessage("Connecting to server...");
                            progress.show();
                            new XMPPConnect().execute();
                            hideConnectionErrorDialog();
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }
    }

    protected void hideConnectionErrorDialog() {
        if(connectionErrorDialog != null){
            connectionErrorDialog.hide();
            connectionErrorDialog = null;
        }
    }

    @Override
    public void onDestroy(){
        muc.leave();
        conn.disconnect();
        super.onDestroy();
    }
}
