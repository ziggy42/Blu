package com.andreapivetta.blu.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.andreapivetta.blu.R;
import com.andreapivetta.blu.data.NotificationsDatabaseManager;
import com.andreapivetta.blu.services.NotificationService;
import com.andreapivetta.blu.twitter.TwitterUtils;
import com.andreapivetta.blu.utilities.Common;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;


public class HomeActivity extends TimeLineActivity {

    private static final int REQUEST_LOGIN = 0;
    private static final String UPCOMING_TWEET_COUNT_TAG = "UPCOMING_TWEET_COUNT";
    private static final String UPCOMING_TWEETS_LIST_TAG = "UPCOMING_TWEET_LIST";

    private SharedPreferences mSharedPreferences;
    private DataUpdateReceiver dataUpdateReceiver;
    private int mNotificationsCount = 0, newTweetsCount = 0;
    private ArrayList<Status> upComingTweets = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPreferences = getSharedPreferences(Common.PREF, 0);

        if (!isTwitterLoggedInAlready()) {
            startActivityForResult(new Intent(HomeActivity.this, LoginActivity.class), REQUEST_LOGIN);
        } else {
            twitter = TwitterUtils.getTwitter(HomeActivity.this);

            if (savedInstanceState != null) {
                tweetList = (ArrayList<Status>) savedInstanceState.getSerializable(TWEETS_LIST_TAG);
                upComingTweets = (ArrayList<Status>) savedInstanceState.getSerializable(UPCOMING_TWEETS_LIST_TAG);
                newTweetsCount = savedInstanceState.getInt(UPCOMING_TWEET_COUNT_TAG);
            } else {
                new GetTimeLine().execute(null, null, null);
            }
            startService(new Intent(HomeActivity.this, NotificationService.class));
        }

        NotificationsDatabaseManager databaseManager = new NotificationsDatabaseManager(HomeActivity.this);
        databaseManager.open();
        mNotificationsCount = databaseManager.getCountUnreadNotifications();
        databaseManager.close();

        super.onCreate(savedInstanceState);

        this.toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Status status : upComingTweets)
                    tweetList.add(0, status);

                mTweetsAdapter.notifyDataSetChanged();
                mLinearLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0);
                getSupportActionBar().setTitle(getString(R.string.app_name));
                newTweetsCount = 0;
                upComingTweets.clear();
            }
        });

        if (newTweetsCount > 0)
            getSupportActionBar().setTitle(
                    getResources().getQuantityString(R.plurals.new_tweets, newTweetsCount, newTweetsCount));
    }

    @Override
    String getInitialText() {
        return "";
    }

    @Override
    List<Status> getCurrentTimeLine() throws TwitterException {
        return twitter.getHomeTimeline(paging);
    }

    @Override
    List<Status> getRefreshedTimeLine(Paging paging) throws TwitterException {
        return twitter.getHomeTimeline(paging);
    }

    private boolean isTwitterLoggedInAlready() {
        return mSharedPreferences.getBoolean(TwitterUtils.PREF_KEY_TWITTER_LOGIN, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("exit")) {
            setIntent(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null) {
            if (("exit").equalsIgnoreCase(getIntent().getStringExtra(("exit")))) {
                onBackPressed();
            }
        }

        if (dataUpdateReceiver == null)
            dataUpdateReceiver = new DataUpdateReceiver();
        registerReceiver(dataUpdateReceiver, new IntentFilter(NotificationService.NEW_TWEETS_INTENT));
        registerReceiver(dataUpdateReceiver, new IntentFilter(NotificationService.NEW_NOTIFICATION_INTENT));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (dataUpdateReceiver != null)
            unregisterReceiver(dataUpdateReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (requestCode == REQUEST_LOGIN) {
            twitter = TwitterUtils.getTwitter(HomeActivity.this);
            new GetTimeLine().execute(null, null, null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(UPCOMING_TWEETS_LIST_TAG, upComingTweets);
        outState.putInt(UPCOMING_TWEET_COUNT_TAG, newTweetsCount);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_home, menu);

        MenuItem item = menu.findItem(R.id.action_notifications);
        MenuItemCompat.setActionView(item, R.layout.menu_notification_button);
        View view = MenuItemCompat.getActionView(item);
        ImageButton notificationImageButton = (ImageButton) view.findViewById(R.id.notificationImageButton);
        notificationImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNotificationsCount = 0;
                invalidateOptionsMenu();
                startActivity(new Intent(HomeActivity.this, NotificationsActivity.class));
            }
        });

        if (mNotificationsCount > 0) {
            TextView notificationsCountTextView = (TextView) view.findViewById(R.id.notificationCountTextView);
            notificationsCountTextView.setVisibility(View.VISIBLE);
            notificationsCountTextView.setText(String.valueOf(mNotificationsCount));
        }

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Intent i = new Intent(HomeActivity.this, SearchActivity.class);
                i.putExtra("QUERY", searchView.getQuery().toString());
                startActivity(i);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            onSearchRequested();
        } else if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(HomeActivity.this, UserActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NotificationService.NEW_TWEETS_INTENT)) {
                Status newStatus = (Status) intent.getSerializableExtra("PARCEL_STATUS");
                upComingTweets.add(newStatus);
                newTweetsCount++;
                getSupportActionBar().setTitle(
                        getResources().getQuantityString(R.plurals.new_tweets, newTweetsCount, newTweetsCount));
            } else if (intent.getAction().equals(NotificationService.NEW_NOTIFICATION_INTENT)) {
                mNotificationsCount++;
                invalidateOptionsMenu();
            }
        }
    }
}
