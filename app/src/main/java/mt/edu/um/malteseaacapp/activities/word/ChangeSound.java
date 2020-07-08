package mt.edu.um.malteseaacapp.activities.word;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.operations.AudioOps;
import mt.edu.um.malteseaacapp.operations.StorageOps;

public class ChangeSound extends AppCompatActivity {

    // Set request codes for various intents
    private final int EXPLORER_REQUEST_CODE = 0;

    // permission request codes
    private final int PERMISSION_ALL = 1;

    private TextView selectedAudioTextView; // The text view to show the name of selected audio
    private TextView isRecordingLabel;
    private ImageView micIcon;
    private ImageView playIcon;

    private byte[] selectedAudio;// To save to internal storage

    private String word; // Word being edited
    private String category; // Category of word being edited

    // for in-app recording
    private MediaRecorder audioRec;
    private MediaPlayer player;
    private boolean isPlayingPreview, wasPlaying;
    private String audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_sound);

        // Get word being edited and its category from intent
        word = getIntent().getStringExtra("Word");
        category = getIntent().getStringExtra("Category");

        player = new MediaPlayer();
        isPlayingPreview = false;
        wasPlaying = false;

        setUpLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopPlayer();
    }

    /**
     * Set up the layout of the "ChangeSound" activity (including onClickListeners)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setUpLayout()
    {
        // Find ProgressBar from layout and set colour
        final ProgressBar progressBar = this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources()
                        .getColor(R.color.colorPrimary),
                android.graphics.PorterDuff.Mode.MULTIPLY);

        // Find selectedAudio TextView from layout
        selectedAudioTextView = this.findViewById(R.id.selectedAudio);
        // Find recordingNotification TextView from layout
        isRecordingLabel = this.findViewById(R.id.recordingNotification);
        // Find micIcon ImageView from layout
        micIcon = this.findViewById(R.id.micIcon);
        // Find playIcon ImageView from layout
        playIcon = this.findViewById(R.id.playIcon);

        // Find views from layout and set their onClickListeners
        final ImageView folderIcon = this.findViewById(R.id.folderIcon);
        final Button okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);
        folderIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent findAudio = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                try {
                    startActivityForResult(findAudio, EXPLORER_REQUEST_CODE);

                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Sorry, no music app installed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        micIcon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(hasAudioPermission() && hasExternalStoragePermission()) {
                    int action = event.getAction();

                    if (action == MotionEvent.ACTION_DOWN) {
                        stopPlayer();

                        try {
                            // set path with
                            audioPath = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString().substring(0, 5) + "_audiorec.3gp";

                            startRec();
                            isRecordingLabel.setVisibility(View.VISIBLE);
                            micIcon.setImageResource(R.drawable.ic_stop);

                            // hide name and play button in case of re-recording audio
                            if (selectedAudioTextView.getVisibility() == View.VISIBLE) {
                                selectedAudioTextView.setVisibility(View.INVISIBLE);
                            }
                            if (playIcon.getVisibility() == View.VISIBLE) {
                                playIcon.setVisibility(View.INVISIBLE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        stopSaveRec();
                        isRecordingLabel.setVisibility(View.GONE);

                        micIcon.setImageResource(R.drawable.ic_mic);
                    }
                } else {
                    requestPermission();
                }
                return true;
            }
        });
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                isPlayingPreview = false;
                playIcon.setImageResource(R.drawable.ic_play);
            }
        });
        playIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPlayingPreview) {

                    // pause the player
                    player.pause();
                    playIcon.setImageResource(R.drawable.ic_play);
                    isPlayingPreview = false;

                } else {

                    try {

                        // set and prepare only if playing for the first time - allows for resuming on start() if it was previously paused
                        if (!wasPlaying) {

                            player.reset();
                            player.setDataSource(audioPath);
                            player.prepare();
                            player.setLooping(false);
                            wasPlaying = true;
                        }

                        isPlayingPreview = true;

                        player.start();

                        playIcon.setImageResource(R.drawable.ic_pause);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                stopPlayer();

                // Check if word audio was selected
                if (selectedAudio == null)
                {
                    Toast.makeText(getApplicationContext(),
                            "Please select a sound for the word.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update word audio
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Save new audio file
                        StorageOps.saveAudio(getApplicationContext(),
                                selectedAudio, category, word);
                        // Terminate activity when audio file is updated
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(player.isPlaying())
            player.stop();
        player.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case EXPLORER_REQUEST_CODE:
                if(resultCode == RESULT_OK)
                {
                    // Get audio from Uri and store in byte array
                    Uri selectedAudioUri = data.getData();
                    // If audio is already selected, remove it and request garbage collection
                    // to prevent out of memory exception
                    if (selectedAudio!=null)
                    {
                        selectedAudio =  null;
                        System.gc();
                    }
                    selectedAudio = AudioOps.getAudio(selectedAudioUri, getApplicationContext());
                    // Show name of selected audio
                    selectedAudioTextView.setText(AudioOps.getFileName(selectedAudioUri, this));
                    selectedAudioTextView.setVisibility(View.VISIBLE);

                    // instructs player to re-set data source of new recording
                    wasPlaying = false;
                }
                break;
        }
    }

    private void startRec() {
        audioRec = new MediaRecorder();
        audioRec.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        audioRec.setOutputFile(audioPath);

        try {
            audioRec.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Since a null check is used for selecting an audio, only declare selectedAudio to non null when we are guaranteed to have an audio
        selectedAudio = new byte[16384];
        audioRec.start();
    }

    private void stopSaveRec() {
        try {
            audioRec.stop();
        } catch (RuntimeException e) {
            Toast.makeText(getApplicationContext(), "Press and hold to record audio", Toast.LENGTH_SHORT).show();
        }
        audioRec.reset();
        audioRec.release();
        audioRec = null;

        try {
            File file = new File(audioPath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            for (int readNum; (readNum = fis.read(selectedAudio)) != -1; ) {
                bos.write(selectedAudio, 0, readNum);
            }

            // Close streams
            fis.close();
            bos.close();

            // Show name for in-app audio recording
            selectedAudioTextView.setText(audioPath.substring(audioPath.lastIndexOf("/") + 1));
            selectedAudioTextView.setVisibility(View.VISIBLE);
            playIcon.setVisibility(View.VISIBLE);

            // instructs player to re-set data source of new recording
            wasPlaying = false;

        } catch (Exception e) {
            e.printStackTrace();

            // error in loading audio means set selectedAudio[] back to null. This is to be able to re-trigger no audio validation
            selectedAudio = null;
        }

        System.gc(); // for mediarecorder
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED;
    }

    private boolean hasExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, PERMISSION_ALL);
    }

    private void stopPlayer() {
        if (player.isPlaying()) {
            player.stop();
            isPlayingPreview = false;

            // stopping needs to prepare again
            wasPlaying = false;

            playIcon.setImageResource(R.drawable.ic_play);
        }
    }
}
