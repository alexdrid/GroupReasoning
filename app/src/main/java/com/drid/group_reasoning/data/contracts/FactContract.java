package com.drid.group_reasoning.data.contracts;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class FactContract {

    public static final String CONTENT_AUTHORITY
            = "com.drid.group_reasoning.data.content_providers.FactProvider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_FACTS = "facts";

    public FactContract() {
    }

    public static final class FactEntry implements BaseColumns {
        public static final Uri FACT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FACTS);
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FACTS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FACTS;

        public static final String TABLE_NAME = "facts";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_FACT_SYMBOL = "fact_symbol";
        public static final String COLUMN_FACT_PROPOSITION = "fact_proposition";

    }
}
