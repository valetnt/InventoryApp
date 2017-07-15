package com.example.android.inventoryapp;

import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

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
    private TextView mImpendingOrdersText;

    // Uri of the item we want to edit in edit mode
    private Uri mCurrentUri;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Retrieve all fields for the current item
        String[] projection = new String[]{InventoryEntry._ID, InventoryEntry.COLUMN_NAME,
                InventoryEntry.COLUMN_CODE, InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_QUANTITY, InventoryEntry.COLUMN_PICTURE,
                InventoryEntry.COLUMN_SUPPLIER_NAME, InventoryEntry.COLUMN_SUPPLIER_MAIL,
                InventoryEntry.COLUMN_IMPENDING_ORDERS};

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
        mImpendingOrdersText = (TextView) findViewById(R.id.number_of_items_ordered);

        mCurrentUri = getIntent().getData();
        if (mCurrentUri == null) {
            // We are in insert-new-item mode
            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_new_item));

            mQuantityText.setText("0");
            mImpendingOrdersText.setText("0");

        } else {
            // We are in edit mode
            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_edit_item));

            // Retrieve item data via cursor loader
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(EDITOR_LOADER, null, this);
        }
    }
}
