package com.example.android.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "inventory.db";
    public static final int DATABASE_VERSION = 1;

    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
            + InventoryEntry.TABLE_NAME + " ( "
            + InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + InventoryEntry.COLUMN_NAME + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_CODE + " TEXT NOT NULL, "
            + InventoryEntry.COLUMN_PRICE + " INTEGER, "
            + InventoryEntry.COLUMN_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
            + InventoryEntry.COLUMN_PICTURE + " BLOB, "
            + InventoryEntry.COLUMN_SUPPLIER_NAME + " TEXT, "
            + InventoryEntry.COLUMN_SUPPLIER_MAIL + " TEXT, "
            + InventoryEntry.COLUMN_IMPENDING_ORDERS + " INTEGER NOT NULL DEFAULT 0);" ;

    public static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " +
            InventoryEntry.TABLE_NAME + ";";

    public InventoryDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
