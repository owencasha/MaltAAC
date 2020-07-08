package mt.edu.um.malteseaacapp.activities.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Random;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.Home;

public class ResetPin extends AppCompatActivity {

    private EditText enterCode;
    private Button resetButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_pin);

        enterCode = this.findViewById(R.id.codeText);
        resetButton = this.findViewById(R.id.resetButton);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // get stored recovery code
                String recoveryCode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("recovery_code", "");

                SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

                String recoveryEntered = enterCode.getText().toString();

                if (recoveryEntered.equals(recoveryCode)) {

                    // reset pin
                    prefEditor.remove("pin").apply();

                    // generate new recovery code
                    final String newRecoveryCode = generateRecoveryCode(6);

                    // store it in preferences
                    prefEditor.putString("recovery_code", newRecoveryCode).apply();

                    // go back
                    setResult(RESULT_OK);
                    finish();

                } else {

                    Toast toast = Toast.makeText(getApplicationContext(), "Wrong recovery code. Please try again.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();

                    enterCode.setText("");
                }
            }
        });
    }

    public static String generateRecoveryCode(int n) {

        final char[] AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz".toCharArray();

        StringBuilder sb = new StringBuilder(n);
        Random rnd = new Random();

        for (int i = 0; i < n; i++)
            sb.append(AlphaNumericString[rnd.nextInt(AlphaNumericString.length)]);

        return sb.toString();
    }
}
