package com.example.SmallTalk;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Nitin
 * Date: 11/1/13
 * Time: 5:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Chat extends Activity {
    private EditText messageText;
    private ListView messageHistoryView;
    private Button sendMessageButton;
    private ArrayList<String> messageHistory;
    private ArrayAdapter<String> messageHistoryAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        messageHistoryView = (ListView) findViewById(R.id.messageHistoryView);
        messageText = (EditText) findViewById(R.id.message);
        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        messageHistory = new ArrayList<String>();

        messageHistoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , messageHistory);
        messageHistoryView.setAdapter(messageHistoryAdapter);

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageHistory.add(messageText.getText().toString());
                messageHistoryAdapter.notifyDataSetChanged();
                messageText.setText("");
                messageHistoryView.setSelection(messageHistoryAdapter.getCount() - 1);
                //TODO: SEND TO SERVER (with id?)
            }
        });
    }
}