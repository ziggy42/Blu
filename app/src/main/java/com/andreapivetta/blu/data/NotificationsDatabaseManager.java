package com.andreapivetta.blu.data;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class NotificationsDatabaseManager {

    private static final String DB_NAME = "notifications_db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + SetsMetaData.TABLE_NAME + " (id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + SetsMetaData.TYPE + " PM_TEXT NOT NULL,"
            + SetsMetaData.STATUS + " PM_TEXT,"
            + SetsMetaData.PICURL + " PM_TEXT NOT NULL,"
            + SetsMetaData.USER + " INTEGER NOT NULL,"
            + SetsMetaData.TARGET_TWEET + " INTEGER,"
            + SetsMetaData.FLAG_READ + " BOOLEAN NOT NULL,"
            + SetsMetaData.DAY + " INTEGER NOT NULL,"
            + SetsMetaData.MONTH + " INTEGER NOT NULL,"
            + SetsMetaData.YEAR + " INTEGER NOT NULL,"
            + SetsMetaData.HOUR + " INTEGER NOT NULL,"
            + SetsMetaData.MINUTE + " INTEGER NOT NULL,"
            + SetsMetaData.USERID + " INTEGER NOT NULL);";
    private DatabaseHelper myDBhelper;
    private SQLiteDatabase myDB;

    public NotificationsDatabaseManager(Context context) {
        this.myDBhelper = new DatabaseHelper(context, DB_NAME, DB_VERSION, TABLE_CREATE);
    }

    public void open() {
        this.myDB = myDBhelper.getWritableDatabase();
    }

    public void close() {
        this.myDB.close();
    }

    public void clearDatabase() {
        myDB.execSQL("DROP TABLE IF EXISTS " + SetsMetaData.TABLE_NAME);
        myDB.execSQL(TABLE_CREATE);
    }

    public long insertNotification(Notification notification) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SetsMetaData.TYPE, notification.type);
        contentValues.put(SetsMetaData.USER, notification.user);
        contentValues.put(SetsMetaData.FLAG_READ, notification.read);
        contentValues.put(SetsMetaData.DAY, notification.DD);
        contentValues.put(SetsMetaData.MONTH, notification.MM);
        contentValues.put(SetsMetaData.YEAR, notification.YY);
        contentValues.put(SetsMetaData.HOUR, notification.hh);
        contentValues.put(SetsMetaData.MINUTE, notification.mm);
        contentValues.put(SetsMetaData.PICURL, notification.profilePicURL);
        contentValues.put(SetsMetaData.USERID, notification.userID);

        if (notification.tweetID > 0) {
            contentValues.put(SetsMetaData.TARGET_TWEET, notification.tweetID);
            contentValues.put(SetsMetaData.STATUS, notification.status);
        }

        return myDB.insert(SetsMetaData.TABLE_NAME, null, contentValues);
    }



    private ArrayList<Notification> getAllNotifications(boolean unread) {
        ArrayList<Notification> notifications = new ArrayList<>();
        String sqlQuery = "SELECT * FROM " + SetsMetaData.TABLE_NAME + " WHERE " + ((unread) ? "" : "NOT ") + SetsMetaData.FLAG_READ +
                " ORDER BY " + SetsMetaData.YEAR + " DESC, " + SetsMetaData.MONTH + " DESC, " + SetsMetaData.DAY + " DESC, " +
                SetsMetaData.HOUR + " DESC, " + SetsMetaData.MINUTE + " DESC";

        Cursor c = myDB.rawQuery(sqlQuery, null);
        while (c.moveToNext()) {
            notifications.add(new Notification(c.getInt(6) == 1, c.getLong(5),
                    c.getString(4), c.getString(1), c.getString(2), c.getString(3),
                    c.getInt(10), c.getInt(11), c.getInt(9), c.getInt(8), c.getInt(7), c.getLong(12), c.getInt(0)));
        }

        c.close();
        return notifications;
    }

    public ArrayList<Notification> getAllUnreadNotifications() {
        return getAllNotifications(false);
    }

    public ArrayList<Notification> getAllReadNotifications() {
        return getAllNotifications(true);
    }

    public void setAllAsRead() {
        ContentValues cv = new ContentValues();
        cv.put(SetsMetaData.FLAG_READ, true);
        myDB.update(SetsMetaData.TABLE_NAME, cv, "NOT " + SetsMetaData.FLAG_READ, null);
    }

    public int getCountUnreadNotifications() {
        return (int) DatabaseUtils.queryNumEntries(myDB, SetsMetaData.TABLE_NAME, "NOT " + SetsMetaData.FLAG_READ);
    }

    static final class SetsMetaData {
        static final String TABLE_NAME = "notifications_table";
        static final String TYPE = "not_type";
        static final String USER = "user";
        static final String TARGET_TWEET = "tweet";
        static final String FLAG_READ = "read";
        static final String DAY = "day";
        static final String MONTH = "month";
        static final String YEAR = "year";
        static final String HOUR = "hour";
        static final String MINUTE = "minute";
        static final String STATUS = "status";
        static final String PICURL = "picurl";
        static final String USERID = "fuckinguserid";
    }

}
