package mt.edu.um.malteseaacapp.activities.word;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.javatuples.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.VariantManager;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.operations.AudioOps;
import mt.edu.um.malteseaacapp.operations.ImageOps;
import mt.edu.um.malteseaacapp.operations.StorageOps;
import mt.edu.um.malteseaacapp.operations.TextOps;

public class AddWord extends AppCompatActivity {
    // Set request codes for various intents
    private final int GALLERY_REQUEST_CODE = 0;
    private final int CAMERA_REQUEST_CODE = 1;
    private final int EXPLORER_REQUEST_CODE = 2;
    private final int SCRAPER_REQUEST_CODE = 4;

    // permission request code, android only requests those required
    private final int PERMISSION_ALL = 3;

    private ImageView selectedImageView; // The image view where the selected image will be shown
    private TextView selectedAudioTextView; // The text view to show the name of selected audio
    private TextView isRecordingLabel;
    private ImageView micIcon;
    private ImageView playIcon;

    // To save to internal storage
    private Bitmap selectedImageBitmap;
    private byte[] selectedAudio;

    private DatabaseAccess dbAccess; // To access database
    private VariantManager variantManager;
    private Collection<Pair<Integer, String>> normalisedWords;

    // for in-app recording
    private MediaRecorder audioRec;
    private MediaPlayer player;
    private boolean isPlayingPreview, wasPlaying;
    private String audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_word);

        // Get instance of DatabaseAccess and open connection
        dbAccess = DatabaseAccess.getInstance(getApplicationContext());
        dbAccess.open();

        // Get a list of all normalised words in database
        normalisedWords = dbAccess.getNormalisedWords();

        // Get instance of the VariantManager class
        variantManager = VariantManager.getInstance(this);

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
     * Set up the layout of the "AddWord" activity (including onClickListeners)
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setUpLayout() {
        // Get categories from database and put them in a list of strings
        final ArrayList<String> categoryNames = dbAccess.getCategoryNames();

        // Create adapter with categories to use with spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);

        // Put categories in spinner
        final Spinner categorySpinner = this.findViewById(R.id.categorySpinner);
        categorySpinner.setAdapter(spinnerAdapter);

        // Find EditText components from layout
        final EditText wordEditText = this.findViewById(R.id.wordEditText);
        final EditText wordEditRoot = this.findViewById(R.id.wordEditRoot);

        // Find ProgressBar from layout and set colour
        final ProgressBar progressBar = this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable()
                .setColorFilter(getResources()
                        .getColor(R.color.colorPrimary), android.graphics.PorterDuff.Mode.MULTIPLY);

        // Find image and audio components from the layout
        selectedImageView = this.findViewById(R.id.selectedImage);
        selectedAudioTextView = this.findViewById(R.id.selectedAudio);
        isRecordingLabel = this.findViewById(R.id.recordingNotification);
        micIcon = this.findViewById(R.id.micIcon);
        playIcon = this.findViewById(R.id.playIcon);

        // Find views from layout and set their onClickListeners
        final ImageView galleryIcon = this.findViewById(R.id.galleryIcon);
        final ImageView cameraIcon = this.findViewById(R.id.cameraIcon);
        final ImageView webIcon = this.findViewById(R.id.webIcon);
        final ImageView folderIcon = this.findViewById(R.id.folderIcon);
        final Button okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);

        galleryIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openGallery = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                try {
                    startActivityForResult(openGallery , GALLERY_REQUEST_CODE);

                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Sorry, no gallery app installed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        cameraIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    startActivityForResult(openCamera, CAMERA_REQUEST_CODE);

                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Sorry, no camera app installed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        webIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openWebScraper = new Intent(getApplicationContext(), ScrapeImage.class);
                startActivityForResult(openWebScraper, SCRAPER_REQUEST_CODE);
            }
        });

        folderIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent findAudio = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                try {
                    startActivityForResult(findAudio, EXPLORER_REQUEST_CODE);
                } catch (ActivityNotFoundException e)
                {
                    Toast.makeText(getApplicationContext(), "Sorry, no music app installed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        micIcon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (hasAudioPermission() && hasExternalStoragePermission()) {
                    int action = event.getAction();

                    if (action == MotionEvent.ACTION_DOWN) {
                        stopPlayer();

                        try {
                            // set path
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
                        isRecordingLabel.setVisibility(View.INVISIBLE);

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

                final String enteredWord = wordEditText.getText().toString();
                final String normalisedWord = TextOps.normalise(enteredWord);
                final String enteredRoot = TextOps.normalise(wordEditRoot.getText().toString());
                final String wordCategory = categorySpinner.getSelectedItem().toString();
                String rootCategory = null;
                int selectedRootId = 0;

                // Check if words already exists in the database
                for (Pair<Integer, String> p : normalisedWords) {
                    if (p.getValue1().equalsIgnoreCase(normalisedWord)) {
                        Toast.makeText(getApplicationContext(),
                                "Sorry, that word already exists",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // If a root was specified, check if it exists and retrieve its id and category
                if (!enteredRoot.isEmpty()) {
                    for (Pair<Integer, String> p : normalisedWords) {
                        int temp = p.getValue0();
                        if (p.getValue1().equalsIgnoreCase(enteredRoot) && !variantManager.isVariant(temp)) {
                            rootCategory = dbAccess.getCategoryName(temp);
                            selectedRootId = temp;
                            break;
                        }
                    }
                }

                // If a root was specified, check that a valid id has been retrieved
                if (!enteredRoot.isEmpty() && selectedRootId == 0) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry, that root does not exist, or is already a variant of another word",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if the root category and the word category match
                if (rootCategory != null && !rootCategory.equals(wordCategory)) {
                    Toast.makeText(getApplicationContext(),
                            "Sorry, root category and word category do not match",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if there are any categories to add the word to
                if (categoryNames.size() == 0) {
                    Toast.makeText(getApplicationContext(),
                            "No categories available. Please add a category first",
                            Toast.LENGTH_SHORT).show();
                    return;
                } // Check if word was left out by user
                else if (wordEditText.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),
                            "Please enter a word", Toast.LENGTH_SHORT).show();
                    return;
                } // Check if word image was selected
                else if (selectedImageView.getVisibility() != View.VISIBLE) {
                    Toast.makeText(getApplicationContext(),
                            "Please select an image for the word",
                            Toast.LENGTH_SHORT).show();
                    return;
                } // Check if word audio was selected
                else if (selectedAudio == null) {
                    Toast.makeText(getApplicationContext(),
                            "Please select a sound for the word",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                int finalRootId = selectedRootId;

                // Add new word
                new Thread(() -> {
                    // Save the new word's corresponding image
                    StorageOps.saveImage(getApplicationContext(), selectedImageBitmap,
                            categorySpinner.getSelectedItem().toString(), enteredWord);

                    // Save the new word's corresponding audio file
                    StorageOps.saveAudio(getApplicationContext(), selectedAudio,
                            categorySpinner.getSelectedItem().toString(), enteredWord);

                    // Insert new word in database
                    dbAccess.insertWord(enteredWord, normalisedWord,
                            categorySpinner.getSelectedItem().toString(), finalRootId);

                    // Update variants cache if necessary
                    if (finalRootId != 0) {
                        variantManager.addVariant(finalRootId, dbAccess.getId(enteredWord));
                    }

                    // Terminate activity when word is inserted
                    setResult(RESULT_OK);
                    finish();
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
            case GALLERY_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    // If image is already selected, remove it and request garbage collection
                    // to prevent out of memory exception
                    if (selectedImageBitmap != null) {
                        selectedImageBitmap =  null;
                        System.gc();
                    }
                    try {
                        // Get selected image bitmap
                        selectedImageBitmap = ImageOps.decodeDownSampledBitamp(selectedImage,
                                getApplicationContext(),
                                (int) getResources().getDimension(R.dimen.selectedImage),
                                (int) getResources().getDimension(R.dimen.selectedImage));

                        // Set selectedImageView to visible
                        selectedImageView.setVisibility(View.VISIBLE);

                        // Load selected image into ImageView
                        Glide.with(this)
                                .load(selectedImageBitmap)
                                .into(selectedImageView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CAMERA_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    // If image is already selected, remove it and request garbage collection
                    // to prevent out of memory exception
                    if (selectedImageBitmap != null)
                    {
                        selectedImageBitmap = null;
                        System.gc();
                    }
                    try {
                        // Get captured image bitmap
                        selectedImageBitmap = (Bitmap) data.getExtras().get("data");

                        // Find selectedImageView from layout and set to visible
                        ImageView selectedImageView
                                = this.findViewById(R.id.selectedImage);
                        selectedImageView.setVisibility(View.VISIBLE);

                        // Load selected image into ImageView
                        Glide.with(this)
                                .load(selectedImageBitmap)
                                .into(selectedImageView);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
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
                    // Show name of audio file
                    selectedAudioTextView.setText(AudioOps.getFileName(selectedAudioUri, this));

                    selectedAudioTextView.setVisibility(View.VISIBLE);
                    playIcon.setVisibility(View.VISIBLE);

                    // instructs player to re-set data source of new recording
                    wasPlaying = false;
                }
                break;
            case SCRAPER_REQUEST_CODE:
                if (resultCode == RESULT_OK) {

                    if (selectedImageBitmap != null) {
                        selectedImageBitmap = null;
                        System.gc();
                    }
                    try {

                        byte[] imageTemp = data.getByteArrayExtra("imagebytes");
                        selectedImageBitmap = BitmapFactory.decodeByteArray(imageTemp, 0, imageTemp.length);

                        // Find selectedImageView from layout and set to visible
                        ImageView selectedImageView = this.findViewById(R.id.selectedImage);
                        selectedImageView.setVisibility(View.VISIBLE);

                        // Load selected image into ImageView
                        Glide.with(this).load(selectedImageBitmap).into(selectedImageView);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

        System.gc(); // for media recorder
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED;
    }

    private boolean hasExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[] {
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
