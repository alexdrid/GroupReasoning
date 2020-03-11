package com.drid.group_reasoning.data.content_providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.drid.group_reasoning.data.contracts.RuleContract;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.RuleSymbolEntry;
import com.drid.group_reasoning.data.contracts.RuleContract.SymbolEntry;
import com.drid.group_reasoning.data.database_helpers.RuleDbHelper;

public class RuleProvider extends ContentProvider {

    public static final String TAG = RuleProvider.class.getSimpleName();

    private RuleDbHelper ruleDbHelper;

    private static final int RULES = 100;
    private static final int RULE_ID = 101;
    private static final int SYMBOLS = 102;
    private static final int SYMBOL_PROPOSITION = 103;
    private static final int RULE_SYMBOL = 104;


    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final SQLiteQueryBuilder joinedTablesQueryBuilder;

    static {
        matcher.addURI(RuleContract.CONTENT_AUTHORITY, RuleContract.PATH_RULES, RULES);
        matcher.addURI(
                RuleContract.CONTENT_AUTHORITY, RuleContract.PATH_RULES + "/#", RULE_ID);

        matcher.addURI(RuleContract.CONTENT_AUTHORITY, RuleContract.PATH_SYMBOLS, SYMBOLS);
        matcher.addURI(
                RuleContract.CONTENT_AUTHORITY, RuleContract.PATH_SYMBOLS + "/*" ,
                SYMBOL_PROPOSITION);

        matcher.addURI(
                RuleContract.CONTENT_AUTHORITY, RuleContract.PATH_RULES_SYMBOLS, RULE_SYMBOL);

    }


    static {
        joinedTablesQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        joinedTablesQueryBuilder.setTables(
                RuleSymbolEntry.TABLE_RULES_SYMBOLS + " INNER JOIN " +
                        SymbolEntry.TABLE_SYMBOLS +
                        " ON " + RuleSymbolEntry.TABLE_RULES_SYMBOLS +
                        "." + RuleSymbolEntry.KEY_SYMBOL_ID +
                        " = " + SymbolEntry.TABLE_SYMBOLS +
                        "." + SymbolEntry._ID);
    }

    @Override
    public boolean onCreate() {
        ruleDbHelper = new RuleDbHelper(getContext());
        return false;
    }


    @Override
    public Cursor query(
            Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        Cursor cursor;

        int match = matcher.match(uri);

        switch (match) {
            case RULES:
                cursor = database.query(
                        RuleEntry.TABLE_RULES,
                        projection, selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case RULE_ID:
                selection = RuleEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(
                        RuleEntry.TABLE_RULES,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null, sortOrder);
                break;
            case SYMBOL_PROPOSITION:
                cursor = database.query(
                        SymbolEntry.TABLE_SYMBOLS,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case RULE_SYMBOL:
                cursor = joinedTablesQueryBuilder.query(
                        ruleDbHelper.getWritableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null
                );
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        int match = matcher.match(uri);
        switch (match) {
            case RULES:
                return RuleEntry.RULE_LIST_TYPE;
            case RULE_ID:
                return RuleEntry.RULE_ITEM_TYPE;
            case SYMBOLS:
                return SymbolEntry.SYMBOL_LIST_TYPE;
            case SYMBOL_PROPOSITION:
                return SymbolEntry.SYMBOL_ITEM_TYPE;
            case RULE_SYMBOL:
                return RuleSymbolEntry.RULE_SYMBOL_LIST_TYPE;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = matcher.match(uri);
        switch (match) {
            case RULES:
                return insertRule(uri, values);
            case SYMBOLS:
                return insertSymbol(uri, values);
            case RULE_SYMBOL:
                return insertRuleSymbol(uri, values);
            default:
                throw new IllegalArgumentException("Insertion not supported for " + uri);
        }
    }

    private Uri insertRule(Uri uri, ContentValues values) {
        String name = values.getAsString(RuleEntry.COLUMN_RULE_NL);
        if (name == null) {
            throw new IllegalArgumentException("Rule name required");
        }
        String status = values.getAsString(RuleEntry.COLUMN_RULE_PL);
        if (status == null) {
            throw new IllegalArgumentException("Rule status required");
        }


        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        long id = database.insert(RuleEntry.TABLE_RULES, null, values);

        if (id == -1) {
            Log.e(TAG, "Failed to insert for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    private Uri insertSymbol(Uri uri, ContentValues values) {
        String symbol = values.getAsString(SymbolEntry.COLUMN_SYMBOL);
        if (symbol == null) {
            throw new IllegalArgumentException("Symbol required");
        }
        String proposition = values.getAsString(SymbolEntry.COLUMN_PROPOSITION);
        if (proposition == null) {
            throw new IllegalArgumentException("Proposition required");
        }


        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        long id = database.insertWithOnConflict(SymbolEntry.TABLE_SYMBOLS, null,
                values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == 0) {

        }

        if (id == -1) {
            Log.e(TAG, "Failed to insert for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    private Uri insertRuleSymbol(Uri uri, ContentValues values) {
        int rule_id = values.getAsInteger(RuleSymbolEntry.KEY_RULE_ID);
        if (rule_id == 0) {
            throw new IllegalArgumentException("Rule id required");
        }
        int symbol_id = values.getAsInteger(RuleSymbolEntry.KEY_SYMBOL_ID);
        if (symbol_id == 0) {
            throw new IllegalArgumentException("Symbol id required");
        }

        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        long id = database.insert(RuleSymbolEntry.TABLE_RULES_SYMBOLS, null, values);

        if (id == -1) {
            Log.e(TAG, "Failed to insert for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = matcher.match(uri);

        switch (match) {
            case RULES:
                return updateRule(uri, values, selection, selectionArgs);
            case RULE_ID:
                selection = RuleEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateRule(uri, values, selection, selectionArgs);
            case SYMBOLS:
            case SYMBOL_PROPOSITION:
                return updateSymbol(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateRule(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(RuleEntry.COLUMN_RULE_NL)) {
            String name = values.getAsString(RuleEntry.COLUMN_RULE_NL);
            if (name == null) {
                throw new IllegalArgumentException("Rule in natural language required");
            }
        }

        if (values.containsKey(RuleEntry.COLUMN_RULE_PL)) {
            String status = values.getAsString(RuleEntry.COLUMN_RULE_NL);
            if (status == null) {
                throw new IllegalArgumentException("Rule propositional logic required");
            }
        }


        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(RuleEntry.TABLE_RULES, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    private int updateSymbol(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(SymbolEntry.COLUMN_SYMBOL)) {
            String symbol = values.getAsString(SymbolEntry.COLUMN_SYMBOL);
            if (symbol == null) {
                throw new IllegalArgumentException("Symbol required");
            }
        }

        if (values.containsKey(SymbolEntry.COLUMN_PROPOSITION)) {
            String value = values.getAsString(SymbolEntry.COLUMN_PROPOSITION);
            if (value == null) {
                throw new IllegalArgumentException("Proposition required");
            }
        }


        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(
                SymbolEntry.TABLE_SYMBOLS, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = ruleDbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = matcher.match(uri);

        switch (match) {
            case RULES:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(RuleEntry.TABLE_RULES, selection, selectionArgs);
                database.execSQL("DELETE FROM SQLITE_SEQUENCE");
                break;
            case RULE_ID:
                getContext().getContentResolver().notifyChange(uri, null);
                // Delete a single row given by the ID in the URI
                selection = RuleEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(RuleEntry.TABLE_RULES, selection, selectionArgs);
                break;
            case SYMBOLS:
                rowsDeleted = database.delete(SymbolEntry.TABLE_SYMBOLS, selection, selectionArgs);
                break;
            case RULE_SYMBOL:
                rowsDeleted = database.delete(RuleSymbolEntry.TABLE_RULES_SYMBOLS, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }
}
