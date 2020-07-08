package mt.edu.um.malteseaacapp.activities.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import mt.edu.um.malteseaacapp.R;

public class ShowRecovery extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_show_recovery);

        String title = getIntent().getStringExtra("title");
        String recovery_key = getIntent().getStringExtra("recovery_k");

        // set title
        TextView recoverTitle = findViewById(R.id.recoveryCodeTitle);
        recoverTitle.setText(title);

        // set recovery key
        TextView recoverKey = findViewById(R.id.recoveryCode);
        recoverKey.setText(recovery_key);

        final Button recoveryOkButton = this.findViewById(R.id.recoveryOkButton);

        recoveryOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
