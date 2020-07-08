package mt.edu.um.malteseaacapp.imagegrid.gridsetup;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

public class AdapterGridLayout extends GridLayout
{
    // Declare GridAdapter
    private GridAdapter gridAdapter;
    // Declare DataSetObserver in order to monitor any changes in the adapter
    private DataSetObserver dataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            loadContent();
        }
    };


    // Define constructors (They call constructors from parent class)
    // This is necessary since there is no default constructor in the parent class
    public AdapterGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public AdapterGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public AdapterGridLayout(Context context) {
        super(context);
    }

    /**
     * Set the adapter to the GridLayout by passing it as an argument,
     * and register the data set observer
     * @param gridAdapter   The grid adapter to be assigned to the grid layout
     */
    public void setAdapter(GridAdapter gridAdapter) {
        if (this.gridAdapter == gridAdapter) return;
        this.gridAdapter = gridAdapter;
        if (gridAdapter != null) gridAdapter.registerDataSetObserver(dataSetObserver);
        loadContent();
    }

    /**
     * Obtains the views from the GridAdapter and loads them into the GridLayout
     */
    private void loadContent() {
        // Remove all views existing in the GridLayout
        removeAllViews();
        // If GridAdapter is not empty, go through all views
        // in the GridAdapter and add them to the GridLayout
        if (gridAdapter != null) {
            for (int i = 0; i < gridAdapter.getCount(); i++) {
                // Set parameters for the ImageView (to fit evenly) and add to the grid layout
                GridLayout.LayoutParams lp = new LayoutParams(GridLayout.spec(GridLayout.UNDEFINED,
                        1f),GridLayout.spec(GridLayout.UNDEFINED, 1f));
                lp.setGravity(Gravity.CENTER);
                View view = gridAdapter.getView(i, null, this);
                if (view != null)
                    addView(view, lp);
            }
            requestLayout();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Unregister the DataSetObserver from the GridAdapter
        if (gridAdapter != null) gridAdapter.unregisterDataSetObserver(dataSetObserver);
    }
}
