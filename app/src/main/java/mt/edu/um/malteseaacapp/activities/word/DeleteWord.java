package mt.edu.um.malteseaacapp.activities.word;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.VariantManager;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.StorageOps;

public class DeleteWord extends AppCompatActivity {
    int id; // Id of the word being edited
    String word; // Word being edited
    String category; // Category of word being edited
    DatabaseAccess dbAccess; // To access database
    VariantManager variantManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_word);

        // Get word being edited and its category from intent
        id = getIntent().getIntExtra("Id", 0);
        word = getIntent().getStringExtra("Word");
        category = getIntent().getStringExtra("Category");

        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getApplicationContext());
        dbAccess.open();

        // Get instance of the VariantManager class
        variantManager = VariantManager.getInstance(this);

        setUpLayout();
    }

    /**
     * Set up the layout of the "DeleteWord" activity (including onClickListeners)
     */
    private void setUpLayout() {
        // Find ProgressBar from layout and set colour
        final ProgressBar progressBar = this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable()
                .setColorFilter(getResources()
                        .getColor(R.color.colorPrimary), android.graphics.PorterDuff.Mode.MULTIPLY);

        // Find buttons from layout and set their onClickListeners
        final Button okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);

        okButton.setOnClickListener(view -> {
            // Delete word
            new Thread(() -> {
                // Delete word image and sound from internal storage
                StorageOps.deleteWord(getApplicationContext(), category, word);

                // Delete word entry from database
                dbAccess.deleteWord(word);

                // Update variants cache if necessary
                if (variantManager.isRootOrVariant(DeleteWord.this.id)) {
                    variantManager.removeEntry(id);
                }

                // Terminate activity when word is deleted
                setResult(RESULT_OK);
                finish();
            }).start();

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);
        });
        cancelButton.setOnClickListener(view -> onBackPressed());
    }
}
