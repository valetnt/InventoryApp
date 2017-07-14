package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class InventoryContract {

    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryapp";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_INVENTORY = "inventory";

    // Constructor
    public InventoryContract() {}

    public static class InventoryEntry implements BaseColumns {

        // Table name
        public static final String TABLE_NAME = "inventory";

        // Columns
        public static final String COLUMN_ID = BaseColumns._ID;
        public static final String COLUMN_NAME = "item name";
        public static final String COLUMN_CODE = "product code";
        public static final String COLUMN_PRICE = "item price";
        public static final String COLUMN_QUANTITY = "quantity in stock";
        public static final String COLUMN_PICTURE = "picture of the item";
        public static final String COLUMN_SUPPLIER_NAME = "name of the item supplier";
        public static final String COLUMN_SUPPLIER_MAIL = "e-mail of the supplier";
        public static final String COLUMN_IMPENDING_ORDERS = "quantity ordered from the supplier";

        // Full URI for this table
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);

        // MIME type for a list of inventory items
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        // MIME type for a single inventory item
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
                + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;
    }
}
