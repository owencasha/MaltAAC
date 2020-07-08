package mt.edu.um.malteseaacapp.activities.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.util.DisplayMetrics;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.Home;

public class Settings extends AppCompatPreferenceActivity {

    // To check whether the pin has been entered
    private boolean pinEntered;
    // To check whether pin has been entered successfully
    private final int ENTER_PIN_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initially the pin is not entered
        pinEntered = false;

        // If no pin has been set or if the correct pin has been entered, show settings menu
        // Otherwise, open the EnterPin activity
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("pin", "").equals("") || pinEntered) {

            addPreferencesFromResource(R.xml.preferences);

            enableRestoreDefaultPreferencesFunctionality();
            determineBiometricPreference();
            hideUnusablePreferenceItems();
        }
        else {
            Intent enterPin = new Intent(this, EnterPin.class);
            startActivityForResult(enterPin, ENTER_PIN_REQUEST_CODE);
        }
    }

    @Override
    public void onBackPressed() {
        // Launch Home activity
        Intent home = new Intent(getApplicationContext(), Home.class);
        startActivity(home);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ENTER_PIN_REQUEST_CODE:
                // Show settings if pin was entered successfully and
                // go back to Home activity if pin was not entered successfully
                if (resultCode == RESULT_OK) {

                    pinEntered = true;

                    addPreferencesFromResource(R.xml.preferences);

                    enableRestoreDefaultPreferencesFunctionality();
                    determineBiometricPreference();
                    hideUnusablePreferenceItems();
                }
                else
                    onBackPressed();

                break;
        }
    }

    /**
     * Enables the Settings activity to restore the default preferences of the app
     */
    @SuppressLint("ApplySharedPref")
    private void enableRestoreDefaultPreferencesFunctionality() {

        // Retrieves restore_preferences and stores it as an object

        // Holds the preference used to restore the default preferences
        Preference restoreDefaultPreferences = findPreference("restore_preferences");

        // Registers a click listener for the preference
        restoreDefaultPreferences.setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setMessage("Are you sure you want to reset your preferences?");
            alert.setNegativeButton("Cancel", (dialog, whichButton) -> {});
            alert.setPositiveButton("Yes", (dialog, whichButton) -> {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                // Note: For setDefaultValues method to work, the shared preferences must first be cleared
                sp.edit().clear().commit();
                PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, true);

                // Done so that the FirstRun activity is not launched after the preferences have been restored
                sp.edit().putBoolean("first_run_complete", true).commit();

                onBackPressed();
            });
            alert.setCancelable(true);
            alert.show();

            return true;
        });
    }

    private void determineBiometricPreference() {

        EditTextPreference pin = (EditTextPreference) findPreference("pin");
        SwitchPreference fingerprint = (SwitchPreference) findPreference("fingerprint_set");

        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(getApplicationContext());

        // enables preference on preference view entry, only if device is eligible and has a set pin
        if (fingerprintManager.isHardwareDetected() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !PreferenceManager.getDefaultSharedPreferences(this).getString("pin", "").isEmpty())
            fingerprint.setEnabled(true);
        else
            fingerprint.setEnabled(false);

        /*
        * event for live enabling or disabling biometric preference while inside the view,
        * depending on whether pin is set or removed
         */
        pin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                if (o.toString().isEmpty()) {

                    // pin became empty, ie removed

                    fingerprint.setChecked(false);
                    fingerprint.setEnabled(false);

                } else {

                    // only enable preference if device supports fingerprint reading
                    if (fingerprintManager.isHardwareDetected() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        fingerprint.setChecked(false);
                        fingerprint.setEnabled(true);
                    }

                }

                return true;
            }
        });
    }

    private void hideUnusablePreferenceItems()
    {
        // get screen diagonal size
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);

        double diagSize = Math.sqrt(x + y);

        if (diagSize < 6.5)
        {
            SwitchPreference holdToEnlarge = (SwitchPreference) findPreference("hold_to_enlarge");
            holdToEnlarge.setChecked(false);
            holdToEnlarge.setEnabled(false);

            // show smaller grid options for mobile devices
            ListPreference gridSizes = (ListPreference) findPreference("grid_size");
            gridSizes.setEntries(R.array.grid_size_entries_small);
            gridSizes.setEntryValues(R.array.grid_size_values_small);
        }
    }
}
