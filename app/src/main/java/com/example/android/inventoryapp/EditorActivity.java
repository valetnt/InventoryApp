package com.example.android.inventoryapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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

import static android.R.attr.name;

/*
 * This code contains a short excerpt adapted from
 * https://github.com/crlsndrsjmnz/MyShareImageExample/blob/master/app/src/main/java/co/carlosandresjimenez/android/myshareimageexample/MainActivity.java
 * about how to load a bitmap image from a given URI.
 */

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EDITOR_MODE_LOADER = 1;
    public static final int INSERT_MODE_LOADER = 2;
    private static final int PICK_IMAGE_REQUEST = 0;
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();
    /*
    // Constant values for storing key-value pairs to support device rotation
    private static final String QUANTITY = "number of items in stock";
    private static final String BOOLEAN_IMAGE_CHANGED = "true only if image has changed";
    private static final String BOOLEAN_IMAGE_DELETED = "true only if image has been deleted";
    private static final String BOOLEAN_DATA_CHANGED = "true only if UI changes detected";
    */
    private EditText mNameEditText;
    private EditText mCodeEditText;
    private EditText mPriceEditText;
    private TextView mQuantityText;
    private EditText mSupplierEditText;
    private EditText mSupplierEMailEditText;
    private EditText mImpendingOrdersText;
    private ImageView mPicture;
    private Uri mPictureUri;
    private boolean mImageHasChanged = false;
    private boolean mImageHasBeenDeleted = false;
    private boolean mDataHasChanged = false;
    // Uri of the item we want to edit in edit mode
    private Uri mCurrentUri;
    //private static final String WIDTH = "width of the ImageView";
    //private static final String HEIGHT = "height of the ImageView";
    /**
     * Implementation of OnTouchListener interface to detect changes of editable fields
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mDataHasChanged = true;
            return false;
        }
    };

    /**
     * Implementation of the {@link LoaderManager.LoaderCallbacks} interface.
     * The cursor loader returns the results of a database query for a specific row.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == EDITOR_MODE_LOADER) {

            // Retrieve all fields for the current item
            String[] projection = new String[]{InventoryEntry._ID, InventoryEntry.COLUMN_NAME,
                    InventoryEntry.COLUMN_CODE, InventoryEntry.COLUMN_PRICE,
                    InventoryEntry.COLUMN_QUANTITY, InventoryEntry.COLUMN_PICTURE_URI,
                    InventoryEntry.COLUMN_SUPPLIER_NAME, InventoryEntry.COLUMN_SUPPLIER_MAIL,
                    InventoryEntry.COLUMN_IMPENDING_ORDERS};

            return new CursorLoader(this, mCurrentUri, projection, null, null, null);

        } else if (id == INSERT_MODE_LOADER) {

            // Retrieve all product codes already present in the database
            String[] projection = new String[]{InventoryEntry.COLUMN_CODE};

            return new CursorLoader(this, InventoryEntry.CONTENT_URI, projection, null, null, null);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() == EDITOR_MODE_LOADER) {

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

            if (pictureUri != null) {
                try {
                    Bitmap bitmap = getBitmapFromUri(Uri.parse(pictureUri));
                    if (bitmap == null) {
                        mPicture.setImageResource(R.drawable.no_image_available);
                    } else {
                        mPicture.setImageBitmap(bitmap);

                        mPicture.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                                builder.setMessage(R.string.delete_image_dialog_msg);
                                builder.setPositiveButton(R.string.confirm_image_deletion, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked the "Delete" button, so delete the image.
                                        // Update database immediately.
                                        mImageHasChanged = true;
                                        mImageHasBeenDeleted = true;
                                        ContentResolver contentResolver = getContentResolver();
                                        ContentValues values = new ContentValues();
                                        values.put(InventoryEntry.COLUMN_PICTURE_URI, "");
                                        contentResolver.update(mCurrentUri, values, null, null);
                                    }
                                });
                                builder.setNegativeButton(R.string.cancel_image_deletion, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User clicked the "Cancel" button, so dismiss the dialog.
                                        if (dialog != null) {
                                            dialog.dismiss();
                                        }
                                    }
                                });

                                // Create and show the AlertDialog
                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();
                                return true;
                            }
                        });
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Problem retrieving the image. ", e);
                }
            } else {
                mPicture.setImageResource(R.drawable.no_image_available);
            }

        } else if (loader.getId() == INSERT_MODE_LOADER) {

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
                    getSupportLoaderManager().destroyLoader(INSERT_MODE_LOADER);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == EDITOR_MODE_LOADER) {

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
        }
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
        mPicture = (ImageView) findViewById(R.id.item_picture);

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

        mCurrentUri = getIntent().getData();
        if (mCurrentUri == null) {
            // If we are in insert-new-item mode
            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_new_item));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete an item that hasn't been created yet.)
            invalidateOptionsMenu();

            mQuantityText.setText("0");
            buttonOrder.setVisibility(View.GONE);

        } else {
            // If we are in edit-existing-item mode
            getSupportActionBar().setTitle(getString(R.string.editor_activity_title_edit_item));

            // Product code cannot be edited. If the user has inserted an item with
            // the wrong product code, he cannot correct it, but instead he has to
            // delete and then recreate the item.
            mCodeEditText.setEnabled(false);

            // Retrieve item data via cursor loader
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(EDITOR_MODE_LOADER, null, this);

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
        /*
        outState.putInt(QUANTITY, Integer.parseInt(mQuantityText.getText().toString()));
        outState.putBoolean(BOOLEAN_DATA_CHANGED, mDataHasChanged);
        outState.putBoolean(BOOLEAN_IMAGE_CHANGED, mImageHasChanged);
        outState.putBoolean(BOOLEAN_IMAGE_DELETED, mImageHasBeenDeleted);
        */
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        /*
        mQuantityText.setText(String.valueOf(savedInstanceState.getInt(QUANTITY)));
        mDataHasChanged = savedInstanceState.getBoolean(BOOLEAN_DATA_CHANGED);
        mImageHasChanged = savedInstanceState.getBoolean(BOOLEAN_IMAGE_CHANGED);
        mImageHasBeenDeleted = savedInstanceState.getBoolean(BOOLEAN_IMAGE_DELETED);
        */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mPictureUri = resultData.getData();

                // Update item picture in the UI.
                // (Database will only be updated on clicking "Save" menu option)
                try {
                    Bitmap bitmap = getBitmapFromUri(mPictureUri);
                    if (bitmap != null) {
                        mPicture.setImageBitmap(bitmap);
                        mImageHasChanged = true;
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Problem retrieving the image. ", e);
                }
            }
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
                // which is the {@link CatalogActivity}.
                if (!mDataHasChanged && !mImageHasChanged) {
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
        // If the pet hasn't changed, continue with handling back button press
        if (!mDataHasChanged && !mImageHasChanged) {
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

    private void saveItem() {

        if (mCurrentUri == null) { // If we are in insert-new-item mode

            // Before saving the new item into the database, we must first check that its
            // product code has not been already used (i.e. that the item is not a duplicate).
            // Query database for product code via a cursor loader.
            // Perform this check in onLoadFinished()
            LoaderManager loaderManager = getSupportLoaderManager();
            loaderManager.initLoader(INSERT_MODE_LOADER, null, this);

        } else { // If we are in edit-existing-item mode

            ContentValues values = checkContentValuesToSave();
            if (values != null) {

                // Update item
                ContentResolver contentResolver = getContentResolver();
                int rowUpdated = contentResolver.update(mCurrentUri, values, null, null);

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
    }

    private ContentValues checkContentValuesToSave() {

        String item_name = mNameEditText.getText().toString().trim();
        String item_code = mCodeEditText.getText().toString().trim();
        String item_price = mPriceEditText.getText().toString().trim();
        String quantity = mQuantityText.getText().toString().trim();
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

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_NAME, item_name);
        values.put(InventoryEntry.COLUMN_CODE, item_code);
        values.put(InventoryEntry.COLUMN_PRICE, item_price);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_SUPPLIER_NAME, supplier_name);
        values.put(InventoryEntry.COLUMN_SUPPLIER_MAIL, supplier_email);
        values.put(InventoryEntry.COLUMN_IMPENDING_ORDERS, impending_orders);

        if (mPictureUri != null) {
            String pictureUri = mPictureUri.toString();
            values.put(InventoryEntry.COLUMN_PICTURE_URI, pictureUri);

        } else if (mImageHasBeenDeleted) {
            // It makes sense to update the database by setting the image to "no image"
            // ONLY IF the user has explicitly chosen to delete the image.
            values.put(InventoryEntry.COLUMN_PICTURE_URI, "");
        }

        return values;
    }

    private void deleteItem() {

        if (mCurrentUri != null) { // If we are in editor mode

            ContentResolver contentResolver = getContentResolver();
            int rowDeleted = contentResolver.delete(mCurrentUri, null, null);

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


    // Helper method to load a bitmap image from a given URI
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {

        Bitmap bitmap = null;

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the ImageView
        // This will be useful to properly resize the bitmap image
        int width = mPicture.getWidth();
        int height = mPicture.getHeight();

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
            int scaleFactor = Math.min(photoW / width, photoH / height);

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
}
