package com.drid.group_reasoning.data.database_helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.drid.group_reasoning.data.contracts.RuleContract.RuleEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.SymbolEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleSymbolEntry;

public class RuleDbHelper extends SQLiteOpenHelper {

    public static final String TAG = RuleDbHelper.class.getSimpleName();
    public static final String DATABASE = "rules.db";
    public static final int DATABASE_VERSION = 1;

    public RuleDbHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // A String that contains the SQL statement to create the rules table
        String CREATE_RULES_TABLE =  "CREATE TABLE " + RuleEntry.TABLE_RULES + " ("
                + RuleEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RuleEntry.COLUMN_RULE_NL + " TEXT NOT NULL, "
                + RuleEntry.COLUMN_RULE_PL + " TEXT NOT NULL);";

        // A String that contains the SQL statement to create the symbols table
        String CREATE_SYMBOLS_TABLE =  "CREATE TABLE " + SymbolEntry.TABLE_SYMBOLS + " ("
                + SymbolEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SymbolEntry.COLUMN_SYMBOL + " TEXT NOT NULL UNIQUE ON CONFLICT REPLACE, "
                + SymbolEntry.COLUMN_PROPOSITION + " TEXT NOT NULL UNIQUE ON CONFLICT REPLACE);";

        // A String that contains the SQL statement to create the symbols table
        String CREATE_RULES_SYMBOLS_TABLE =  "CREATE TABLE " + RuleSymbolEntry.TABLE_RULES_SYMBOLS + " ("
                + RuleSymbolEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RuleSymbolEntry.KEY_RULE_ID + " INTEGER , "
                + RuleSymbolEntry.KEY_SYMBOL_ID + " INTEGER," +
                " FOREIGN KEY (" + RuleSymbolEntry.KEY_RULE_ID + ") " +
                " REFERENCES " + RuleEntry.TABLE_RULES +"( "+RuleEntry._ID +")" +
                " ON UPDATE CASCADE ON DELETE CASCADE," +
                " FOREIGN KEY (" + RuleSymbolEntry.KEY_SYMBOL_ID + ")"  +
                " REFERENCES " + SymbolEntry.TABLE_SYMBOLS +"( "+ SymbolEntry._ID +")" +
                " ON UPDATE CASCADE ON DELETE CASCADE);";



        db.execSQL(CREATE_RULES_TABLE);
        db.execSQL(CREATE_SYMBOLS_TABLE);
        db.execSQL(CREATE_RULES_SYMBOLS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RuleEntry.TABLE_RULES);
        db.execSQL("DROP TABLE IF EXISTS " + SymbolEntry.TABLE_SYMBOLS);
        db.execSQL("DROP TABLE IF EXISTS " + RuleSymbolEntry.TABLE_RULES_SYMBOLS);
        onCreate(db);
    }
}
