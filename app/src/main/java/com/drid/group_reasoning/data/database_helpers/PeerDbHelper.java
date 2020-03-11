package com.drid.group_reasoning.data.database_helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.drid.group_reasoning.data.contracts.PeerContract.PeerEntry;

public class PeerDbHelper extends SQLiteOpenHelper {

    public static final String TAG = PeerDbHelper.class.getSimpleName();
    public static final String DATABASE = "peers.db";
    public static final int DATABASE_VERSION = 1;

    public PeerDbHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the peers table
        String SQL_CREATE_PEERS_TABLE =  "CREATE TABLE " + PeerEntry.TABLE_NAME + " ("
                + PeerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PeerEntry.COLUMN_PEER_ID + " TEXT NOT NULL, "
                + PeerEntry.COLUMN_PEER_NAME + " TEXT NOT NULL, "
                + PeerEntry.COLUMN_PEER_STATUS + " TEXT NOT NULL);";

        db.execSQL(SQL_CREATE_PEERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
