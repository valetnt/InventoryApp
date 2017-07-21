package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import static com.example.android.inventoryapp.R.id.impending_orders;

public class InventoryProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /*
     * URI matcher code for the inventory table
     */
    public static final int INVENTORY = 100;

    /*
     * URI matcher code for a single inventory item
     */
    public static final int INVENTORY_ID = 101;

    /**
     * {@link UriMatcher} object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY,
                INVENTORY);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY +
                "/#", INVENTORY_ID);
    }

    /**
     * Database helper object
     */
    private InventoryDBHelper mDbHelper;

    @Override
    public boolean onCreate() {

        mDbHelper = new InventoryDBHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI.
     * Use the given projection, selection, selection arguments, and sort order.
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        // The database query is performed according to whether we want to address the whole
        // inventory table or just a specific row.
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // Cursor can contain multiple rows of the table
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;

            case INVENTORY_ID:

                // For the INVENTORY_ID code, extract out the ID from the URI.
                selection = InventoryEntry._ID + " =? ";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // Cursor contains only one row of the table: the one associated to
                // the id appended to the content URI
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set the cursor to be automatically updated whenever the data associated with
        // this URI is updated. If the uri path addresses the whole inventory table, then
        // the cursor is updated whenever a new entry is created or
        // an already existing row is updated/deleted.
        // Updating the cursor triggers the cursor adapter (and thus the UI) to update as well.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Insert new data with the given ContentValues.
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        // Inserting a new entry into the table requires that the URI matches code INVENTORY,
        // i.e. that we are addressing the WHOLE inventory table.
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // If the UriMatcher recognizes the URI as valid, then create
                // a new database entry with the specified content values
                return insertItem(uri, values);

            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a new item into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertItem(Uri uri, ContentValues values) {

        /*
         * Data Validation
         */

        // Check that name is not null
        String name = values.getAsString(InventoryEntry.COLUMN_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Item requires a name");
        }

        // Check that product code is not null
        String product_code = values.getAsString(InventoryEntry.COLUMN_CODE);
        if (product_code == null) {
            throw new IllegalArgumentException("Item requires a product code");
        }

        //Check that price is not null and positive
        Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Item requires valid price");
        }

        // Check that quantity in stock is not null and positive
        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Item requires valid quantity");
        }

        // Check that product has a picture
        String picture_uri = values.getAsString(InventoryEntry.COLUMN_PICTURE_URI);
        if (picture_uri == null) {
            throw new IllegalArgumentException("Item requires an image");
        }

        // Check that number of impending orders is not null and positive
        Integer impending_orders = values.getAsInteger(InventoryEntry.COLUMN_IMPENDING_ORDERS);
        if (impending_orders == null || impending_orders < 0) {
            throw new IllegalArgumentException("Number of impending orders is invalid");
        }

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        //Insert new entry into our database
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);

        if (id == -1) {
            // Insertion unsuccessful, return early
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all the listeners that data at this uri has changed
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateItem(uri, values, selection, selectionArgs);

            case INVENTORY_ID:

                // Update only the row associated to the specific id appended to the content URI
                selection = InventoryEntry._ID + " =? ";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateItem(uri, values, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update items in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments.
     * Return the number of rows that were successfully updated.
     */
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Data Validation
        // If name is updated, new value must be not null
        if (values.containsKey(InventoryEntry.COLUMN_NAME)) {
            String name = values.getAsString(InventoryEntry.COLUMN_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        // If product code is updated, new value must be not null
        if (values.containsKey(InventoryEntry.COLUMN_CODE)) {
            String product_code = values.getAsString(InventoryEntry.COLUMN_CODE);
            if (product_code == null) {
                throw new IllegalArgumentException("Item requires a product code");
            }
        }

        // If price is updated, new value must be not null and positive
        if (values.containsKey(InventoryEntry.COLUMN_PRICE)) {
            Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Item requires valid price");
            }
        }

        // If quantity in stock is updated, new value must be not null and positive
        if (values.containsKey(InventoryEntry.COLUMN_QUANTITY)) {
            Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_QUANTITY);
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Item requires valid quantity");
            }
        }

        // If picture is updated, new value must be not null
        if (values.containsKey(InventoryEntry.COLUMN_PICTURE_URI)) {
            String picture_uri = values.getAsString(InventoryEntry.COLUMN_PICTURE_URI);
            if (picture_uri == null) {
                throw new IllegalArgumentException("Item requires an image");
            }
        }

        // If number of impending orders is updated, new value must be not null and positive
        if (values.containsKey(InventoryEntry.COLUMN_IMPENDING_ORDERS)) {
            Integer impending_orders = values.getAsInteger(InventoryEntry.COLUMN_IMPENDING_ORDERS);
            if (impending_orders == null || impending_orders < 0) {
                throw new IllegalArgumentException("Number of impending orders is invalid");
            }
        }

        int rowsUpdated = 0;

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return rowsUpdated;
        }

        // Update database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        rowsUpdated = database.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated > 0) {
            // Notify all the listeners that data at this uri has changed
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }


    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        int rowsDeleted = 0;

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case INVENTORY_ID:
                // Delete only the row associated to the specific id appended to the content URI
                selection = InventoryEntry._ID + " =? ";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted > 0) {
            // Notify all the listeners that data at this uri has changed
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;

            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
