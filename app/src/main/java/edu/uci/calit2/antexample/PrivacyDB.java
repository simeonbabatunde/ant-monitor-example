/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *
 *  AntMonitor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  AntMonitor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntMonitor. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.uci.calit2.antexample;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PrivacyDB {

    private static final String TAG = "PrivacyDB";
    public static final String DATABASE_NAME = "SCREENSAVER_DATABASE";
    public static final String TABLE_LEAKS_LOGS= "TABLE_LEAKS_LOGS";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_APP = "appname";
    public static final String COLUMN_PII_LABEL = "label";
    public static final String COLUMN_TIME = "timestamp";
    public static final String COLUMN_REMOTE_IP = "remoteIp";

    private static PrivacyDB instance;
    private static final int DATABASE_VERSION = 1;
    private SQLHandler sqlHandler;
    private SQLiteDatabase _database;

    private synchronized boolean isClose() {
        return _database == null;
    }

    /** Called when database is first created */
    private static class SQLHandler extends SQLiteOpenHelper {

        public SQLHandler(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Called when database is first created
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_LEAKS_LOGS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_APP + " TEXT NOT NULL, "
                    + COLUMN_PII_LABEL + " TEXT NOT NULL,"
                    + COLUMN_TIME + " INTEGER , "
                    + COLUMN_REMOTE_IP + " TEXT NOT NULL);");


        }

        /** If database exists, this method will be called */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Upgrade new tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAKS_LOGS);

            onCreate(db);
        }

    }

    /** Database constructor */
    public PrivacyDB(Context c) {
        sqlHandler = new SQLHandler(c);
    }

    /**Singleton getter.
     * @param c context used to open the database
     * @return The current instance of PrivacyDB, if none, a new instance is created.
     * After calling this method, the database is open for writing. */
    public static PrivacyDB getInstance(Context c)
    {
        if (instance == null)
            instance = new PrivacyDB(c);

        if (instance._database == null) {
            instance._database = instance.sqlHandler.getWritableDatabase();
        }

        return instance;
    }

    public SQLiteDatabase getDatabase() {
        if (this.isClose()) {
            _database = sqlHandler.getWritableDatabase();
        }
        return _database;
    }

    /**Retrieves a history of leaked events for an Application
     * @return A cursor pointing to the data. Caller must close the cursor.
     * Cursor should have data for filter label, filter action taken (none, hashed, blocked, etc), and timestamp */
    public synchronized Cursor getPrivacyLeaksAppHistory(final String appName) {
        return getDatabase().query(TABLE_LEAKS_LOGS, new String[] {
                        COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL,COLUMN_TIME},
                COLUMN_APP + " = ?", new String[] {appName},
                null, null, COLUMN_TIME + " DESC");
    }

    /** Logs the leak for historical purposes
     * @param appName the name of the app responsible for the leak
     * @param pii the leaking string
     * @param action the action being taken
     * @return the row ID of the updated row, or -1 if an error occurred
     */
    public synchronized long logLeak (String appName, String remoteIp,  String label, long Timestampe) {
        // Add leak
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_APP, appName);
        cv.put(COLUMN_PII_LABEL, label);
        cv.put(COLUMN_TIME, Timestampe);
        cv.put(COLUMN_REMOTE_IP, remoteIp);
        return getDatabase().insert(TABLE_LEAKS_LOGS, null, cv);
    }
    /** Close the database */
    public synchronized void close() {
        sqlHandler.close();
        _database = null;
    }

}
