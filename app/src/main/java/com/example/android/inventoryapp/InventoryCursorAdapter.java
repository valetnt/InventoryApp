package com.example.android.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = InventoryCursorAdapter.class.getSimpleName();
    private TextView mQuantityTextView;

    /**
     * Constructs a new {@link InventoryCursorAdapter}
     *
     * @param context The context.
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context App context.
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to.
     * @return the newly created list item view
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method.
     * @param context App context.
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView nameTextView = (TextView) view.findViewById(R.id.item_name);
        TextView productCodeTextView = (TextView) view.findViewById(R.id.item_product_code);
        TextView priceTextView = (TextView) view.findViewById(R.id.item_price);
        mQuantityTextView = (TextView) view.findViewById(R.id.item_quantity_in_stock);

        View currencySymbol = view.findViewById(R.id.currency_symbol);

        String currentName = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_NAME));
        String currentProductCode = cursor.getString(cursor.getColumnIndex(
                InventoryEntry.COLUMN_CODE));
        String currentQuantity = String.valueOf(cursor.getInt(cursor.getColumnIndex(
                InventoryEntry.COLUMN_QUANTITY)));

        nameTextView.setText(currentName);
        productCodeTextView.setText(currentProductCode);
        mQuantityTextView.setText(currentQuantity);

        String currentPrice = "";
        int dataTypeOfCurrentPrice = cursor.getType(cursor.getColumnIndex(
                InventoryEntry.COLUMN_PRICE));
        if (dataTypeOfCurrentPrice == Cursor.FIELD_TYPE_INTEGER) {
            // If field is not null, convert the price in cents (integer) into
            // a price in dollars (decimal)
            double priceInDollars = cursor.getInt(cursor.getColumnIndex(
                    InventoryEntry.COLUMN_PRICE)) / 100.;
            currentPrice = String.valueOf(priceInDollars);
            currencySymbol.setVisibility(View.VISIBLE);

        } else if (dataTypeOfCurrentPrice == Cursor.FIELD_TYPE_NULL) {
            currentPrice = view.getContext().getString(R.string.unknown);
            currencySymbol.setVisibility(View.GONE);
        }
        priceTextView.setText(currentPrice);

        View buttonSell = view.findViewById(R.id.button_sell);
        buttonSell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(mQuantityTextView.getText().toString());
                if (quantity > 0) {
                    quantity--;
                    mQuantityTextView.setText(String.valueOf(quantity));

                    // Update item in database
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_QUANTITY, quantity);

                    ContentResolver contentResolver = v.getContext().getContentResolver();
                    Uri currentUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, v.getId());
                    int rowUpdated = contentResolver.update(currentUri, values, null, null);

                    // Show a toast message depending on whether or not the updating was successful
                    if (rowUpdated == 1) {
                        Toast.makeText(v.getContext(),
                                v.getContext().getString(R.string.update_successful),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(v.getContext(),
                                v.getContext().getString(R.string.update_failed),
                                Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }
}
