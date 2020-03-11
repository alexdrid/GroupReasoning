package com.drid.group_reasoning.data.contracts;

import android.Manifest;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import java.security.Provider;

public final class PeerContract {

    public static final String CONTENT_AUTHORITY =
            "com.drid.group_reasoning.data.content_providers.PeerProvider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PEERS = "peers";
    public static final String PATH_PEER_ID = "peer_id";

    public PeerContract() {
    }

    public static final class PeerEntry implements BaseColumns{
        public static final Uri PEER_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PEERS);
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PEERS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PEERS;

        public static final String TABLE_NAME = "peers";


        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PEER_ID = "peer_id";
        public static final String COLUMN_PEER_NAME = "name";
        public static final String COLUMN_PEER_STATUS = "status";

    }
}
