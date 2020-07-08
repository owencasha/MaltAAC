package mt.edu.um.malteseaacapp.imagegrid.gridsetup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.graphics.ColorUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.util.List;

import mt.edu.um.malteseaacapp.ImageObject;
import mt.edu.um.malteseaacapp.R;

public class GridAdapter extends BaseAdapter {
    public interface GridItemListener {
        void onItemTouchUp(int index, MotionEvent motionEvent);
        void onItemLongPress(int index);
        void onItemClick(int index);
    }

    private Context mContext;

    // Declare a list to hold the drawables for the image buttons
    private List<ImageObject> mItems;

    // Declare variables to be used for setting the size of the ImageView items within the grid
    private int mRows;
    private int mColumns;
    private int mBackgroundColor;

    private boolean mShowHidden;
    private GridItemListener mListener;

    /**
     * Constructor - Creates the instance and sets the values of the adapter's variables
     * @param context       The context
     * @param columns       The number of grid columns
     * @param rows          The number of grid rows
     * @param backgroundCol The background color
     */
    public GridAdapter(Context context, int columns, int rows, int backgroundCol) {
        this.mContext = context;

        setColumns(columns);
        setRows(rows);

        mBackgroundColor = backgroundCol;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View getView(int i, View view, final ViewGroup viewGroup) {
        if (view == null) {
            // Inflate image button layout
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = layoutInflater.inflate(R.layout.image_button_layout, null);
        }

        // Item to bind
        final ImageObject item = mItems.get(i);

        // Find image and text views
        final ImageView imageButton = view.findViewById(R.id.imageButton);

        // Update image text with word corresponding to the image
        final TextView imageText = view.findViewById(R.id.imageText);

        imageText.setText(item.getWord());

        if (ColorUtils.calculateLuminance(mBackgroundColor) > 0.4) {
            imageText.setTextColor(view.getResources().getColor(R.color.black));
        } else {
            imageText.setTextColor(view.getResources().getColor(R.color.white));
        }

        // Set up events
        imageButton.setOnClickListener(view1 -> {
            if (mListener != null) {
                mListener.onItemClick(i);
            }
        });

        imageButton.setOnLongClickListener((View.OnLongClickListener) view12 -> {
            if (mListener != null) {
                mListener.onItemLongPress(i);
            }

            return true;
        });

        imageButton.setOnTouchListener((view13, motionEvent) -> {
            if (mListener != null && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                mListener.onItemTouchUp(i, motionEvent);
            }

            return false;
        });

        // Global layout listener is called when the layout state changes,
        // at which point the dimensions of the parent view will be available,
        // thus, the size of the images can be set to make them fit evenly.
        imageButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                // Remove global layout callback
                imageButton.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                // Calculate width and height of ImageView based on screen size and grid size
                // so as to fill the whole grid evenly. Multiply by 0.7f so as to use 70% of
                // the available space and make the images fit more neatly
                final float width = (int)(viewGroup.getWidth() / mColumns) * 0.7f;
                final float height = (int)(viewGroup.getHeight() / mRows) * 0.7f;

                // Set width and height for the image view
                if (width < height) {
                    imageButton.getLayoutParams().width = (int)width;
                    imageText.setWidth(imageButton.getLayoutParams().width = (int)width);

                    imageButton.getLayoutParams().height = (int)width;

                } else {
                    imageButton.getLayoutParams().width = (int)height;
                    imageText.setWidth(imageButton.getLayoutParams().width = (int)width);

                    imageButton.getLayoutParams().height = (int)height;
                }
            }
        });

        // Get image path from imageButtons array list and set as image resource using Glide
        // The signature is set so that when the path of the image changes, the new resource
        // is loaded as opposed to the image stored in Glide's cache
        String path = item.getImage();
        Glide.with(mContext)
                .load(path).apply(new RequestOptions().override(300, 300)
                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis()))))
                .into(imageButton)
                .clearOnDetach();

        // Set visibility
        if (item.isHidden()) {
            if (mShowHidden) {
                view.setAlpha(0.25f);
            } else {
                view.setVisibility(View.INVISIBLE);
                imageButton.setEnabled(false);
            }
        }

        return view;
    }

    @Override
    public CharSequence[] getAutofillOptions() {
        return new CharSequence[0];
    }

    /**
     * Set the image buttons array list equal to the array list passed as an argument
     * @param imageButtons  Array list containing drawables to be assigned to the image buttons
     */
    public void setGridList(List<ImageObject> imageButtons)
    {
        this.mItems = imageButtons;
    }

    /**
     * Sets the number of rows equal to the given parameter
     * @param rows The number of rows in the grid
     */
    private void setRows(int rows) {
        this.mRows = rows;
    }

    /**
     * Sets the number of columns equal to the given parameter
     * @param columns The number of columns in the grid
     */
    private void setColumns(int columns) {
        this.mColumns = columns;
    }

    public void setHiddenVisibility(boolean value) {
        if (mShowHidden != value) {
            mShowHidden = value;
            notifyDataSetChanged();
        }
    }

    public void setListener(GridItemListener listener) {
        mListener = listener;
    }
}