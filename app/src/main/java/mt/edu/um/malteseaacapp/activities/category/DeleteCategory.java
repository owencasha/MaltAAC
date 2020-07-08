package mt.edu.um.malteseaacapp.activities.category;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.VariantManager;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.StorageOps;

public class DeleteCategory extends AppCompatActivity {

    String categoryName; // Name of category being edited
    DatabaseAccess dbAccess; // To access database
    VariantManager variantManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_category);

        // Get name of category being edited from intent
        categoryName = getIntent().getStringExtra("Category");

        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getApplicationContext());
        dbAccess.open();

        // Get instance of the VariantManager class
        variantManager = VariantManager.getInstance(getApplicationContext());

        setUpLayout();
    }

    /**
     * Set up the layout of the "DeleteCategory" activity (including onClickListeners)
     */
    private void setUpLayout()
    {

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
                // Delete category
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Delete the directories pertaining to the category being edited
                        StorageOps.deleteCategoryDirectories(getApplicationContext(), categoryName);

                        // Delete the image associated with the category being edited
                        StorageOps.deleteCategoryImage(getApplicationContext(), categoryName);

                        // Delete category and associated words from database
                        dbAccess.deleteCategory(categoryName);

                        // Rebuild variants cache
                        variantManager.buildCache();

                        // Terminate activity when category is deleted
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
