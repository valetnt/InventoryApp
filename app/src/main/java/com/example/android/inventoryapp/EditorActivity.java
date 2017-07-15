package com.example.android.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EDITOR_LOADER = 1;

    private EditText mNameEditText;
    private EditText mCodeEditText;
    private EditText mPriceEditText;
    private TextView mQuantityText;
    private EditText mSupplierEditText;
    private EditText mSupplierEMailEditText;
    private EditText mImpendingOrdersText;

    // Uri of the item we want to edit in edit mode
    private Uri mCurrentUri;

    /**
     * Implementation of the {@link LoaderManager.LoaderCallbacks} interface.
     * The cursor loader returns the results of a database query for a specific row.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Retrieve all fields for the current item
        String[] projection = new String[]{InventoryEntry._ID, InventoryEntry.COLUMN_NAME,
                InventoryEntry.COLUMN_CODE, InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_QUANTITY, InventoryEntry.COLUMN_PICTURE,
                InventoryEntry.COLUMN_SUPPLIER_NAME, InventoryEntry.COLUMN_SUPPLIER_MAIL,
                InventoryEntry.COLUMN_IMPENDING_ORDERS};

        // Query database for the Uri mCurrentUri
        return new CursorLoader(this, mCurrentUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (data == null || data.getCount() < 1) {
            return;
        }

        data.moveToFirst();
        String name = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_NAME));
        String code = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_CODE));
        int price = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRICE));
        String stringPrice = String.valueOf(price);
        int quantity = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_QUANTITY));
        String stringQuantity = String.valueOf(quantity);
        String supplier = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_NAME));
        String email = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_MAIL));
        int impendingOrders = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_IMPENDING_ORDERS));
        String stringImpendingOrders = String.valueOf(impendingOrders);

        mNameEditText.setText(name);
        mCodeEditText.setText(code);
        if (TextUtils.isEmpty(stringPrice)) {
            mPriceEditText.setText(getString(R.string.unknown));
        } else {
            mPriceEditText.setText(stringPrice);
        }
        mQuantityText.setText(stringQuantity);
        if (TextUtils.isEmpty(supplier)) {
            mSupplierEditText.setText(getString(R.string.unknown));
        } else {
            mSupplierEditText.setText(supplier);
        }
        if (TextUtils.isEmpty(email)) {
            mSupplierEMailEditText.setText(getString(R.string.unknown));
        } else {
            mSupplierEMailEditText.setText(email);
        }
        if (TextUtils.isEmpty(stringImpendingOrders)) {
            mImpendingOrdersText.setText(getString(R.string.unknown));
        } else {
            mImpendingOrdersText.setText(stringImpendingOrders);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clear all data fields
        mNameEditText.setText("");
        mCodeEditText.setText("");
        mQuantityText.setText("");
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
        mSupplierEMailEditText.setText("");
        mImpendingOrdersText.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        mNameEditText = (EditText) findViewById(R.id.item_name);
        mCodeEditText = (EditText) findViewById(R.id.item_product_code);
        mPriceEditText = (EditText) findViewById(R.id.item_price);
        mQuantityText = (TextView) findViewById(R.id.item_quantity_in_stock);
        mSupplierEditText = (EditText) findViewById(R.id.item_supplier_name);
        mSupplierEMailEditText = (EditText) findViewById(R.id.item_supplier_email);
        mImpendingOrdersText = (EditText) findViewById(R.id.number_of_items_ordered);

        FrameLayout buttonAddPhoto = (FrameLayout) findViewById(R.id.button_add_photo);
        FrameLayout buttonChangePhoto = (FrameLayout) findViewById(R.id.button_change_photo);
        FrameLayout buttonIncreaseQuantity = (FrameLayout) findViewById(R.id.button_increase_quantity);
        FrameLayout buttonDecreaseQuantity = (FrameLayout) findViewById(R.id.button_decrease_quantity);
        FrameLayout buttonOrder = (FrameLayout) findViewById(R.id.button_order);

        buttonIncreaseQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(String.valueOf(mQuantityText.getText()));
                quantity++;
                mQuantityText.setText(String.valueOf(quantity));
            }
        });

        buttonDecreaseQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(String.valueOf(mQuantityText.getText()));
                if (quantity > 0) {
                    quantity--;
                    mQuantityText.setText(String.valueOf(quantity));
                }
            }
        });

        mCurrentUri = getIntent().getData();
        if (mCurrentUri == null) {
            // We are in insert-new-item mode
            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_new_item));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete an item that hasn't been created yet.)
            invalidateOptionsMenu();

            mQuantityText.setText("0");
            buttonChangePhoto.setVisibility(View.GONE);
            buttonAddPhoto.setVisibility(View.VISIBLE);
            buttonOrder.setVisibility(View.GONE);

        } else {
            // We are in edit-existing-item mode
            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_edit_item));

            buttonChangePhoto.setVisibility(View.VISIBLE);
            buttonAddPhoto.setVisibility(View.GONE);
            buttonOrder.setVisibility(View.VISIBLE);

            // Retrieve item data via cursor loader
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(EDITOR_LOADER, null, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_save:
                saveItem();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveItem() {

        String item_name = mNameEditText.getText().toString().trim();
        String item_code = mCodeEditText.getText().toString().trim();
        int item_price = Integer.parseInt(mPriceEditText.getText().toString());
        String supplier_name = mSupplierEditText.getText().toString().trim();
        String supplier_email = mSupplierEMailEditText.getText().toString().trim();
        int quantity;
        int impending_orders;

        if (TextUtils.isEmpty(item_name)) {
            Toast.makeText(this, "Field NAME cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(item_code)) {
            Toast.makeText(this, "Field PRODUCT_CODE cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(mQuantityText.getText())) {
            Toast.makeText(this, "Field QUANTITY_IN_STOCK cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return;
        } else {
            quantity = Integer.parseInt(mQuantityText.getText().toString());
        }

        if (TextUtils.isEmpty(mImpendingOrdersText.getText())) {
            Toast.makeText(this, "Field IMPENDING_ORDERS cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return;
        } else {
            impending_orders = Integer.parseInt(mImpendingOrdersText.getText().toString());
        }

        ContentResolver contentResolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_NAME, item_name);
        values.put(InventoryEntry.COLUMN_CODE, item_code);
        values.put(InventoryEntry.COLUMN_PRICE, item_price);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_SUPPLIER_NAME, supplier_name);
        values.put(InventoryEntry.COLUMN_SUPPLIER_MAIL, supplier_email);
        values.put(InventoryEntry.COLUMN_IMPENDING_ORDERS, impending_orders);

        if (mCurrentUri == null) {

            // Save new item into the database
            Uri newUri = contentResolver.insert(InventoryEntry.CONTENT_URI, values);
            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.insert_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.insert_successful),
                        Toast.LENGTH_SHORT).show();
            }

        } else {

            // Update item
            int rowUpdated = contentResolver.update(mCurrentUri, values, null, null);
            // Show a toast message depending on whether or not the updating was successful
            if (rowUpdated == 1) {
                Toast.makeText(this, getString(R.string.update_successful),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.update_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
