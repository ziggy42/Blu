package com.andreapivetta.blu.services;

import android.app.IntentService;
import android.content.Intent;

import com.andreapivetta.blu.data.FollowersDatabaseManager;
import com.andreapivetta.blu.data.Notification;
import com.andreapivetta.blu.twitter.TwitterUtils;
import com.andreapivetta.blu.utilities.Common;

import java.util.ArrayList;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;


public class CheckFollowersService extends IntentService {

    public CheckFollowersService() {
        super("CheckFollowersService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Twitter twitter = TwitterUtils.getTwitter(getApplicationContext());
        FollowersDatabaseManager dbm = new FollowersDatabaseManager(getApplicationContext());
        dbm.open();
        try {
            ArrayList<Long> usersIDs = new ArrayList<>();
            IDs ids = twitter.getFollowersIDs(-1);
            do {
                for (long userID : ids.getIDs())
                    usersIDs.add(userID);
            } while (ids.hasNext());

            ArrayList<Long> newUsersIDs = dbm.check(usersIDs);
            for (long userID : newUsersIDs)
                Common.pushNotification(-1L, userID, Notification.TYPE_FOLLOW, getApplicationContext());
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        dbm.close();
    }
}