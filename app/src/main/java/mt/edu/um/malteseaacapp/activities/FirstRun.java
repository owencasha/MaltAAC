package mt.edu.um.malteseaacapp.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.settings.Settings;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.StorageOps;

public class FirstRun extends AppCompatActivity {
     private DatabaseAccess mDbAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent activity from closing due to outside touch events
        setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_first_run);
        // Execute firstRun method
        firstRun();
    }

    @Override
    public void onBackPressed() {
        // Do nothing when back button is pressed
    }

    /**
     * Method which is executed the first time the application is run. Takes care of loading the
     * "predictions", "images" and "audio" folders from the Assets directory to internal storage.
     */
    private void firstRun()
    {
        mDbAccess = DatabaseAccess.getInstance(getApplicationContext());
        mDbAccess.open();

        // Find OK button from layout and set onClickListener
        final Button okButton = this.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Disable any further clicks on the activity
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                // Copy directories from Assets folder to internal storage
                new FirstRun.CopyAssetsTask(FirstRun.this).execute();
            }
        });
    }

    /**
     * Static class used for copying directories from Assets folder to internal storage
     * Declared static and uses a weak reference to the FirstRun activity to prevent memory leaks
     */
    private static class CopyAssetsTask extends AsyncTask<Void, Integer, Void>
    {

        private WeakReference<FirstRun> firstRunWeakReference;

        /**
         * Constructor of CopyAssetsTask. Retains a weak reference to the FirstRun
         * activity to prevent memory leaks
         * @param context   The context
         */
        CopyAssetsTask(FirstRun context) {
            firstRunWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            // Find linear layout containing progress components and make it visible
            LinearLayout progressLayout = firstRunWeakReference.get()
                    .findViewById(R.id.progressLayout);
            progressLayout.setVisibility(View.VISIBLE);
            // Find ProgressBar from layout and set colour
            ProgressBar progressBar = firstRunWeakReference.get()
                    .findViewById(R.id.progressBar);
            progressBar.getIndeterminateDrawable().setColorFilter(firstRunWeakReference.get()
                            .getResources().getColor(R.color.colorPrimary),
                    android.graphics.PorterDuff.Mode.MULTIPLY);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            publishProgress(0);
            // Copy 'predictions' directory from Assets to internal storage
            StorageOps.copyAssetsSubdirectoryRecursively(firstRunWeakReference.get()
                    .getApplicationContext(), "predictions");
            publishProgress(10);
            // Copy 'images' directory from Assets to internal storage
            StorageOps.copyAssetsSubdirectoryRecursively(firstRunWeakReference.get()
                    .getApplicationContext(), "images");
            publishProgress(50);
            // Copy 'audio' directory from Assets to internal storage
            StorageOps.copyAssetsSubdirectoryRecursively(firstRunWeakReference.get()
                    .getApplicationContext(), "audio");
            publishProgress(90);

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            // Find ProgressBar from layout
            ProgressBar progressBar = firstRunWeakReference.get()
                    .findViewById(R.id.progressBar);

            // Animate progress changes
            ObjectAnimator progressBarAnimation
                    = ObjectAnimator.ofInt(progressBar, "progress", progress);
            progressBarAnimation.setInterpolator(new DecelerateInterpolator());
            progressBarAnimation.setDuration(500);
            progressBarAnimation.start();

            // Find progress TextView from layout and set text
            TextView progressText = firstRunWeakReference.get()
                    .findViewById(R.id.progressText);
            String text = progress +"%";
            progressText.setText(text);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Indicate that the first run has completed in
            // shared preferences and start Settings activity
            PreferenceManager
                    .getDefaultSharedPreferences(firstRunWeakReference.get())
                    .edit().putBoolean("first_run_complete", true).commit();

            Intent settings = new Intent(firstRunWeakReference.get().getApplicationContext(),  Settings.class);
            firstRunWeakReference.get().mDbAccess.close();
            firstRunWeakReference.get().startActivity(settings);
            firstRunWeakReference.get().finish();
        }
    }
}
