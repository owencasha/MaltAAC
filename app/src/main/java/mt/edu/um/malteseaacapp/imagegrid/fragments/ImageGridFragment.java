package mt.edu.um.malteseaacapp.imagegrid.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.transitionseverywhere.Slide;
import com.transitionseverywhere.TransitionManager;

import java.util.Collections;

import mt.edu.um.malteseaacapp.ImageObject;
import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.Home;
import mt.edu.um.malteseaacapp.activities.word.ChangeCategory;
import mt.edu.um.malteseaacapp.activities.word.ChangeImage;
import mt.edu.um.malteseaacapp.activities.word.ChangeSound;
import mt.edu.um.malteseaacapp.activities.word.ChangeWord;
import mt.edu.um.malteseaacapp.activities.word.DeleteWord;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.imagegrid.gridsetup.AdapterGridLayout;
import mt.edu.um.malteseaacapp.imagegrid.gridsetup.GridAdapter;
import mt.edu.um.malteseaacapp.imagegrid.gridsetup.GridPagination;

public abstract class ImageGridFragment extends Fragment implements GestureDetector.OnGestureListener, GridAdapter.GridItemListener {
    protected AdapterGridLayout gridLayout; // For displaying image buttons in a grid
    protected GridAdapter gridAdapter; // To populate grid layout with image buttons
    protected GridPagination gridPagination; // To handle grid pages
    protected Cursor imageObjectDetailsCursor; // To hold ImageObject details loaded from database
    // Variables related to grid pages
    protected int noOfButtonsPerPage;
    protected int currentPage;
    protected int noOfRows;
    protected int noOfColumns;
    protected int backgroundColor;
    protected String gridSize;
    // Declare zoomed image
    protected ImageView zoomedImage;
    protected ImageButton coreButton;
    protected ImageButton homeButton;
    protected ImageButton nextButton;
    protected ImageButton backButton;

    protected boolean editable = false;
    protected boolean isLongPress = false;
    protected boolean isCursorValid = false;
    DatabaseAccess dbAccess; // To access database
    View view; // The view returned by onCreateView
    // To detect gestures
    GestureDetector gestureDetector;
    OnImageButtonClickedListener callback;
    OnCoreButtonClickedListener coreCallback;
    onHomeButtonClickedListener homeCallback;

    public ImageGridFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // For detecting touches on grid
        gestureDetector = new GestureDetector(getContext(), this);

        // If available, get grid details from saved instance
        if (savedInstanceState != null) {
            noOfButtonsPerPage = savedInstanceState.getInt("NoOfButtonsPerPage");
            noOfRows = savedInstanceState.getInt("NoOfRows");
            noOfColumns = savedInstanceState.getInt("NoOfColumns");
            currentPage = savedInstanceState.getInt("CurrentPage");
            backgroundColor = savedInstanceState.getInt("BackgroundColor");
            isCursorValid = savedInstanceState.getBoolean("IsCursorValid");
        } else {
            // Obtain grid details from shared preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

            gridSize = sharedPreferences.getString("grid_size", "2x2");

            switch (gridSize) {
                case "1x1":
                    noOfRows = 1;
                    noOfColumns = 1;
                    break;
                case "1x2":
                    noOfRows = 1;
                    noOfColumns = 2;
                    break;
                case "1x3":
                    noOfRows = 1;
                    noOfColumns = 3;
                    break;
                case "2x4":
                    noOfRows = 2;
                    noOfColumns = 4;
                    break;
                case "2x5":
                    noOfRows = 2;
                    noOfColumns = 5;
                    break;
                case "2x6":
                    noOfRows = 2;
                    noOfColumns = 6;
                    break;
                case "3x5":
                    noOfRows = 3;
                    noOfColumns = 5;
                    break;
                case "4x6":
                    noOfRows = 4;
                    noOfColumns = 6;
                    break;
                default: // 2x2
                    noOfRows = 2;
                    noOfColumns = 2;
                    break;
            }

            backgroundColor = sharedPreferences.getInt("background_color", 0xFFFFFFFF);
            noOfButtonsPerPage = noOfRows * noOfColumns;
            currentPage = 0; // Set default grid page/index to 0 (the start page)
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("NoOfButtonsPerPage", noOfButtonsPerPage);
        outState.putInt("NoOfRows", noOfRows);
        outState.putInt("NoOfColumns", noOfColumns);
        outState.putInt("CurrentPage", currentPage);
        outState.putInt("BackgroundColor", backgroundColor);
        outState.putBoolean("IsCursorValid", isCursorValid);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate layout for fragment
        final View view = inflater.inflate(R.layout.fragment_image_grid, container, false);

        this.view = view;
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialise views
        zoomedImage = view.findViewById(R.id.zoomedImage);
        coreButton = view.findViewById(R.id.showCore);
        homeButton = view.findViewById(R.id.home);

        // Set listeners for the home and core buttons
        coreButton.setOnClickListener(view -> coreCallback.onCoreButtonClicked());
        homeButton.setOnClickListener(view -> homeCallback.onHomeButtonClicked());

        // Set listener for buttons grid if any items where loaded from database
        if (imageObjectDetailsCursor != null) {
            gridPagination = new GridPagination(getActivity().getApplicationContext(), imageObjectDetailsCursor, noOfButtonsPerPage);

            if (gridPagination.getCount() != 0) {
                view.findViewById(R.id.buttonsGrid).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
            } else {
                TextView errorMessage = view.findViewById(R.id.errorMessage);
                errorMessage.setText(R.string.no_words);
                errorMessage.setVisibility(View.VISIBLE);

                if (ColorUtils.calculateLuminance(backgroundColor) > 0.4) {
                    errorMessage.setTextColor(view.getResources().getColor(R.color.black));
                } else {
                    errorMessage.setTextColor(view.getResources().getColor(R.color.white));
                }
            }
        }

        // Set image grid according to layout specified in xml
        gridLayout = view.findViewById(R.id.buttonsGrid);
        gridLayout.setColumnCount(noOfColumns);
        gridLayout.setRowCount(noOfRows);

        // Set the background colour
        view.setBackgroundColor(backgroundColor);

        // If there are any image buttons to display
        if (imageObjectDetailsCursor != null) {
            if (gridPagination.getCount() != 0) {

                // Create an instance of the GridAdapter class
                gridAdapter = new GridAdapter(getActivity().getApplicationContext(), noOfColumns, noOfRows, backgroundColor);

                // Set grid adapter so as to populate grid view with image buttons
                // and notify that the data set has been changed
                gridAdapter.setHiddenVisibility(editable);
                gridAdapter.setListener(this);
                gridAdapter.setGridList(gridPagination.newPage(currentPage));
                gridAdapter.notifyDataSetChanged();
                gridLayout.setAdapter(gridAdapter);
            }

            setUpNavButtons(view);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Close database cursor
        if (imageObjectDetailsCursor != null)
            imageObjectDetailsCursor.close();

        // Close database cursor in GridPagination
        if (gridPagination != null)
            gridPagination.close();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Set callback to Home activity
        callback = (OnImageButtonClickedListener) activity;
        coreCallback = (OnCoreButtonClickedListener) activity;
        homeCallback = (onHomeButtonClickedListener) activity;
    }

    protected void setUpNavButtons(View view) {
        nextButton = view.findViewById(R.id.nextButton);
        backButton = view.findViewById(R.id.backButton);

        nextButton.setEnabled(false);
        backButton.setEnabled(false);

        if (gridPagination.getCount() != 0) {
            nextButton.setOnClickListener(v -> swipeNext());
            backButton.setOnClickListener(v -> swipeBack());

            if (currentPage != 0) {
                backButton.setEnabled(true);
            }

            if (currentPage != gridPagination.getLastPage()) {
                nextButton.setEnabled(true);
            }
        }
    }

    /**
     * Sets up the image buttons grid (including adapter and pagination)
     * and configures the navigation, i.e. previous and next buttons
     *
     * @param view The view on which to make changes
     */
    protected void configureGridNavigation(View view) {
        if (view == null) {
            return;
        }

        // Create instance of GridPagination with the appropriate details
        gridPagination = new GridPagination(getActivity().getApplicationContext(), imageObjectDetailsCursor, noOfButtonsPerPage);

        // Update adapter contents and notify that the data set has been changed
        gridAdapter.setGridList(gridPagination.newPage(currentPage));
        gridAdapter.notifyDataSetChanged();

        setUpNavButtons(view);
    }

    /***************************************************************
     *Implementation of "GestureDetector.OnGestureListener" methods*
     ***************************************************************/

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float velocityX, float velocityY) {
        // Calculate difference in x-coordinate
        float difference = motionEvent1.getRawX() - motionEvent.getRawX();

        // Ensure swipe is strong enough
        if (Math.abs(difference) > 100 && Math.abs(velocityX) > 100) {
            // Check if swipe right or left
            if (difference > 0) {
                swipeBack();
            } else {
                swipeNext();
            }

            return true;
        }

        return false;
    }

    private void swipeBack() {
        ViewGroup transitionsContainer = view.findViewById(R.id.buttonsGrid);

        if (currentPage != 0) {
            // If currently on the 2nd page, hide the previous button
            if (currentPage == 1) {
                backButton.setEnabled(false);
            }

            // If currently on the last page (and not on the first), show the next button
            if (currentPage == gridPagination.getLastPage() && currentPage != 0) {
                nextButton.setEnabled(true);
            }

            // Animate image buttons
            TransitionManager.beginDelayedTransition(transitionsContainer, new Slide(Gravity.START).setDuration(100));

            currentPage--;

            // Update adapter content according to page number
            // and handle clicks for buttons on the new page
            gridAdapter.setGridList(gridPagination.newPage(currentPage));
            gridAdapter.notifyDataSetChanged();
        }
    }

    private void swipeNext() {
        ViewGroup transitionsContainer = view.findViewById(R.id.buttonsGrid);

        if (currentPage != gridPagination.getLastPage()) {
            // If currently on the page before the last, hide the next button
            if (currentPage == (gridPagination.getLastPage() - 1)) {
                nextButton.setEnabled(false);
            }

            // If currently on the 1st page, show the previous button
            if (currentPage == 0 && currentPage != gridPagination.getLastPage()) {
                backButton.setEnabled(true);
            }

            // Animate image buttons
            TransitionManager.beginDelayedTransition(transitionsContainer, new Slide(Gravity.END).setDuration(100));

            currentPage++;

            // Update adapter content according to page number
            // and handle clicks for buttons on the new page
            gridAdapter.setGridList(gridPagination.newPage(currentPage));
            gridAdapter.notifyDataSetChanged();
        }
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean value) {
        if (editable != value) {
            editable = value;

            if (gridAdapter != null) {
                gridAdapter.setHiddenVisibility(value);
            }
        }
    }

    /****************************************
     *"GridAdapter.GridItemListener" methods*
     ****************************************/

    public void onItemTouchUp(int index, MotionEvent motionEvent) {
        if (isLongPress && zoomedImage.getVisibility() == View.VISIBLE) {
            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setInterpolator(new AccelerateInterpolator());
            fadeOut.setStartOffset(0);
            fadeOut.setDuration(350);

            zoomedImage.startAnimation(fadeOut);
            zoomedImage.setVisibility(View.GONE);

            isLongPress = false;
        }
    }

    public void onItemLongPress(int index) {
        if (editable) {
            final ImageObject imageObject = (ImageObject) gridAdapter.getItem(index);

            // Pop up add menu and handle clicks
            PopupMenu contextMenu = new PopupMenu(getContext(), gridLayout.getChildAt(index));
            contextMenu.setOnMenuItemClickListener(item -> {
                Intent intent;

                switch (item.getItemId()) {
                    case R.id.change_word:
                        // Open ChangeWord activity and pass extras
                        intent = new Intent(getActivity().getApplicationContext(), ChangeWord.class);

                        intent.putExtra("Id", imageObject.getId());
                        intent.putExtra("Word", imageObject.getWord());
                        intent.putExtra("Category", imageObject.getCategory());

                        getActivity().startActivityForResult(intent, Home.REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE);
                        return true;

                    case R.id.change_category:
                        // Open ChangeCategory activity and pass extras
                        intent = new Intent(getActivity().getApplicationContext(), ChangeCategory.class);

                        intent.putExtra("Word", imageObject.getWord());
                        intent.putExtra("Category", imageObject.getCategory());

                        getActivity().startActivityForResult(intent, Home.REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE);
                        return true;

                    case R.id.change_image:
                        // Open ChangeImage activity and pass extras
                        intent = new Intent(getActivity().getApplicationContext(), ChangeImage.class);

                        intent.putExtra("Word", imageObject.getWord());
                        intent.putExtra("Category", imageObject.getCategory());

                        getActivity().startActivityForResult(intent, Home.REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE);
                        return true;

                    case R.id.change_sound:
                        // Open ChangeSound activity and pass extras
                        intent = new Intent(getActivity().getApplicationContext(), ChangeSound.class);

                        intent.putExtra("Word", imageObject.getWord());
                        intent.putExtra("Category", imageObject.getCategory());

                        getActivity().startActivityForResult(intent, Home.REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE);
                        return true;

                    case R.id.delete_word:
                        // Open DeleteWord activity and pass extras
                        intent = new Intent(getActivity().getApplicationContext(), DeleteWord.class);

                        intent.putExtra("Id", imageObject.getId());
                        intent.putExtra("Word", imageObject.getWord());
                        intent.putExtra("Category", imageObject.getCategory());

                        getActivity().startActivityForResult(intent, Home.REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE);
                        return true;

                    case R.id.show_hide:
                        boolean result;
                        boolean value = imageObject.isHidden();

                        if (imageObject.isHidden()) {
                            result = dbAccess.showWords(Collections.singletonList(imageObject.getWord()));
                        } else {
                            result = dbAccess.hideWord(imageObject.getWord());
                        }

                        // Successfully written to the database
                        if (result) {
                            imageObject.setHidden(!value);
                            gridAdapter.notifyDataSetChanged();
                            isCursorValid = false;
                        } else {
                            Toast.makeText(getContext(), R.string.database_error, Toast.LENGTH_SHORT).show();
                        }

                        return true;

                    default:
                        return false;
                }
            });

            // Show menu and load contents from xml
            MenuInflater menuInflater = contextMenu.getMenuInflater();
            menuInflater.inflate(R.menu.word_context, contextMenu.getMenu());

            if (imageObject.isHidden()) {
                MenuItem item = contextMenu.getMenu().findItem(R.id.show_hide);
                item.setTitle(R.string.show);
            }

            contextMenu.show();

        } else if (PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean("hold_to_enlarge", true)) {
            showEnlargedImage(index);
        }
    }

    public void onItemClick(int index) {
        ImageObject imageObject = (ImageObject) gridAdapter.getItem(index);
        ImageView imageView = (ImageView) gridLayout.getChildAt(index).findViewById(R.id.imageButton);

        // Return the item to the main activity
        callback.onImageButtonClicked(imageObject, imageView);
    }

    protected void showEnlargedImage(int index) {
        final ImageView imageView = (ImageView) gridLayout.getChildAt(index).findViewById(R.id.imageButton);

        zoomedImage.setImageDrawable(imageView.getDrawable());

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(100);

        zoomedImage.startAnimation(fadeIn);
        zoomedImage.setVisibility(View.VISIBLE);

        isLongPress = true;
    }

    // Interface to be implemented by Home activity
    public interface OnImageButtonClickedListener {
        // When the image button is clicked, the image is passed as an argument
        void onImageButtonClicked(ImageObject imageObject, ImageView tappedImage);
    }

    // Interface to be implemented by Home activity
    public interface OnCoreButtonClickedListener {
        void onCoreButtonClicked();
    }

    // Interface to be implemented by Home activity
    public interface onHomeButtonClickedListener {
        void onHomeButtonClicked();
    }
}