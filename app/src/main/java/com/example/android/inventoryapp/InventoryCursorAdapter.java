package com.example.android.inventoryapp;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryCursorAdapter extends CursorAdapter {

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
        TextView quantityTextView = (TextView) view.findViewById(R.id.item_quantity_in_stock);

        String currentName = cursor.getString(cursor.getColumnIndex(InventoryEntry.COLUMN_NAME));
        String currentProductCode = cursor.getString(cursor.getColumnIndex(
                InventoryEntry.COLUMN_CODE));
        String currentQuantity = String.valueOf(cursor.getInt(cursor.getColumnIndex(
                InventoryEntry.COLUMN_QUANTITY)));

        nameTextView.setText(currentName);
        productCodeTextView.setText(currentProductCode);
        quantityTextView.setText(currentQuantity);

        String currentPrice = view.getContext().getString(R.string.unknown);
        int dataTypeOfCurrentPrice = cursor.getType(cursor.getColumnIndex(
                InventoryEntry.COLUMN_PRICE));
        if (dataTypeOfCurrentPrice == Cursor.FIELD_TYPE_INTEGER) {
            // If field is not null, convert the price in cents (integer) into
            // a price in dollars (decimal)
            double priceInDollars = cursor.getInt(cursor.getColumnIndex(
                    InventoryEntry.COLUMN_PRICE)) / 100;
            currentPrice = String.valueOf(priceInDollars);
        }
        priceTextView.setText(currentPrice);
    }
}
