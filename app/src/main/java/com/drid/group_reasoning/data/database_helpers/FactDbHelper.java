package com.drid.group_reasoning.data.database_helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.drid.group_reasoning.data.contracts.FactContract.FactEntry;

public class FactDbHelper extends SQLiteOpenHelper {

    public static final String TAG = FactDbHelper.class.getSimpleName();
    public static final String DATABASE = "facts.db";
    public static final int DATABASE_VERSION = 1;

    public FactDbHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the facts table
        String SQL_CREATE_FACTS_TABLE =  "CREATE TABLE " + FactEntry.TABLE_NAME + " ("
                + FactEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FactEntry.COLUMN_FACT_SYMBOL + " TEXT NOT NULL, "
                + FactEntry.COLUMN_FACT_PROPOSITION + " TEXT NOT NULL, " +
                "UNIQUE ("+ FactEntry.COLUMN_FACT_SYMBOL +", "+ FactEntry.COLUMN_FACT_PROPOSITION +" )" +
                " ON CONFLICT IGNORE); ";


        db.execSQL(SQL_CREATE_FACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
