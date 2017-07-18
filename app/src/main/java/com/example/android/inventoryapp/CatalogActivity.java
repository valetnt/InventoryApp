package com.example.android.inventoryapp;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class CatalogActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int CATALOG_LOADER_ID = 0;
    private static final String LOG_TAG = CatalogActivity.class.getSimpleName();
    private InventoryCursorAdapter mCursorAdapter;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /*
         * Create a CursorLoader that queries inventory database for:
         * item _id, name, picture, product code, price and quantity in stock
         */
        String[] projection = new String[]{InventoryEntry._ID, InventoryEntry.COLUMN_NAME,
                InventoryEntry.COLUMN_PICTURE_URI, InventoryEntry.COLUMN_CODE,
                InventoryEntry.COLUMN_PRICE, InventoryEntry.COLUMN_QUANTITY};

        return new CursorLoader(this, InventoryEntry.CONTENT_URI, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update cursor adapter with the results of the query
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Reset cursor adapter
        mCursorAdapter.swapCursor(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        ListView listView = (ListView) findViewById(R.id.list);
        View emptyView = findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        mCursorAdapter = new InventoryCursorAdapter(this, null);
        // Set an instance of PetCursorAdapter onto the ListView
        listView.setAdapter(mCursorAdapter);

        // Initialize the cursor loader
        LoaderManager loaderManager = getSupportLoaderManager();
        loaderManager.initLoader(CATALOG_LOADER_ID, null, this);
    }
}
