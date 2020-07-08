package mt.edu.um.malteseaacapp.activities.word;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.StorageOps;

public class ChangeCategory extends AppCompatActivity {

    DatabaseAccess dbAccess; // To access database
    String word; // Word being edited
    String category; // Category of word being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_category);

        // Get word being edited and its category from intent
        word = getIntent().getStringExtra("Word");
        category = getIntent().getStringExtra("Category");

        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getApplicationContext());
        dbAccess.open();

        setUpLayout();
    }

    /**
     * Set up the layout of the "ChangeCategory" activity (including onClickListeners)
     */
    private void setUpLayout()
    {
        // Get categories from database and put them in a list of strings
        ArrayList<String> categoryNames = dbAccess.getCategoryNames();
        // Remove current category from list of categories (since it cannot be selected)
        categoryNames.remove(category);
        // Create adapter with categories to use with spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this, R.layout.spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
        // Put categories in spinner
        final Spinner kategorijiSpinner = this.findViewById(R.id.kategorijiSpinner);
        kategorijiSpinner.setAdapter(spinnerAdapter);

        // Find ProgressBar from layout and set colour
        final ProgressBar progressBar = this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources()
                        .getColor(R.color.colorPrimary),
                android.graphics.PorterDuff.Mode.MULTIPLY);

        // Find buttons from layout and set their onClickListeners
        final Button okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if there are any categories to add the word to
                if (categoryNames.size() == 0) {
                    Toast.makeText(getApplicationContext(),
                            "No categories available. Please add a category first.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Change word category
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Move image and audio files to new category directory
                        StorageOps.changeCategory(getApplicationContext(), category,
                                kategorijiSpinner.getSelectedItem().toString(), word);
                        // Update category attribute in database
                        dbAccess.changeCategory(word,
                                kategorijiSpinner.getSelectedItem().toString());
                        // Terminate activity when word category is changed
                        setResult(RESULT_OK);
                        finish();
                    }
                }).start();

                // Show progress bar
                progressBar.setVisibility(View.VISIBLE);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
}
