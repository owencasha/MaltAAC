package mt.edu.um.malteseaacapp.imagegrid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;

public class CoreFragment extends ImageGridFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        dbAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext());
        dbAccess.open();

        // Load Cursor
        imageObjectDetailsCursor = dbAccess.getCoreUnigrams();
        isCursorValid = true;

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TransitionManager.beginDelayedTransition(container, new Fade().setDuration(getResources().getInteger(R.integer.transition_duration)));

        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
