package com.drid.group_reasoning.data.content_providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.drid.group_reasoning.data.contracts.PeerContract;
import com.drid.group_reasoning.data.contracts.PeerContract.PeerEntry;
import com.drid.group_reasoning.data.database_helpers.PeerDbHelper;

public class PeerProvider extends ContentProvider {

    public static final String TAG = PeerProvider.class.getSimpleName();

    private PeerDbHelper peerDbHelper;

    private static final int PEERS = 100;
    private static final int PEER_ID = 101;

    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        matcher.addURI(PeerContract.CONTENT_AUTHORITY, PeerContract.PATH_PEERS, PEERS);
        matcher.addURI(PeerContract.CONTENT_AUTHORITY,
                PeerContract.PATH_PEERS + "/*", PEER_ID);
    }


    @Override
    public boolean onCreate() {
        peerDbHelper = new PeerDbHelper(getContext());
        return false;
    }


    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        SQLiteDatabase database = peerDbHelper.getWritableDatabase();

        Cursor cursor;

        int match = matcher.match(uri);

        switch (match) {
            case PEERS:
                cursor = database.query(PeerEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PEER_ID:
                selection = PeerEntry.COLUMN_PEER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PeerEntry.TABLE_NAME, projection, selection, selectionArgs,
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
            case PEERS:
                return PeerEntry.CONTENT_LIST_TYPE;
            case PEER_ID:
                return PeerEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = matcher.match(uri);
        switch (match) {
            case PEERS:
                return insertPeer(uri, values);
            default:
                throw new IllegalArgumentException("Insertion not supported for " + uri);
        }
    }

    private Uri insertPeer(Uri uri, ContentValues values) {
        String name = values.getAsString(PeerEntry.COLUMN_PEER_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Peer name required");
        }
        String status = values.getAsString(PeerEntry.COLUMN_PEER_STATUS);
        if (status == null) {
            throw new IllegalArgumentException("Peer status required");
        }


        SQLiteDatabase database = peerDbHelper.getWritableDatabase();

        long id = database.insert(PeerEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(TAG, "Failed to insert for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        final int match = matcher.match(uri);

        switch (match) {
            case PEERS:
            case PEER_ID:
                return updatePeer(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePeer(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        if (values.containsKey(PeerEntry.COLUMN_PEER_ID)) {
            String peer_id = values.getAsString(PeerEntry.COLUMN_PEER_NAME);
            if (peer_id == null) {
                throw new IllegalArgumentException("Peer id required");
            }
        }
        if (values.containsKey(PeerEntry.COLUMN_PEER_NAME)) {
            String name = values.getAsString(PeerEntry.COLUMN_PEER_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Peer name required");
            }
        }

        if (values.containsKey(PeerEntry.COLUMN_PEER_STATUS)) {
            String status = values.getAsString(PeerEntry.COLUMN_PEER_STATUS);
            if (status == null) {
                throw new IllegalArgumentException("Peer status required");
            }
        }


        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = peerDbHelper.getWritableDatabase();

        int rowsUpdated = database.update(PeerEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = peerDbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = matcher.match(uri);

        switch (match) {
            case PEERS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(PeerEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PEER_ID:
                getContext().getContentResolver().notifyChange(uri, null);
                // Delete a single row given by the ID in the URI
                selection = PeerEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(PeerEntry.TABLE_NAME, selection, selectionArgs);
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
