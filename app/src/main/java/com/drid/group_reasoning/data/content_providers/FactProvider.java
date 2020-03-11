package com.drid.group_reasoning.data.content_providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.drid.group_reasoning.data.contracts.FactContract;
import com.drid.group_reasoning.data.contracts.FactContract.FactEntry;
import com.drid.group_reasoning.data.database_helpers.FactDbHelper;

public class FactProvider extends ContentProvider {

    public static final String TAG = FactProvider.class.getSimpleName();

    private FactDbHelper factDbHelper;

    private static final int FACTS = 100;
    private static final int FACT_ID = 101;

    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(FactContract.CONTENT_AUTHORITY, FactContract.PATH_FACTS, FACTS);
        matcher.addURI(FactContract.CONTENT_AUTHORITY,
                FactContract.PATH_FACTS + "/#", FACT_ID);
    }


    @Override
    public boolean onCreate() {
        factDbHelper = new FactDbHelper(getContext());
        return false;
    }


    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        SQLiteDatabase database = factDbHelper.getWritableDatabase();

        Cursor cursor;

        int match = matcher.match(uri);

        switch (match) {
            case FACTS:
                cursor = database.query(FactEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case FACT_ID:
                selection = FactEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(FactEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
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
            case FACTS:
                return FactEntry.CONTENT_LIST_TYPE;
            case FACT_ID:
                return FactEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = matcher.match(uri);
        switch (match) {
            case FACTS:
                return insertFact(uri, values);
            default:
                throw new IllegalArgumentException("Insertion not supported for " + uri);
        }
    }


    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        final int match = matcher.match(uri);

        switch (match) {
            case FACTS:
                return updateFact(uri, values, selection, selectionArgs);
            case FACT_ID:
                selection = FactEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateFact(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = factDbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = matcher.match(uri);

        switch (match) {
            case FACTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(FactEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case FACT_ID:
                getContext().getContentResolver().notifyChange(uri, null);
                // Delete a single row given by the ID in the URI
                selection = FactEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(FactEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }


    private Uri insertFact(Uri uri, ContentValues values) {
        String name = values.getAsString(FactEntry.COLUMN_FACT_SYMBOL);
        if (name == null) {
            throw new IllegalArgumentException("Fact in natural language required");
        }
        String status = values.getAsString(FactEntry.COLUMN_FACT_PROPOSITION);
        if (status == null) {
            throw new IllegalArgumentException("Fact in propositional logic  required");
        }


        SQLiteDatabase database = factDbHelper.getWritableDatabase();

        long id = database.insert(FactEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(TAG, "Failed to insert for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    private int updateFact(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {

        if (values.containsKey(FactEntry.COLUMN_FACT_SYMBOL)) {
            String name = values.getAsString(FactEntry.COLUMN_FACT_SYMBOL);
            if (name == null) {
                throw new IllegalArgumentException("Fact in natural language required");
            }
        }

        if (values.containsKey(FactEntry.COLUMN_FACT_PROPOSITION)) {
            String status = values.getAsString(FactEntry.COLUMN_FACT_PROPOSITION);
            if (status == null) {
                throw new IllegalArgumentException("Fact in propositional logic  required");
            }
        }


        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = factDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(FactEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }
}
