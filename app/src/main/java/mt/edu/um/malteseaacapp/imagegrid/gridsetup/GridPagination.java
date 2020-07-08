package mt.edu.um.malteseaacapp.imagegrid.gridsetup;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import mt.edu.um.malteseaacapp.ImageObject;

public class GridPagination {

    // Cursor containing imageObjects obtained from database
    private Cursor mCursor;

    // Declare Page Related variables
    private int mItemsPerPage;
    private int mItemsRemaining;
    private int mLastPage;

    // A complete collection of grid items on which to return views
    private List<ImageObject> mItems;

    /**
     * Constructor of the GridPagination class. Assign and calculate page related variables.
     * @param context The application context
     * @param cursor Cursor containing ImageObject details obtained from database
     * @param itemsPerPage The number of images to display per page
     */
    public GridPagination(Context context, Cursor cursor, int itemsPerPage) {
        mItems = new ArrayList<>();

        // Load items from the database cursor into the buffer
        loadItems(context, cursor);

        mItemsPerPage = itemsPerPage;
        mItemsRemaining = mItems.size() % mItemsPerPage;

        // If there is no remainder, there is no need for an extra page
        if (mItemsRemaining == 0) {
            // Recall that page numbers start from 0
            mLastPage = (mItems.size() / mItemsPerPage) - 1;

        } // If some buttons remain, an extra page is required to fit the remaining buttons
        else {
            // Recall that page numbers start from 0
            mLastPage = mItems.size() / mItemsPerPage;
        }

        mCursor = cursor;
    }

    /**
     * Returns the page number of the last page (page nos. start from 0)
     * @return Returns the index of the last page
     */
    public int getLastPage() {
        return mLastPage;
    }

    public int getCount() {
        return mItems.size();
    }

    /**
     * Generates an array list which contains the contents to be displayed on a new page
     * @param   page   The page number of the current page
     * @return  Returns an array list with the contents for the new grid page
     */
    public List<ImageObject> newPage (int page) {
        int start = page * mItemsPerPage;
        int count = (page == mLastPage && mItemsRemaining > 0) ? mItemsRemaining : mItemsPerPage;

        return mItems.subList(start, start + count);
    }

    /**
     * Close cursor
     */
    public void close() {
        if (mCursor != null)
            mCursor.close();
    }

    private void loadItems(Context context, Cursor cursor) {
        for(int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);

            int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            String word = cursor.getString(cursor.getColumnIndexOrThrow("Word"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("Category"));
            boolean hidden = cursor.getInt(cursor.getColumnIndexOrThrow("IsHidden")) == 1;

            mItems.add(new ImageObject(context, id, word, category, hidden));
        }
    }
}
