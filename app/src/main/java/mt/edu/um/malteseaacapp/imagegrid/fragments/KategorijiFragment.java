package mt.edu.um.malteseaacapp.imagegrid.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;

import java.util.Collections;

import mt.edu.um.malteseaacapp.ImageObject;
import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.Home;
import mt.edu.um.malteseaacapp.activities.category.ChangeCategoryImage;
import mt.edu.um.malteseaacapp.activities.category.DeleteCategory;
import mt.edu.um.malteseaacapp.activities.category.RenameCategory;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;

public class KategorijiFragment extends ImageGridFragment {
    // Interface to be implemented by Home activity
    public interface OnKategorijaClickedListener {
        // When the image button is clicked, the image is passed as an argument
        void onKategorijaClicked(String kategorija);
    }
    OnKategorijaClickedListener callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext());
        dbAccess.open();

        // Load image categories from database
        getCursor();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Animate loading of images in grid
        TransitionManager.beginDelayedTransition(container, new Fade().setDuration(getResources().getInteger(R.integer.transition_duration)));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Set callback to Home activity
        callback = (OnKategorijaClickedListener) activity;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (!isCursorValid) {
            getCursor();
        }

        super.onActivityCreated(savedInstanceState);
    }

    /****************************************
     *"GridAdapter.GridItemListener" methods*
     ****************************************/

    @Override
    public void onItemClick(int index) {
        final ImageObject item = (ImageObject) gridAdapter.getItem(index);

        // Pass the category to the main activity
        callback.onKategorijaClicked(item.getWord());
    }

    @Override
    public void onItemLongPress(int index) {
        if (editable) {
            final ImageObject imageObject = (ImageObject) gridAdapter.getItem(index);

            // Pop up add menu and handle clicks
            PopupMenu contextMenu = new PopupMenu(getContext(), gridLayout.getChildAt(index));
            contextMenu.setOnMenuItemClickListener(item -> {
                Intent intent;

                switch (item.getItemId()) {
                    case R.id.rename_category:
                        // Open RenameCategory activity and pass category
                        intent = new Intent(getActivity().getApplicationContext(), RenameCategory.class);
                        intent.putExtra("Category", imageObject.getWord());

                        getActivity().startActivityForResult(intent, Home.REFRESH_KATEGORIJI_FRAGMENT_CODE);
                        return true;

                    case R.id.change_category_image:
                        // Open ChangeCategoryImage activity and pass category
                        intent = new Intent(getActivity().getApplicationContext(), ChangeCategoryImage.class);
                        intent.putExtra("Category", imageObject.getWord());

                        getActivity().startActivityForResult(intent, Home.REFRESH_KATEGORIJI_FRAGMENT_CODE);
                        return true;

                    case R.id.delete_category:
                        // Open DeleteCategory activity and pass category
                        intent = new Intent(getActivity().getApplicationContext(), DeleteCategory.class);
                        intent.putExtra("Category", imageObject.getWord());

                        getActivity().startActivityForResult(intent, Home.REFRESH_KATEGORIJI_FRAGMENT_CODE);
                        return true;

                    case R.id.show_hide:
                        boolean result;
                        boolean value = imageObject.isHidden();

                        if (imageObject.isHidden()) {
                            result = dbAccess.showCategories(Collections.singletonList(imageObject.getWord()));
                        }
                        else {
                            result = dbAccess.hideCategory(imageObject.getWord());
                        }

                        // Successfully written to the database
                        if (result) {
                            imageObject.setHidden(!value);
                            gridAdapter.notifyDataSetChanged();
                            isCursorValid = false;
                        }
                        else {
                            Toast.makeText(getContext(), R.string.database_error, Toast.LENGTH_SHORT).show();
                        }

                        return true;

                    default:
                        return false;
                }
            });

            // Show menu and load contents from xml
            MenuInflater menuInflater = contextMenu.getMenuInflater();
            menuInflater.inflate(R.menu.category_context, contextMenu.getMenu());

            if (imageObject.isHidden()) {
                MenuItem item = contextMenu.getMenu().findItem(R.id.show_hide);
                item.setTitle(R.string.show);
            }

            contextMenu.show();
        } else if (PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean("hold_to_enlarge", true)) {
            showEnlargedImage(index);
        }
    }

    /**
     * Loads categories from database to Cursor
     */
    private void getCursor() {
        if (imageObjectDetailsCursor != null) {
            imageObjectDetailsCursor.close();
        }

        // Load categories from database to cursor
        imageObjectDetailsCursor =  dbAccess.getKategoriji();
        isCursorValid = true;
    }
}
