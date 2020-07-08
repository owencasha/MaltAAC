package mt.edu.um.malteseaacapp.imagegrid.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;

public class VariantsFragment extends ImageGridFragment {
    private int mRootId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        dbAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext());
        dbAccess.open();

        // Load Args
        Bundle args = getArguments();
        mRootId = args.getInt("RootId");

        // Load Cursor
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
            mRootId = savedInstanceState.getInt("RootId");
        }

        if (!isCursorValid) {
            getCursor();
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("RootId", mRootId);
    }

    private void getCursor() {
        if (imageObjectDetailsCursor != null) {
            imageObjectDetailsCursor.close();
        }

        // Load categories from database to cursor
        imageObjectDetailsCursor = dbAccess.getVariants(mRootId);
        isCursorValid = true;
    }
}