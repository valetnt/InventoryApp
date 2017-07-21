package com.example.android.inventoryapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/*
 * This code contains a short excerpt adapted from
 * https://github.com/crlsndrsjmnz/MyShareImageExample/blob/master/app/src/main/java/co/carlosandresjimenez/android/myshareimageexample/MainActivity.java
 * about how to load a bitmap image from a given URI.
 */

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // Tag for the cursor loader used in edit-item mode to retrieve data related to the current item
    public static final int EDITOR_MODE_LOADER_ID = 1;

    // Tag for the cursor loader used in insert-new-item mode to check that the product code of the
    // new item is not a duplicate (if it is, the item must not be inserted into the database)
    public static final int INSERT_MODE_LOADER_ID = 2;

    // Tag for receiving an intent containing the URI of the image picked by the user
    private static final int PICK_IMAGE_REQUEST = 0;

    // Tag for printing Log messages
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();

    // Constant values for storing key-value pairs to save/restore instance states
    private static final String QUANTITY = "number of items in stock";
    private static final String IMAGE_URI = "URI of the image";
    private static final String IMAGE = "resized bitmap";
    private static final String BOOLEAN_IMAGE_INSERTED = "true if an image has been picked";
    private static final String BOOLEAN_DATA_CHANGED = "true if one of editable fields touched";

    private EditText mNameEditText;
    private EditText mCodeEditText;
    private EditText mPriceEditText;
    private TextView mQuantityText;
    private EditText mSupplierEditText;
    private EditText mSupplierEMailEditText;
    private EditText mImpendingOrdersText;
    private ImageView mPicture;
    private Uri mPictureUri;
    private Bitmap mBitmap;
    private EditText mQuantityEditText;

    private boolean mDataHasChanged = false;
    private boolean mImageHasBeenInserted = false; // variable used only in insert-new-item mode

    // Uri of the item we want to edit in edit-item mode
    private Uri mCurrentItemUri;

    /**
     * Implementation of OnTouchListener interface to detect changes of editable fields.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mDataHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        mNameEditText = (EditText) findViewById(R.id.item_name);
        mCodeEditText = (EditText) findViewById(R.id.item_product_code);
        mPriceEditText = (EditText) findViewById(R.id.item_price);
        mQuantityText = (TextView) findViewById(R.id.item_quantity_in_stock);
        mQuantityEditText = (EditText) findViewById(R.id.insert_mode_view);
        View quantityLinearLayout = findViewById(R.id.edit_mode_view);
        mSupplierEditText = (EditText) findViewById(R.id.item_supplier_name);
        mSupplierEMailEditText = (EditText) findViewById(R.id.item_supplier_email);
        mImpendingOrdersText = (EditText) findViewById(R.id.number_of_items_ordered);
        mPicture = (ImageView) findViewById(R.id.item_picture);
        mPictureUri = null;
        mBitmap = null;

        mPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Select an image from the file system
                openImageSelector();
            }
        });

        mNameEditText.setOnTouchListener(mTouchListener);
        mCodeEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mSupplierEMailEditText.setOnTouchListener(mTouchListener);
        mImpendingOrdersText.setOnTouchListener(mTouchListener);
        mPicture.setOnTouchListener(mTouchListener);

        View buttonIncreaseQuantity = findViewById(R.id.button_increase_quantity);
        View buttonDecreaseQuantity = findViewById(R.id.button_decrease_quantity);
        View buttonOrder = findViewById(R.id.button_order);

        buttonDecreaseQuantity.setOnTouchListener(mTouchListener);
        buttonIncreaseQuantity.setOnTouchListener(mTouchListener);

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

        // The EditorActivity is always launched from the CatalogActivity.
        // It can be launched either in INSERT-NEW-ITEM mode (if the user clicks the floating
        // action button to add a new item) or in EDIT-ITEM mode (if the user clicks one of the
        // catalog list items). In the former case, the intent does NOT contain any data, while
        // in the latter case the intent contains the content URI for the selected item.
        // To pull the data attached to the intent, call getIntent().getData()

        mCurrentItemUri = getIntent().getData();

        if (mCurrentItemUri == null) {
            // If we are in INSERT-NEW-ITEM mode

            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_new_item));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete an item that hasn't been created yet.)
            invalidateOptionsMenu();

            buttonOrder.setVisibility(View.GONE); // You cannot order an item that hasn't been
            // created yet.

            quantityLinearLayout.setVisibility(View.GONE);
            mQuantityEditText.setVisibility(View.VISIBLE);
            buttonDecreaseQuantity.setEnabled(false);
            buttonIncreaseQuantity.setEnabled(false);

        } else {
            // If we are in EDIT-ITEM mode

            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_edit_item));

            quantityLinearLayout.setVisibility(View.VISIBLE);
            mQuantityEditText.setVisibility(View.GONE);
            buttonDecreaseQuantity.setEnabled(true);
            buttonIncreaseQuantity.setEnabled(true);

            // Product code cannot be edited. If the user has inserted an item with
            // the wrong product code, he cannot correct it, but instead he has to
            // delete and then recreate the item.
            mCodeEditText.setEnabled(false);

            // Retrieve item data via cursor loader
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(EDITOR_MODE_LOADER_ID, null, this);

            buttonOrder.setVisibility(View.VISIBLE);
            buttonOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Open e-mail client to write an e-mail to the supplier
                    String supplierEmail = mSupplierEMailEditText.getText().toString();
                    Intent sendEmailToSupplier = new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:" + supplierEmail));
                    if (sendEmailToSupplier.resolveActivity(getPackageManager()) != null) {
                        startActivity(sendEmailToSupplier);
                    }
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BOOLEAN_DATA_CHANGED, mDataHasChanged);
        outState.putBoolean(BOOLEAN_IMAGE_INSERTED, mImageHasBeenInserted);

        if (mCurrentItemUri != null) {
            outState.putString(QUANTITY, mQuantityText.getText().toString());
        } else {
            outState.putString(QUANTITY, mQuantityEditText.getText().toString());
        }

        if (mPictureUri != null) {
            outState.putString(IMAGE_URI, mPictureUri.toString());
        }

        if (mBitmap != null) {
            Parcel parcel = Parcel.obtain();
            mBitmap.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            Bitmap destinationBitmap = Bitmap.CREATOR.createFromParcel(parcel);
            outState.putParcelable(IMAGE, destinationBitmap);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mDataHasChanged = savedInstanceState.getBoolean(BOOLEAN_DATA_CHANGED);

        if (mCurrentItemUri != null) {
            mQuantityText.setText(savedInstanceState.getString(QUANTITY));
        } else {
            mQuantityEditText.setText(savedInstanceState.getString(QUANTITY));
        }

        mImageHasBeenInserted = savedInstanceState.getBoolean(BOOLEAN_IMAGE_INSERTED);
        if (mCurrentItemUri == null) {
            if (!mImageHasBeenInserted) {
                mPicture.setImageResource(R.drawable.add_photo);
            } else {
                mPicture.setImageResource(R.drawable.no_image_available);
            }
        }

        if (savedInstanceState.containsKey(IMAGE_URI)) {
            mPictureUri = Uri.parse(savedInstanceState.getString(IMAGE_URI));
        } else {
            mPictureUri = null;
        }

        if (savedInstanceState.containsKey(IMAGE)) {
            mBitmap = savedInstanceState.getParcelable(IMAGE);
            mPicture.setImageBitmap(mBitmap);
        } else if (mCurrentItemUri != null) {
            mBitmap = null;
            mPicture.setImageResource(R.drawable.no_image_available);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {

                // The URI of the image picked by the user is attached to the intent "resultData".
                // Pull the URI by calling resultData.getData().
                Uri selectedImageUri = resultData.getData();

                // Retrieve the image associated with this URI and store it as a bitmap.
                try {
                    mBitmap = getBitmapFromUri(selectedImageUri);

                    // If we are in insert-mode, from this point onwards the UI will not
                    // prompt the user anymore to insert a picture. It will simply show
                    // the photo chosen by the user or a "no-image-available" view.
                    mImageHasBeenInserted = true;

                    if (mBitmap != null) {
                        mPictureUri = selectedImageUri;
                        // Update item picture in the UI
                        mPicture.setImageBitmap(mBitmap);

                    } else {
                        mPicture.setImageResource(R.drawable.no_image_available);
                        mPictureUri = null;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Problem retrieving the image. ", e);
                }
            }
        }
    }

    /**
     * Implementation of the {@link LoaderManager.LoaderCallbacks} interface.
     * The cursor loader returns the results of a database query for a specific row.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == EDITOR_MODE_LOADER_ID) {

            // Retrieve all fields for the current item
            String[] projection = new String[]{InventoryEntry._ID, InventoryEntry.COLUMN_NAME,
                    InventoryEntry.COLUMN_CODE, InventoryEntry.COLUMN_PRICE,
                    InventoryEntry.COLUMN_QUANTITY, InventoryEntry.COLUMN_PICTURE_URI,
                    InventoryEntry.COLUMN_SUPPLIER_NAME, InventoryEntry.COLUMN_SUPPLIER_MAIL,
                    InventoryEntry.COLUMN_IMPENDING_ORDERS};

            return new CursorLoader(this, mCurrentItemUri, projection, null, null, null);

        } else if (id == INSERT_MODE_LOADER_ID) {

            // Retrieve all product codes already present in the database
            String[] projection = new String[]{InventoryEntry.COLUMN_CODE};

            return new CursorLoader(this, InventoryEntry.CONTENT_URI, projection, null, null, null);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() == EDITOR_MODE_LOADER_ID) {

            // Bail early if the cursor is null or there is less than 1 row in the cursor
            if (data == null || data.getCount() < 1) {
                return;
            }

            data.moveToFirst();

            String name = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_NAME));
            String code = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_CODE));
            String stringPrice = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_PRICE));
            String stringQuantity = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_QUANTITY));
            String pictureUri = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_PICTURE_URI));
            String supplier = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_NAME));
            String email = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER_MAIL));
            String stringImpendingOrders =
                    data.getString(data.getColumnIndex(InventoryEntry.COLUMN_IMPENDING_ORDERS));

            mNameEditText.setText(name);
            mCodeEditText.setText(code);
            mPriceEditText.setText(stringPrice);
            mQuantityText.setText(stringQuantity);
            mSupplierEditText.setText(supplier);
            mSupplierEMailEditText.setText(email);
            mImpendingOrdersText.setText(stringImpendingOrders);

            mBitmap = null;

            mPictureUri = Uri.parse(pictureUri);
            try {
                mBitmap = getBitmapFromUri(mPictureUri);
                if (mBitmap != null) {
                    mPicture.setImageBitmap(mBitmap);

                } else {
                    mPicture.setImageResource(R.drawable.no_image_available);
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the image. ", e);
            }

        } else if (loader.getId() == INSERT_MODE_LOADER_ID) {

            if (data != null && data.getCount() > 0) {

                data.moveToFirst();
                while (!data.isAfterLast()) {
                    String code = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_CODE));
                    if ((code.toLowerCase()).equals
                            (mCodeEditText.getText().toString().trim().toLowerCase())) {
                        // If item is a duplicate, it must not be saved into the database!
                        // Break the loop and display a warning message to the user.
                        Toast.makeText(this, getString(R.string.duplicate_item),
                                Toast.LENGTH_SHORT).show();
                        break;
                    }
                    data.moveToNext();
                }
            }

            if (data == null || data.getCount() == 0
                    || (data.getCount() > 0 && data.isAfterLast())) {
                // If we have reached the last index without having encountered any duplicates
                // of the product code field, then the new item can be inserted into the database.
                insertNewItem();
            }
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
        mPicture.setImageBitmap(null);
        mPictureUri = null;
        mBitmap = null;

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
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                saveItem();
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the data hasn't changed, continue with navigating up to parent activity
                // which is the CatalogActivity.
                if (!mDataHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mDataHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the current item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the current item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the current item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Insert or update item according to whether we are in INSERT-NEW-ITEM or EDIT-ITEM mode.
     */
    private void saveItem() {

        if (mCurrentItemUri == null) { // If we are in INSERT-NEW-ITEM mode

            // Before saving the new item into the database, we must first check that its
            // product code has not been already used (i.e. that the item is not a duplicate).
            // Query database for product code via a cursor loader.
            // Perform this check and insert item in onLoadFinished()
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(INSERT_MODE_LOADER_ID, null, this);

        } else { // If we are in EDIT-ITEM mode
            updateItem();
        }
    }

    /**
     * Insert new item into the database.
     */
    private void insertNewItem() {
        /*
         * Create a ContentValues object by calling method checkContentValuesToSave().
         * If method returns null, it means that the ContentValues object is invalid, so
         * don't try to insert it into the database.
         */
        ContentValues values = checkContentValuesToSave();
        if (values != null) {
            // Insert new item into the database
            ContentResolver contentResolver = getContentResolver();
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

            // Terminate the activity
            finish();
            // Destroy the loader
            getSupportLoaderManager().destroyLoader(INSERT_MODE_LOADER_ID);
        }
    }

    /**
     * Update database at the URI of the current item.
     */
    private void updateItem() {
        /*
         * Create a ContentValues object by calling method checkContentValuesToSave().
         * If method returns null, it means that the ContentValues object is invalid, so
         * don't try to update the database.
         */
        ContentValues values = checkContentValuesToSave();
        if (values != null) {
            // Update item
            ContentResolver contentResolver = getContentResolver();
            int rowUpdated = contentResolver.update(mCurrentItemUri, values, null, null);

            // Show a toast message depending on whether or not the updating was successful
            if (rowUpdated == 1) {
                Toast.makeText(this, getString(R.string.update_successful),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.update_failed),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    /**
     * Helper method that checks that we are inserting or editing an item in compliance with
     * the table schema defined in {@link com.example.android.inventoryapp.data.InventoryDBHelper}.
     * If the constraints are complied with, this method returns a ContentValues object
     * that can be safely fed to the database without causing app to crash.
     * Else it returns null and warns the user that some fields need modifications.
     */
    private ContentValues checkContentValuesToSave() {

        String item_name = mNameEditText.getText().toString().trim();
        String item_code = mCodeEditText.getText().toString().trim();
        String item_price = mPriceEditText.getText().toString().trim();

        String quantity;
        if (mCurrentItemUri != null) {
            quantity = mQuantityText.getText().toString().trim();
        } else {
            quantity = mQuantityEditText.getText().toString().trim();
        }

        String supplier_name = mSupplierEditText.getText().toString().trim();
        String supplier_email = mSupplierEMailEditText.getText().toString().trim();
        String impending_orders = mImpendingOrdersText.getText().toString().trim();

        // If name field is empty, warn user and return early
        if (TextUtils.isEmpty(item_name)) {
            Toast.makeText(this, "Field NAME cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        // If product code field is empty, warn user and return early
        if (TextUtils.isEmpty(item_code)) {
            Toast.makeText(this, "Field PRODUCT_CODE cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        // If price field is empty, warn user and return early
        if (TextUtils.isEmpty(item_price)) {
            Toast.makeText(this, "Field PRICE cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        // If quantity field is empty, warn user and return early
        if (TextUtils.isEmpty(quantity)) {
            Toast.makeText(this, "Field QUANTITY_IN_STOCK cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        // If impending-orders field is empty, warn user and return early
        if (TextUtils.isEmpty(impending_orders)) {
            Toast.makeText(this, "Field IMPENDING_ORDERS cannot be empty!",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        // If no valid image has been found, warn the user and return early
        if (mPictureUri == null) {
            Toast.makeText(this, "Please, select an IMAGE for this product",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_NAME, item_name);
        values.put(InventoryEntry.COLUMN_CODE, item_code);
        values.put(InventoryEntry.COLUMN_PRICE, item_price);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_SUPPLIER_NAME, supplier_name);
        values.put(InventoryEntry.COLUMN_SUPPLIER_MAIL, supplier_email);
        values.put(InventoryEntry.COLUMN_IMPENDING_ORDERS, impending_orders);
        values.put(InventoryEntry.COLUMN_PICTURE_URI, mPictureUri.toString());

        return values;
    }

    /*
     * Delete current item
     */
    private void deleteItem() {

        if (mCurrentItemUri != null) { // If we are in EDIT-ITEM mode

            ContentResolver contentResolver = getContentResolver();
            int rowDeleted = contentResolver.delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not deleting was successful
            if (rowDeleted == 1) {
                Toast.makeText(this, getString(R.string.delete_successful),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.delete_failed),
                        Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }

    /*
     * Open image selector to allow the user to pick a picture for the item
     */
    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    /*
     * Helper method to load a bitmap image from a given URI.
     * Contains an excerpt from
     * https://github.com/crlsndrsjmnz/MyShareImageExample/blob/master/app/src/main/java/co/carlosandresjimenez/android/myshareimageexample/MainActivity.java
     * about how to load a bitmap image from a given URI.
     */
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {

        Bitmap bitmap = null;

        if (uri == null || uri.toString().isEmpty())
            return null;

        int targetSize = (int) getResources().getDimension(R.dimen.image_size);

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);
            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetSize, photoH / targetSize);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            input = this.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(input, null, bmOptions);

        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File not found. ", e);

        } finally {
            if (input != null) {
                input.close();
            }
        }

        return bitmap;
    }
}
