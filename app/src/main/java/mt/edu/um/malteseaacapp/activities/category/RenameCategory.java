package mt.edu.um.malteseaacapp.activities.category;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.StorageOps;
import mt.edu.um.malteseaacapp.operations.TextOps;

public class RenameCategory extends AppCompatActivity {

    String categoryName; // Name of category being edited
    DatabaseAccess dbAccess; // To access database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename_category);

        // Get name of category being edited from intent
        categoryName = getIntent().getStringExtra("Category");

        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getApplicationContext());
        dbAccess.open();

        setUpLayout();
    }

    /**
     * Set up the layout of the "RenameCategory" activity (including onClickListeners)
     */
    private void setUpLayout()
    {
        // Find EditText from layout
        final EditText categoryEditText = this.findViewById(R.id.categoryEditText);

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
                // Get a list of all normalised category names in database
                ArrayList<String> normalisedCategoryNames = dbAccess.getNormalisedCategoryNames();

                // Get entered category name
                final String enteredCategoryName = categoryEditText.getText().toString();
                // Normalise entered category name
                final String normalisedCategoryName = TextOps.normalise(enteredCategoryName);
                // Convert to lower case
                final String normalisedLowerCase = normalisedCategoryName.toLowerCase();

                // Check if category name already exists in database
                for (String s : normalisedCategoryNames) {
                    if (s.equalsIgnoreCase(normalisedLowerCase)) {
                        Toast.makeText(getApplicationContext(),
                                "Sorry, that category name already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Check if category name was left out by user
                if (categoryEditText.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),
                            "Please enter a category name", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update category  name
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Rename the directories pertaining to the category being edited
                        StorageOps.renameCategoryDirectories(
                                getApplicationContext(), categoryName, enteredCategoryName);
                        // Rename the image associated with the category being edited
                        StorageOps.renameCategoryImage(
                                getApplicationContext(), categoryName, enteredCategoryName);
                        // Change references to category's old name in database
                        dbAccess.editCategoryName(
                                categoryName, enteredCategoryName, normalisedCategoryName);
                        // Terminate activity when category is renamed
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
