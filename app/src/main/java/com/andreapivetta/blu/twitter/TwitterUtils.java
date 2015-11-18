package com.andreapivetta.blu.twitter;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

public final class TwitterUtils {
    public static final String CALLBACK_URL = "http://andreapivetta.altervista.org/";
    public static final boolean DUMMY_CALLBACK_URL = true;
    public static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    public static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    public static final String TWITTER_CONSUMER_KEY = "bqC07DsGd4GxN7a5SzPYvAkOm";
    public static final String TWITTER_CONSUMER_SECRET = "qJeeNMIuranov0m7hkGI6QvtcMx2BQYWqaYxwUegNf30bs1INO";

    private static AccessToken accessToken;
    private static TwitterFactory factory;

    private static AccessToken getAccessToken(Context context) {
        if (accessToken == null) {
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            accessToken = new AccessToken(mSharedPreferences.getString(TwitterUtils.PREF_KEY_OAUTH_TOKEN, ""),
                    mSharedPreferences.getString(TwitterUtils.PREF_KEY_OAUTH_SECRET, ""));
        }

        return accessToken;
    }

    private static TwitterFactory getFactory() {
        if (factory == null) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TwitterUtils.TWITTER_CONSUMER_KEY)
                    .setOAuthConsumerSecret(TwitterUtils.TWITTER_CONSUMER_SECRET);
            factory = new TwitterFactory(builder.build());
        }

        return factory;
    }

    public static Twitter getTwitter(Context context) {
        return getFactory().getInstance(getAccessToken(context));
    }

    public static void nullTwitter() {
        factory = null;
        accessToken = null;
    }

    public static TwitterStream getTwitterStream(Context context) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TwitterUtils.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(TwitterUtils.TWITTER_CONSUMER_SECRET);

        return new TwitterStreamFactory(builder.build()).getInstance(getAccessToken(context));
    }

}
