package mt.edu.um.malteseaacapp.activities.word;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.operations.ImageOps;
import mt.edu.um.malteseaacapp.operations.StorageOps;

public class ChangeImage extends AppCompatActivity {

    // Set request codes for various intents
    private static final int GALLERY_REQUEST_CODE = 0;
    private static final int CAMERA_REQUEST_CODE = 1;
    private final int SCRAPER_REQUEST_CODE = 2;

    ImageView selectedImageView; // The image view where the selected image will be shown
    Bitmap selectedImageBitmap; // To save to internal storage

    String word; // Word being edited
    String category; // Category of word being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_image);

        // Get word being edited and its category from intent
        word = getIntent().getStringExtra("Word");
        category = getIntent().getStringExtra("Category");

        setUpLayout();
    }

    /**
     * Set up the layout of the "ChangeImage" activity (including onClickListeners)
     */
    private void setUpLayout()
    {
        // Find ProgressBar from layout and set colour
        final ProgressBar progressBar = this.findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources()
                        .getColor(R.color.colorPrimary),
                android.graphics.PorterDuff.Mode.MULTIPLY);

        // Find selectedImageView from layout
        selectedImageView = this.findViewById(R.id.selectedImage);

        // Find views from layout and set their onClickListeners
        final ImageView galleryIcon = this.findViewById(R.id.galleryIcon);
        final ImageView cameraIcon = this.findViewById(R.id.cameraIcon);
        final ImageView webIcon = this.findViewById(R.id.webIcon);
        final Button okButton = this.findViewById(R.id.okButton);
        final Button cancelButton = this.findViewById(R.id.cancelButton);
        galleryIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openGallery = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                try{
                    startActivityForResult(openGallery , GALLERY_REQUEST_CODE);
                } catch (ActivityNotFoundException e)
                {
                    Toast.makeText(getApplicationContext(), "Sorry, no gallery app installed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        cameraIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try{
                    startActivityForResult(openCamera, CAMERA_REQUEST_CODE);
                } catch (ActivityNotFoundException e)
                {
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
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if word image was selected
                if (selectedImageView.getVisibility() != View.VISIBLE) {
                    Toast.makeText(getApplicationContext(),
                            "Please select an image for the word.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Change word image
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Save new image
                        StorageOps.saveImage(getApplicationContext(), selectedImageBitmap,
                                category, word);
                        // Terminate activity when word image is changed
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case GALLERY_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    // If image is already selected, remove it and request garbage collection
                    // to prevent out of memory exception
                    if (selectedImageBitmap!=null)
                    {
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
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                break;
            case CAMERA_REQUEST_CODE:
                if(resultCode == RESULT_OK){
                    // If image is already selected, remove it and request garbage collection
                    // to prevent out of memory exception
                    if (selectedImageBitmap!=null)
                    {
                        selectedImageBitmap =  null;
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
}
