package mt.edu.um.malteseaacapp.imagegrid.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;

public class ImageButtonsFragment extends ImageGridFragment {

    String category; // Category of ImageButtons to display

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext());
        dbAccess.open();

        // Load image buttons pertaining to specified category
        Bundle args = getArguments();
        category = args.getString("Kategorija");

        getCursor();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TransitionManager.beginDelayedTransition(container, new Fade().setDuration(getResources().getInteger(R.integer.transition_duration)));

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            category = savedInstanceState.getString("Category");
        }

        if (!isCursorValid) {
            getCursor();
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("Category", category);
    }

    /**
     * Loads details of ImageObjects pertaining to a particular
     * category from from database to Cursor
     */
    public void getCursor() {
        if (imageObjectDetailsCursor != null) {
            imageObjectDetailsCursor.close();
        }

        // Load ImageObject details from database to cursor
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("variant_grouping", "Show").equals("Show")) {
            imageObjectDetailsCursor = dbAccess.getUnigrams(category, false);
        } else {
            imageObjectDetailsCursor = dbAccess.getUnigrams(category, true);
        }

        isCursorValid = true;
    }

    /**
     * Gets the category of the image buttons which are being loaded
     * @return Returns the name of the category of the image buttons which are being loaded
     */
    public String getCategory() {
        return category;
    }
}
