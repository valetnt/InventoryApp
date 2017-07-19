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
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_CODE = "product_code";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_QUANTITY = "items_already_in_stock";
        public static final String COLUMN_IMPENDING_ORDERS = "items_from_previous_orders";
        public static final String COLUMN_PICTURE_URI = "picture_uri";
        public static final String COLUMN_SUPPLIER_NAME = "supplier_name";
        public static final String COLUMN_SUPPLIER_MAIL = "supplier_mail";

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
