package com.example.SmallTalk;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ArrayList<String> messageHistory = new ArrayList<String>();
    private ArrayAdapter<String> messageHistoryAdapter;
    private Map<String, Integer> hashtagCount = new HashMap<String, Integer>();
    private String firstTag = "";
    private String secondTag = "";
    private String thirdTag = "";
    private List<CharSequence> filters = new ArrayList<CharSequence>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        messageHistoryView = (ListView) findViewById(R.id.messageHistoryView);
        messageText = (EditText) findViewById(R.id.message);
        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        hashtagCount.put("", 0);

        //filters.add("hello");

        messageHistoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked , messageHistory);
        messageHistoryView.setAdapter(messageHistoryAdapter);
        new DownloadMessages().execute();

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //messageHistoryAdapter.getFilter()
                String message = messageText.getText().toString();
                countHashtags(message);
                messageHistory.add(message);
                messageHistoryAdapter.notifyDataSetChanged();
                messageText.setText("");
                messageHistoryView.setSelection(messageHistoryAdapter.getCount() - 1);



                //TODO: SEND TO SERVER (with id?)
            }
        });
    }

    private void filterByHashtag() {
        for(CharSequence filter : filters)
            Chat.this.messageHistoryAdapter.getFilter().filter("#");
    }

    private void countHashtags(String message) {
        Matcher matcher = Pattern.compile("#\\s*(\\w+)").matcher(message);
        while (matcher.find()) {
            String tag = matcher.group(1);
            if(hashtagCount.get(tag) == null)
                hashtagCount.put(tag, 1);
            else
                hashtagCount.put(tag, hashtagCount.get(tag) + 1);
            checkTopHashtags(tag);
        }
        showTopTags();
    }

    private void checkTopHashtags(String tag) {
        int count = hashtagCount.get(tag);

        if(count > hashtagCount.get(firstTag)) {
            thirdTag = secondTag;
            secondTag = firstTag;
            firstTag = tag;
        } else if(count > hashtagCount.get(secondTag)) {
            thirdTag = secondTag;
            secondTag = tag;
        } else if(count > hashtagCount.get(thirdTag)) {
            thirdTag = tag;
        }
    }

    private void showTopTags() {
        TextView firstView = (TextView) findViewById(R.id.top_1);
        firstView.setText("#" + firstTag);
        TextView secondView = (TextView) findViewById(R.id.top_2);
        secondView.setText("#" + secondTag);
        TextView thirdView = (TextView) findViewById(R.id.top_3);
        thirdView.setText("#" + thirdTag);
    }

    private class DownloadMessages extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... nothing) {
            String message = null;
            try {
                HttpParams params = new BasicHttpParams();
                HttpClient httpClient = new DefaultHttpClient(params);

                //prepare the HTTP GET call
                HttpGet httpget = new HttpGet("http://matphillips.com/st/receive.php");
                //get the response entity
                HttpEntity entity = httpClient.execute(httpget).getEntity();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return message;
        }

        protected void onPostExecute(String response) {
            //this.execute();
            try {
                JSONArray jsonArray = new JSONArray(response.trim());
                if(jsonArray != null) {
                    for(int i = 0 ; i < jsonArray.length() ; i++) {
                        JSONObject object1 = (JSONObject) jsonArray.get(i);
                        String message = object1.getString("m");
                        messageHistory.add(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            messageHistoryAdapter.notifyDataSetChanged();

        }

    }
}