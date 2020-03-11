package com.drid.group_reasoning.data.contracts;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class RuleContract {

    public static final String CONTENT_AUTHORITY =
            "com.drid.group_reasoning.data.content_providers.RuleProvider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_RULES = "rules";
    public static final String PATH_SYMBOLS = "symbols";
    public static final String PATH_SYMBOL = "symbol";
    public static final String PATH_RULES_SYMBOLS = "rules_symbols";

    public RuleContract() {
    }

    public static final class RuleEntry implements BaseColumns {
        // Rules table uri
        public static final Uri RULES_URI =
                Uri.withAppendedPath(BASE_CONTENT_URI, PATH_RULES);

        public static final String RULE_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RULES;
        public static final String RULE_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RULES;


        public static final String TABLE_RULES = "rules";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_RULE_NL = "rule_nl";
        public static final String COLUMN_RULE_PL = "rule_pl";


    }

    public static final class SymbolEntry implements BaseColumns{
        public static final Uri SYMBOLS_URI =
                Uri.withAppendedPath(BASE_CONTENT_URI, PATH_SYMBOLS);

        public static final String SYMBOL_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_SYMBOLS;
        public static final String SYMBOL_ITEM_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_SYMBOLS;

        // symbols table
        public static final String TABLE_SYMBOLS = "symbols";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_PROPOSITION = "proposition";
    }

    public static final class RuleSymbolEntry implements BaseColumns{
        // Rule-symbols table uri
        public static final Uri RULE_SYMBOL_URI =
                Uri.withAppendedPath(BASE_CONTENT_URI, PATH_RULES_SYMBOLS);

        public static final String RULE_SYMBOL_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/"
                        + PATH_RULES_SYMBOLS;

        // rules_symbols table
        public static final String TABLE_RULES_SYMBOLS = "rules_symbols";

        public static final String _ID = BaseColumns._ID;
        public static final String KEY_RULE_ID = "rule_id";
        public static final String KEY_SYMBOL_ID = "symbol_id";
    }
}
