package mt.edu.um.malteseaacapp.activities.word;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.javatuples.Pair;

import java.util.Collection;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.VariantManager;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.StorageOps;
import mt.edu.um.malteseaacapp.operations.TextOps;

public class ChangeWord extends AppCompatActivity {
    private DatabaseAccess mDatabase;
    private VariantManager mVariantManager;

    private Collection<Pair<Integer, String>> mNormalisedWords;

    // Target Properties
    private int mId;
    private int mRootId;
    private String mWord;
    private String mCategory;
    private String mNormalisedWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_change_word);

        // Get word properties from the intent
        mId = getIntent().getIntExtra("Id", 0);
        mWord = getIntent().getStringExtra("Word");
        mCategory = getIntent().getStringExtra("Category");

        // Get instance of DatabaseAccess and open connection
        mDatabase = DatabaseAccess.getInstance(getApplicationContext());
        mDatabase.open();

        // Get a list of all normalised words in database
        mNormalisedWords = mDatabase.getNormalisedWords();
        mNormalisedWord = TextOps.normalise(mWord);

        // Get instance of the VariantManager class
        mVariantManager = VariantManager.getInstance(this);

        setUpLayout();
    }

    /**
     * Set up the layout of the "ChangeWord" activity (including onClickListeners)
     */
    private void setUpLayout() {
        final TextView wordEditTitle = this.findViewById(R.id.wordEditTitle);

        // Set activity title
        wordEditTitle.setText(mWord.toUpperCase());

        // Find EditText components from layout
        final EditText wordEditText = this.findViewById(R.id.wordEditText);
        final EditText wordEditRoot = this.findViewById(R.id.wordEditRoot);

        wordEditText.setText(mWord);

        // If the target word is already a root, disable the root field
        if (mVariantManager.isRoot(mId)) {
            wordEditRoot.setEnabled(false);
            wordEditRoot.setAlpha(0.25f);
        }

        // If the target word is a variant, display its root
        if ((mRootId = mVariantManager.getRootId(mId)) != 0) {
            wordEditRoot.setText(mDatabase.getWord(mRootId));
        }

        // Find ProgressBar from layout and set colour
        final ProgressBar progressBar = this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable()
                .setColorFilter(getResources()
                        .getColor(R.color.colorPrimary), android.graphics.PorterDuff.Mode.MULTIPLY);

        // Find buttons from layout and set their onClickListeners
        final Button okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);

        okButton.setOnClickListener(view -> {
            final String enteredWord = wordEditText.getText().toString();
            final String normalisedWord = TextOps.normalise(enteredWord);
            final String enteredRoot = TextOps.normalise(wordEditRoot.getText().toString());
            int selectedRootId = 0;

            // Check if word already exists in database, if modified
            if (!mNormalisedWord.equals(normalisedWord)) {
                for (Pair<Integer, String> p : mNormalisedWords) {
                    if (p.getValue1().equalsIgnoreCase(normalisedWord)) {
                        Toast.makeText(getApplicationContext(),
                                "Sorry, that word already exists",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            // If a root was specified, check if it exists and retrieve its id
            if (!enteredRoot.isEmpty()) {
                for (Pair<Integer, String> p : mNormalisedWords) {
                    int temp = p.getValue0();

                    if (p.getValue1().equalsIgnoreCase(enteredRoot) && !mVariantManager.isVariant(temp)) {
                        selectedRootId = temp;
                        break;
                    }
                }
            }

            // If a root was specified, check that a valid id has been retrieved
            if (!enteredRoot.isEmpty() && selectedRootId == 0) {
                Toast.makeText(getApplicationContext(),
                        "Sorry, that root does not exist, or is already a variant of another word",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // Check if word was left out by user
            if (wordEditText.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(),
                        "Please enter a word", Toast.LENGTH_SHORT).show();
                return;
            }

            int finalRootId = selectedRootId;

            // Change the word
            new Thread(() -> {
                // Rename word image and audio files in internal storage
                StorageOps.changeWord(getApplicationContext(), mCategory, mWord, enteredWord);

                // Insert new word in database
                mDatabase.changeWord(mWord, enteredWord, normalisedWord, finalRootId);

                // Update variants cache if necessary
                if (ChangeWord.this.mRootId != finalRootId) {
                    if (finalRootId != 0) {
                        mVariantManager.modifyVariant(finalRootId, ChangeWord.this.mId);
                    } else {
                        mVariantManager.removeVariant(ChangeWord.this.mId);
                    }
                }

                // Terminate activity when word is changed
                setResult(RESULT_OK);
                finish();
            }).start();

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);
        });

        cancelButton.setOnClickListener(view -> onBackPressed());
    }
}
