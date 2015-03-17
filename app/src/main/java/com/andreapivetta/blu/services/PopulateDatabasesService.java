package com.andreapivetta.blu.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.andreapivetta.blu.data.FavoritesDatabaseManager;
import com.andreapivetta.blu.data.FollowersDatabaseManager;
import com.andreapivetta.blu.data.MentionsDatabaseManager;
import com.andreapivetta.blu.data.RetweetsDatabaseManager;
import com.andreapivetta.blu.twitter.TwitterUtils;
import com.andreapivetta.blu.utilities.Common;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;


public class PopulateDatabasesService extends IntentService {

    public PopulateDatabasesService() {
        super("PopulateDatabasesService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("PopulateService", "Populating...");

        Twitter twitter = TwitterUtils.getTwitter(getApplicationContext());
        FavoritesDatabaseManager favoritesDatabaseManager = new FavoritesDatabaseManager(getApplicationContext());
        RetweetsDatabaseManager retweetsDatabaseManager = new RetweetsDatabaseManager(getApplicationContext());
        MentionsDatabaseManager mentionsDatabaseManager = new MentionsDatabaseManager(getApplicationContext());
        FollowersDatabaseManager followersDatabaseManager = new FollowersDatabaseManager(getApplicationContext());

        try {
            favoritesDatabaseManager.open();
            retweetsDatabaseManager.open();
            favoritesDatabaseManager.clearDatabase();
            retweetsDatabaseManager.clearDatabase();
            for (Status tmp : twitter.getUserTimeline(new Paging(1, 200))) {
                for (long userID : Common.getFavoriters(tmp.getId()))
                    favoritesDatabaseManager.insertCouple(userID, tmp.getId());

                for (long userID : Common.getRetweeters(tmp.getId()))
                    retweetsDatabaseManager.insertCouple(userID, tmp.getId());
            }

            favoritesDatabaseManager.close();
            retweetsDatabaseManager.close();

            mentionsDatabaseManager.open();
            mentionsDatabaseManager.clearDatabase();
            for (Status tmp : twitter.getMentionsTimeline(new Paging(1, 200)))
                mentionsDatabaseManager
                        .insertTriple(tmp.getId(), tmp.getUser().getId(), tmp.getCreatedAt().getTime());
            mentionsDatabaseManager.close();

            followersDatabaseManager.open();
            followersDatabaseManager.clearDatabase();
            IDs ids = twitter.getFollowersIDs(-1);
            do {
                for (long userID : ids.getIDs())
                    followersDatabaseManager.insertFollower(userID);
            } while (ids.hasNext());
            followersDatabaseManager.close();

            getApplicationContext().getSharedPreferences(Common.PREF, 0).edit()
                    .putBoolean(Common.PREF_DATABASE_POPULATED, true).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("PopulateService", "FINISH");
    }

}
