package mt.edu.um.malteseaacapp;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Predictions implements Serializable{
    // Declare HashMaps to contain n-grams (for next word prediction)
    private HashMap<String, HashMap<String, Double>> bigrams;
    private HashMap<String, HashMap<String, Double>> trigrams;

    // Prefix on which to base next word predictions
    private String prefix;

    public Predictions(Context context)
    {
        // Load n-grams from serialisable files
        loadNGrams(context);
    }

    /**
     * Load n-grams from serialisable files into HashMaps
     * @param c The context from where to get files directory to read serialisable files
     */
    private void loadNGrams(Context c)
    {
        final Context context = c;
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fileInputStream;
                ObjectInputStream objectInputStream;
                try
                {
                    // Load serialisable HashMap containing bigrams
                    fileInputStream = new FileInputStream(
                            new File(context.getFilesDir().getPath() +
                            "/predictions/bigrams.ser"));
                    objectInputStream = new ObjectInputStream(fileInputStream);
                    bigrams = (HashMap) objectInputStream.readObject();
                    objectInputStream.close();
                    fileInputStream.close();
                    // Load serialisable HashMap containing trigrams
                    fileInputStream = new FileInputStream(
                            new File(context.getFilesDir().getPath() +
                                    "/predictions/trigrams.ser"));
                    objectInputStream = new ObjectInputStream(fileInputStream);
                    trigrams = (HashMap) objectInputStream.readObject();
                    objectInputStream.close();
                    fileInputStream.close();
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Get the most appropriate n-grams HashMap to use for making predictions based on the image
     * selection shown in the imageSelectionFragment. The already selected images will be set as
     * the prefix. Start by checking for predictions based on the highest order n-grams and back
     * off to lower order n-grams if no predictions are available for that order of n.
     * @param imageObjects The selected image objects (obtained from the imageSelectionFragment)
     * @return The HashMap containing the most appropriate set of n-grams for making predictions
     */
    public HashMap<String, HashMap<String, Double>> getPredictionsNGrams(
            ArrayList<ImageObject> imageObjects)
    {
        // Set preceding words to null
        String p2, p1;

        // Set preceding words based on the total number of words entered
        if (imageObjects.size() == 1)
        {
            p1 = imageObjects.get(imageObjects.size()-1).getWord();
            // Check whether their exist any bigrams starting with the given prefix
            if (bigrams.containsKey(p1))
            {
                prefix = p1;
                return bigrams;
            } else
            {
                // If no predictions are found, return null
                return null;
            }
        } else if (imageObjects.size() >= 2)
        {
            p1 = imageObjects.get(imageObjects.size()-2).getWord();
            p2 = imageObjects.get(imageObjects.size()-1).getWord();
            // Check whether their exist any trigrams starting with the given prefix.
            // If no trigrams are found, back off to bigrams.
            if (trigrams.containsKey(p1 + " " + p2))
            {
                prefix = p1 + " " + p2;
                return trigrams;
            }
            else if (bigrams.containsKey(p2))
            {
                prefix = p2;
                return bigrams;
            } else
            {
                // If no predictions are found, return null
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the prefix on which to base next word predictions
     * @return Returns the prefix
     */
    public String getPrefix() {
        return prefix;
    }
}
