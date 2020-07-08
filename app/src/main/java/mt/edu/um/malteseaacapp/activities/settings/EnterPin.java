package mt.edu.um.malteseaacapp.activities.settings;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import mt.edu.um.malteseaacapp.R;

public class EnterPin extends AppCompatActivity {

    private CancellationSignal fingerprintCancellation;
    private FingerprintManagerCompat fingerprintManager;
    private int triesLeft = 5;
    private TextView enterPinText;
    private EditText pinEditText;
    private Button okButton;

    private boolean isBiometricsSet;
    private String correctPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_pin);

        fingerprintCancellation = new CancellationSignal();
        fingerprintManager = FingerprintManagerCompat.from(this);

        correctPin = PreferenceManager.getDefaultSharedPreferences(this).getString("pin", "");
        isBiometricsSet = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fingerprint_set", false);

        setUpLayout();
    }

    @Override
    public void onBackPressed() {

        cancelBiometricReading();

        // Indicate that activity was cancelled (i.e. pin was not entered)
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

       /*
       * we will use cancellation signal to determine whether onResume is in first entry or if returning from onPause()
       */

        if (fingerprintCancellation.isCanceled() && triesLeft > 0) {

            // reset signal
            fingerprintCancellation = new CancellationSignal();

            // resume reading
            if (canReadFingerprint())
                authenticate(fingerprintManager, fingerprintCancellation);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        cancelBiometricReading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelBiometricReading();
    }

    /**
     * Set up the layout of the "EnterPin" activity (including onClickListeners)
     */
    private void setUpLayout()
    {
        enterPinText = this.findViewById(R.id.enterPinText);

        // Find views from layout and set onClickListeners
        pinEditText = this.findViewById(R.id.pinEditText);
        okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Get entered pin
                final String enteredPin = pinEditText.getText().toString();

                // Check if entered pin is correct
                if (enteredPin.equals(correctPin)) {

                    // If pin is correct indicate a successful result and terminate activity
                    setResult(RESULT_OK);
                    finish();

                } else {

                    // Indicate if the entered pin is wrong and clear text
                    Toast toast = Toast.makeText(getApplicationContext(), "Sorry, wrong pin.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();
                    pinEditText.setText("");
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        if (canReadFingerprint()) {

            // replace the message with that of fingerprint or pin entry
            enterPinText.setText(R.string.pin_prompt_fingerprint);

            authenticate(fingerprintManager, fingerprintCancellation);
        }
    }

    // since M is minimum for fingerprint
    private boolean isSdkVersionSupported() {

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    // cancels fingerprint reading
    private void cancelBiometricReading() {

        if (fingerprintCancellation != null && !fingerprintCancellation.isCanceled()) {
            fingerprintCancellation.cancel();
        }
    }

    // returns true if device is eligible to read fingerprint
    private boolean canReadFingerprint() {

        if (isBiometricsSet && isSdkVersionSupported() && fingerprintManager.isHardwareDetected()) {

            if (fingerprintManager.hasEnrolledFingerprints()) {

                return true;

            } else {

                Toast toast = Toast.makeText(getApplicationContext(), "No saved fingerprints found.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }
        }
        return false;
    }

    private void authenticate(FingerprintManagerCompat fingerprintManager, CancellationSignal fpc) {

        fingerprintManager.authenticate(null, 0, fpc, new FingerprintManagerCompat.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                super.onAuthenticationError(errMsgId, errString);
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                super.onAuthenticationHelp(helpMsgId, helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                pinEditText.setText(correctPin);

                okButton.performClick();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();

                Toast toast = Toast.makeText(getApplicationContext(), "Try again", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
                triesLeft--;

                // if all tries are used, cancel fingerprint and require PIN
                if (triesLeft == 0) {

                    cancelBiometricReading();

                    // switch back to manual pin entry
                    enterPinText.setText(R.string.pin_prompt);
                }
            }
        }, null);
    }
}
