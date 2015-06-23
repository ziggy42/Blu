package com.andreapivetta.blu.activities;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.andreapivetta.blu.R;
import com.andreapivetta.blu.adapters.ConversationAdapter;
import com.andreapivetta.blu.data.DirectMessagesDatabaseManager;
import com.andreapivetta.blu.data.Message;
import com.andreapivetta.blu.twitter.SendDirectMessage;
import com.andreapivetta.blu.twitter.TwitterUtils;

import java.util.ArrayList;
import java.util.Calendar;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class ConversationActivity extends ThemedActivity {

    private User currentUser;
    private Twitter twitter;
    private long userID;
    private ArrayList<Message> mDataSet = new ArrayList<>();

    private ProgressBar loadingProgressBar;
    private Toolbar toolbar;
    private EditText messageEditText;
    private RecyclerView mRecyclerView;

    private ConversationAdapter mAdapter;
    private DataUpdateReceiver dataUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        userID = getIntent().getLongExtra("ID", 0L);

        twitter = TwitterUtils.getTwitter(ConversationActivity.this);
        ImageButton sendMessageImageButton = (ImageButton) findViewById(R.id.sendMessageImageButton);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        messageEditText = (EditText) findViewById(R.id.messageEditText);

        mRecyclerView = (RecyclerView) findViewById(R.id.conversationRecyclerView);
        mAdapter = new ConversationAdapter(mDataSet, ConversationActivity.this);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        sendMessageImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                if (message.length() > 0) {
                    new SendDirectMessage(ConversationActivity.this, twitter, userID, currentUser.getName(),
                            currentUser.getBiggerProfileImageURL()).execute(message, null, null);

                    mDataSet.add(
                            new Message(0L, PreferenceManager.getDefaultSharedPreferences(ConversationActivity.this)
                                    .getLong(getString(R.string.pref_key_logged_user), 0L), 0L, message, Calendar.getInstance().getTime().getTime(), "", "", true));

                    mAdapter.notifyDataSetChanged();

                    messageEditText.setText("");
                    mRecyclerView.scrollToPosition(mDataSet.size() - 1);
                }
            }
        });

        if (userID == 0)
            finish();
        else
            new Loader().execute(null, null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dataUpdateReceiver == null)
            dataUpdateReceiver = new DataUpdateReceiver();

        registerReceiver(dataUpdateReceiver, new IntentFilter(Message.NEW_MESSAGE_INTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (dataUpdateReceiver != null)
            unregisterReceiver(dataUpdateReceiver);
    }

    private class Loader extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                currentUser = twitter.showUser(userID);
                DirectMessagesDatabaseManager dbm = new DirectMessagesDatabaseManager(ConversationActivity.this);
                dbm.open();

                for (Message message : dbm.getConversation(userID))
                    mDataSet.add(message);

                for (int i = mDataSet.size() - 1; i >= 0; i--)
                    if (!mDataSet.get(i).isRead())
                        dbm.setRead(mDataSet.get(i).getMessageID());
                    else
                        break;

                dbm.close();
            } catch (TwitterException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                loadingProgressBar.setVisibility(View.GONE);
                toolbar.setTitle(currentUser.getName());
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public class DataUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Message.NEW_MESSAGE_INTENT)) {
                mDataSet.clear();
                DirectMessagesDatabaseManager dbm = new DirectMessagesDatabaseManager(ConversationActivity.this);
                dbm.open();
                for (Message message : dbm.getConversation(userID))
                    mDataSet.add(message);
                mAdapter.notifyDataSetChanged();

                for (int i = mDataSet.size() - 1; i >= 0; i--)
                    if (!mDataSet.get(i).isRead())
                        dbm.setRead(mDataSet.get(i).getMessageID());
                    else
                        break;

                dbm.close();

                NotificationManager nMgr = (NotificationManager)
                        getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                nMgr.cancel((int) mDataSet.get(mDataSet.size() - 1).getMessageID());

                mRecyclerView.scrollToPosition(mDataSet.size() - 1);
            }
        }
    }
}
