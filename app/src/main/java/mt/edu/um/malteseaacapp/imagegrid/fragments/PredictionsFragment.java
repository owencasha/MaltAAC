package mt.edu.um.malteseaacapp.imagegrid.fragments;

import android.app.Activity;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mt.edu.um.malteseaacapp.ImageObject;
import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;

public class PredictionsFragment extends ImageGridFragment {
    public interface FragmentRequestListener {
        void onRemoveRequest();
    }

    private HashMap<String, HashMap<String, Double>> mNgrams; // To hold n-grams
    private FragmentRequestListener mListener;
    private String mPrefix; // Prefix for which to generate predictions
    private boolean mEndOfSentence; // To check whether the end of the sentence has been reached

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
        mListener = (FragmentRequestListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext());
        dbAccess.open();

        // If available, load data from savedInstanceState
        if (savedInstanceState != null) {
            mPrefix = savedInstanceState.getString("Prefix");
            mNgrams = (HashMap) savedInstanceState.getSerializable("Ngrams");
            mEndOfSentence = savedInstanceState.getBoolean("EndOfSentence");
        } else {
            // Obtain n-grams and prefix from Home activity
            Bundle args = getArguments();
            mPrefix = args.getString("Prefix");
            mNgrams = (HashMap) args.getSerializable("Ngrams");
            mEndOfSentence = false; // Initially, the end of sentence has not been reached
        }

        populateGridWithPredictions(mNgrams, mPrefix);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save data to outState
        outState.putSerializable("Ngrams", mNgrams);
        outState.putString("Prefix", mPrefix);
        outState.putBoolean("EndOfSentence", mEndOfSentence);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        homeButton.setEnabled(false);
        coreButton.setEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        // If the end of sentence has been reached at the start of the fragment, request its removal
        if (mEndOfSentence && mListener != null) {
            mListener.onRemoveRequest();
        }
    }

    private MatrixCursor getMatrixCursor() {
        String[] columns = {"_id", "Word", "Category", "IsHidden"};
        Object[] columnValues = new Object[4]; // To store data to put in cursor
        MatrixCursor matrixCursor = new MatrixCursor(columns);

        // Declare image object and the attributes needed to be put in cursor
        ImageObject imageObject;

        // Sort predictions by PKN given the prefix
        HashMap<String, Double> sortedNextWords = sortByPKN(mNgrams.get(mPrefix));

        // Go through sorted HashMap and populate predictions cursor
        for (Map.Entry<String, Double> entry: sortedNextWords.entrySet())
        {
            // If the next word is the sentence termination symbol,
            // do not show image (since no image exists)
            if (entry.getKey().equals("</s>"))
            {
                // If the only prediction is the sentence termination symbol, the end of sentence
                // has been reached; therefore, return to kategorijiFragment i.e. onBackPressed
                if (sortedNextWords.size() == 1)
                {
                    mEndOfSentence = true;
                    matrixCursor.close();

                    return null;
                }

                continue;
            }

            // Create ImageObject and put details in cursor
            imageObject = dbAccess.getImageObject(getActivity().getApplicationContext(), entry.getKey());

            // If the predicted word was not found in the database,
            // skip it and don't list it with the rest of the predictions
            if (imageObject == null) continue;

            // Don't show the predicted word if it is hidden
            if (!imageObject.isHidden()) {
                columnValues[0] = imageObject.getId();
                columnValues[1] = imageObject.getWord();
                columnValues[2] = imageObject.getCategory();
                columnValues[3] = 0;

                matrixCursor.addRow(columnValues);
            }
        }

        return matrixCursor;
    }

    /**
     * Populates the image grid with the predictions and their corresponding images
     * @param ngrams    A HashMap containing all n-grams of order n
     * @param prefix    The sequence of words on which to base the next word prediction
     */
    public void populateGridWithPredictions(HashMap<String, HashMap<String, Double>> ngrams, String prefix) {
        mNgrams = ngrams;
        mPrefix = prefix;

        // Indicate whether the predictions fragment is active
        boolean predictionsActive = false;

        if (imageObjectDetailsCursor != null) {
            predictionsActive = imageObjectDetailsCursor.getCount() > 0;
        }

        MatrixCursor matrixCursor = getMatrixCursor();

        // End of sentence
        if (mEndOfSentence) {
            if (gridLayout != null) {
                mListener.onRemoveRequest();
            }

            return;
        }

         /* If no predictions were added to the cursor, go back to previous fragment if the
          * predictions fragment was not previously active, or simply return and leave the
          * predictions list unchanged if the predictions fragment was previously active
          */
        if (matrixCursor.getCount() == 0) {
            matrixCursor.close();

            if (!predictionsActive) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            }

            return;
        }

        // Copy the MatrixCursor to the Cursor
        if (imageObjectDetailsCursor != null) {
            imageObjectDetailsCursor.close();
        }

        imageObjectDetailsCursor = matrixCursor;
        isCursorValid = true;

        // Set page number to 0 (i.e. 1st page)
        currentPage = 0;

        // Check if grid layout is null. If it is null, do not configure grid navigation yet.
        // The configuration will take place when onCreateView gets called
        if (gridLayout != null) {
            ViewGroup viewGroup = (ViewGroup) getView().getParent();

            // Animate loading of predictions
            TransitionManager.beginDelayedTransition(viewGroup, new Fade().setDuration(150));
            configureGridNavigation(getView());
        }
    }

    /**
     * Sorts the HashMap containing the possible next words and their probabilities
     * in descending order of their interpolated Kneser-Ney probability
     * @param nextWords The HashMap containing the possible next words and their probabilities
     * @return Returns the HashMap sorted by PKN in descending order
     */
    public HashMap<String, Double> sortByPKN(HashMap<String, Double> nextWords) {
        // Convert HashMap to LinkedList to allow for sorting
        List<Map.Entry<String, Double>> nextWordsList = new LinkedList<>(nextWords.entrySet());
        // Sort list in descending order by using a comparator
        Collections.sort( nextWordsList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> entry1, Map.Entry<String, Double> entry2) {
                return (entry2.getValue()).compareTo(entry1.getValue());
            }
        });

        // Store sorted next words in LinkedHashMap to retain order
        HashMap<String, Double> sortedNextWords = new LinkedHashMap<>();
        // Go through sorted list and populate new HashMap with sorted entries
        for (Map.Entry<String, Double> entry : nextWordsList) {
            sortedNextWords.put(entry.getKey(), entry.getValue());
        }
        return sortedNextWords;
    }

    /**
     * Sets the prefix to be used for generating predictions
     * @param prefix    The specified prefix to be used for generating predictions
     */
    public void setPrefix(String prefix) {
        mPrefix = prefix;
    }

    /**
     * Sets the ngrams to be used for generating predictions
     * @param ngrams    The specified ngrams to be used for generating predictions
     */
    public void setNgrams(HashMap<String, HashMap<String, Double>> ngrams) {
        mNgrams = ngrams;
    }
}