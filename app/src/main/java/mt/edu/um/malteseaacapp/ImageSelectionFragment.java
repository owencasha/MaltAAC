package mt.edu.um.malteseaacapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.transitionseverywhere.Explode;
import com.transitionseverywhere.TransitionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class ImageSelectionFragment extends Fragment {

    private ArrayList<ImageObject> imageObjects; // List containing all selected image objects
    private HorizontalScrollView horizontalScrollView; // To allow horizontal scrolling of images
    private LinearLayout linearLayout; // To hold clicked image buttons
    private ImageButton playButton; // To play sounds corresponding to selected images
    private boolean reloading; // To check whether images are being reloaded back
    private boolean playing;
    private int backgroundColor;

    public ImageSelectionFragment() {
        // Required empty public constructor

        // Initialise empty array list
        imageObjects = new ArrayList<>();
    }

    // Interface to be implemented by Home activity
    public interface OnSelectionChangedListener {
        // When an image is selected or deleted, the updated array list is passed as an argument
        void onSelectionChanged(ArrayList<ImageObject> imageObjects);
    }
    OnSelectionChangedListener callback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            backgroundColor = savedInstanceState.getInt("SelectionColor");
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            backgroundColor = sharedPreferences.getInt("selection_color", 0x2196F3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Reloading of images is not taking place at first
        reloading = false;

        // Inflate layout for fragment
        View view = inflater.inflate(R.layout.fragment_image_selection, container, false);

        // Set linear layout according to the layout specified in XML
        linearLayout = view.findViewById(R.id.imageSelection);

        // Initialise horizontal scroll view
        horizontalScrollView = view.findViewById(R.id.horizontalScrollView);

        // Set the background color
        Drawable drawable = DrawableCompat.wrap(horizontalScrollView.getBackground());
        DrawableCompat.setTint(drawable, backgroundColor);
        horizontalScrollView.setBackground(drawable);

        // Initialise play button and set onClickListener
        playButton = view.findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButtonClicked();
            }
        });

        // Restore images in linear layout if available
        if (savedInstanceState != null)
        {
            // Indicate that image selections are being loaded back
            reloading = true;
            ArrayList<ImageObject>  retrievedImageObjects
                    = savedInstanceState.getParcelableArrayList("ImageObjects");
            // Add each ImageObject to the selection
            for (int i = 0; i < retrievedImageObjects.size(); i++)
            {
                addImageSelection(retrievedImageObjects.get(i),
                        getActivity().getApplicationContext());
            }
            // Indicate that reloading has stopped
            reloading = false;
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Set callback to Home activity
        callback = (OnSelectionChangedListener) activity;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save selected image objects
        outState.putParcelableArrayList("ImageObjects",imageObjects);
        outState.putInt("SelectionColor", backgroundColor);
    }

    /**
     * Method called when the play button is clicked.
     * 1) Goes through all selected images and plays their corresponding sound.
     * 2) Clears image selection on completion if that is the user's preference.
     */
    private void playButtonClicked() {
        // Animate tapping on play button
        Animation bounce = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.scale);
        bounce.setInterpolator(new DecelerateInterpolator());
        playButton.startAnimation(bounce);

        // Check if there are any images selected or if playback is already in progress
        if (imageObjects.size() == 0 || playing) {
            return;
        }

        /* Add sentence to:
         1) Text document with history of entered sentences
         2) Text document with sentences which are yet to be added to the corpus */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String sentence = "";
                    // Go through all selected image objects and
                    // add the corresponding word to the sentence
                    for (ImageObject imageObject: imageObjects)
                    {
                        sentence = sentence.concat(imageObject.getWord());
                        sentence = sentence.concat(" ");
                    }
                    // Add sentence to text document with history of entered sentences
                    String filePath = getActivity().getApplicationContext()
                            .getFilesDir().getPath() + "/predictions/history.txt";
                    FileOutputStream fileOutputStream
                            = new FileOutputStream(new File(filePath), true);
                    OutputStreamWriter outputStreamWriter
                            = new OutputStreamWriter(fileOutputStream, "UTF-8");
                    outputStreamWriter.append(sentence);
                    outputStreamWriter.append("\r\n");
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    // Add sentence to text document with sentences
                    // which are yet to be added to the corpus
                    String corpusSentence = "<s> " + sentence + "</s>";
                    filePath = getActivity().getApplicationContext()
                            .getFilesDir().getPath() + "/predictions/add_to_corpus.txt";
                    fileOutputStream = new FileOutputStream(new File(filePath), true);
                    outputStreamWriter
                            = new OutputStreamWriter(fileOutputStream, "UTF-8");
                    outputStreamWriter.append(corpusSentence);
                    outputStreamWriter.append("\r\n");
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();

        // Play the sounds corresponding to the selected images starting from index 0
        playSound(0);
        playing = true;
    }

    /**
     * Plays the sound corresponding to the selected image view, and upon finishing
     * makes a recursive call to play the next sound
     * @param i The ArrayList index of the word whose sound will be spoken
     */
    private void playSound(final int i) {
        // If the end of the imageObjects list has not been reached
        if (i < imageObjects.size()) {
            // To play sounds
            final MediaPlayer mediaPlayer = new MediaPlayer();
            // To highlight spoken word
            final View selectedWord = linearLayout.getChildAt(i);
            // Set the view's background color to indicate that it will be spoken next
            selectedWord.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            // Scroll to the word being spoken
            horizontalScrollView.post(new Runnable() {
                @Override
                public void run() {
                    horizontalScrollView.scrollTo(selectedWord.getLeft(),0);
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Play sound corresponding to selected image
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(imageObjects.get(i).getAudio());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mediaPlayer.stop();
                                mediaPlayer.release();
                                // Remove background on view whose word was being spoken
                                selectedWord.setBackgroundColor(getResources().getColor(R.color.transparent));
                                // Play the next sound when completed
                                playSound(i + 1);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            // Clear selection if that is the user's preference
            if (PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean("clear_image_selection", true)) {
                imageObjects.clear();
                linearLayout.removeAllViews();
                callback.onSelectionChanged(imageObjects);
            }

            playing = false;
        }
    }

    /**
     * Add the clicked image button to the linear layout
     * @param imageObject   The image object to be added to the linear layout
     */
    public void addImageSelection(final ImageObject imageObject, Context context)
    {
        // Add selected object to array list
        imageObjects.add(imageObject);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View imageButtonView = layoutInflater.inflate(R.layout.image_button_layout, null);

        // Find image button layout from xml
        final ImageView imageSelection = imageButtonView.findViewById(R.id.imageButton);

        // Update image text with word corresponding to the image
        final TextView imageText = imageButtonView.findViewById(R.id.imageText);
        imageText.setText(imageObject.getWord());

        if (ColorUtils.calculateLuminance(backgroundColor) > 0.4)
            imageText.setTextColor(getResources().getColor(R.color.black));
        else
            imageText.setTextColor(getResources().getColor(R.color.white));

        // Global layout listener is called when the layout state changes,
        // at which point the dimensions of the linear layout will be available
        // thus, the size of the images can be set to make them fit neatly.
        imageSelection.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageSelection.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                // Set image width and height equal to 70%
                // of the available height in the LinearLayout
                float width, height;
                height = linearLayout.getHeight() * 0.7f;
                width = height;
                imageSelection.getLayoutParams().width = (int) width;
                imageSelection.getLayoutParams().height = (int) height;
            }
        });

        // Get image path from image object and set as image resource using Glide
        // The signature is set so that when the path of the image changes, the new resource
        // is loaded as opposed to the image stored in Glide's cache
        String path = imageObject.getImage();
        Glide.with(context)
                .load(path).apply(new RequestOptions()
                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis()))))
                .into(imageSelection)
                .clearOnDetach();

        // Animate addition to image selection linear layout
        TransitionManager.beginDelayedTransition(linearLayout,new Explode().setDuration(150));
        linearLayout.addView(imageButtonView);
        linearLayout.refreshDrawableState();

        // Speak selected word if that is the user's preference
        if (PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean("speak_selected_word", true) && !reloading) {
            try {
                // Play sound corresponding to selected image
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(imageObject.getAudio());
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Scroll to the end of the HorizontalScrollView when new word is added
        horizontalScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                horizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        },150);

        // Pass image object list to main activity through
        // interface (to be passed to ImageGridFragment)
        if (imageObjects != null && !reloading)
            callback.onSelectionChanged(imageObjects);
    }

    /**
     * Remove the last entered image from the image selection linear layout
     */
    public void removeImageSelection()
    {
        // Get last entered element
        View lastEnteredView = linearLayout.getChildAt(linearLayout.getChildCount()-1);
        // Remove last entered element
        linearLayout.removeView(lastEnteredView);

        // Remove from array list as well
        int lastElementIndex = imageObjects.size()-1;
        // Check if all elements have been deleted before removing
        if (lastElementIndex >= 0)
            imageObjects.remove(lastElementIndex);
        // Pass image object list to main activity through
        // interface (to be passed to ImageGridFragment)
        if(imageObjects != null && !reloading)
            callback.onSelectionChanged(imageObjects);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
    }

    /**
     * Get list of currently selected ImageObjects
     * @return  ArrayList containing the currently selected ImageObjects
     */
    public ArrayList<ImageObject> getImageObjects() {
        return imageObjects;
    }
}