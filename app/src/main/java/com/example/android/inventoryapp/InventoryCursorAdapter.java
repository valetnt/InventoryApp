package com.example.android.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

        int currentRowId = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));
        final Uri currentUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, currentRowId);

        TextView nameTextView = (TextView) view.findViewById(R.id.item_name);
        TextView productCodeTextView = (TextView) view.findViewById(R.id.item_product_code);
        TextView priceTextView = (TextView) view.findViewById(R.id.item_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.item_quantity_in_stock);

        TextView currencySymbol = (TextView) view.findViewById(R.id.currency_symbol);

        final String currentName = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_NAME));
        String currentProductCode = cursor.getString(cursor.getColumnIndex(
                InventoryEntry.COLUMN_CODE));
        String currentQuantity = String.valueOf(cursor.getInt(cursor.getColumnIndex(
                InventoryEntry.COLUMN_QUANTITY)));

        nameTextView.setText(currentName);
        productCodeTextView.setText(currentProductCode);
        quantityTextView.setText(currentQuantity);

        String currentPrice = view.getContext().getString(R.string.unknown);
        currencySymbol.setVisibility(View.GONE);
        int dataTypeOfCurrentPrice = cursor.getType(cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE));
        if (dataTypeOfCurrentPrice == Cursor.FIELD_TYPE_INTEGER) {
            // If field is not null, convert the price in cents (integer) into
            // a price in dollars (decimal)
            double priceInDollars = cursor.getInt(cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE))
                    / 100.;
            currentPrice = String.valueOf(priceInDollars);
            currencySymbol.setVisibility(View.VISIBLE);
        }
        priceTextView.setText(currentPrice);

        View buttonSell = view.findViewById(R.id.button_sell);
        View buttonEdit = view.findViewById(R.id.button_edit);


        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openEditor = new Intent(v.getContext(), EditorActivity.class);
                openEditor.setData(currentUri);
                v.getContext().startActivity(openEditor);
            }
        });


        buttonSell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = Integer.parseInt(quantityTextView.getText().toString());
                if (quantity > 0) {
                    quantity--;
                    quantityTextView.setText(String.valueOf(quantity));

                    // Update item in database
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
                    ContentResolver contentResolver = v.getContext().getContentResolver();
                    int rowUpdated = contentResolver.update(currentUri, values, null, null);

                    // Show a toast message depending on whether or not the updating was successful
                    if (rowUpdated == 1) {
                        Toast.makeText(v.getContext(),
                                v.getContext().getString(R.string.label_item_sold_part1)
                                        + " " + currentName + " " +
                                        v.getContext().getString(R.string.label_item_sold_part2),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(v.getContext(),
                                v.getContext().getString(R.string.label_item_selling_failed),
                                Toast.LENGTH_SHORT).show();
                    }

                }
            }

        });
    }
}
