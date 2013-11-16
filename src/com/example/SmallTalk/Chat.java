package com.example.SmallTalk;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
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
        for(int i=0; i<4; i++) {
            DownloadMessages my_task = new DownloadMessages();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                my_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                my_task.execute();
        }


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
                //TODO: Send with some id
                PostMessageTask my_task = new PostMessageTask();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    my_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
                else
                    my_task.execute(message);
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

    private class DownloadMessages extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... nothing) {
            String message = null;
            try {

                String url = "http://matphillips.com/st/receive.php";

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                // optional default is GET
                con.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                message = response.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return message;
        }

        protected void onPostExecute(String response) {

            try {
                JSONArray jsonArray = new JSONArray(response.trim());
                if(jsonArray != null) {
                    for(int i = 0 ; i < jsonArray.length() ; i++) {
                        JSONObject object1 = (JSONObject) jsonArray.get(i);
                        String message = object1.getString("m");
                        messageHistory.add(message);
                        countHashtags(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            messageHistoryAdapter.notifyDataSetChanged();
        }

    }

    private class PostMessageTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... message) {
            StringBuffer response = new StringBuffer();
            try {
                String url = "http://matphillips.com/st/send.php";
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");

                String urlParameters = "m=" + message;

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            } catch (Exception e) {
                Log.e("HTTP", "Error in http connection " + e.toString());
            }
            return message[0];
        }

        protected void onPostExecute(String response) {
            System.out.println(response);
        }

    }
}
