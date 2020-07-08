package mt.edu.um.malteseaacapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

public class ImageObject implements Parcelable {
    private int id;
    private String word; // The word which must be spoken
    private String image; // The path for the image representing the word
    private String audio; // The path of the audio file corresponding to the word
    private String category; // The category of the word to be spoken
    private boolean hidden; // Determines whether the object will be shown in the grid

    /**
     * Constructor for image object. Sets the word, and category as obtained from database
     * @param context       The context
     * @param word          The word which must be spoken
     * @param category      The word category
     */
    public ImageObject(Context context, String word, String category) {
        setWord(word);
        setImage(context, word, category);
        setAudio(context, word, category);
        setCategory(category);
    }

    /**
     * Constructor for image object. Sets the word, category and whether it should be hidden as obtained from database
     * @param context       The context
     * @param word          The word which must be spoken
     * @param category      The word category
     * @param hidden        Whether the item should be hidden
     */
    public ImageObject(Context context, String word, String category, boolean hidden) {
        setWord(word);
        setImage(context, word, category);
        setAudio(context, word, category);
        setCategory(category);
        setHidden(hidden);
    }

    /**
     * Constructor for image object. Sets the word, category and whether it should be hidden as obtained from database
     * @param context       The context
     * @param id            The unique reference number
     * @param word          The word which must be spoken
     * @param category      The word category
     * @param hidden        Whether the item should be hidden
     */
    public ImageObject(Context context, int id, String word, String category, boolean hidden) {
        setId(id);
        setWord(word);
        setImage(context, word, category);
        setAudio(context, word, category);
        setCategory(category);
        setHidden(hidden);
    }

    protected ImageObject(Parcel in) {
        id = in.readInt();
        word = in.readString();
        image = in.readString();
        audio = in.readString();
        category = in.readString();
        hidden = in.readInt() == 1;
    }

    /**
     * Gets a unique reference number
     * @return The unique reference
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the unique reference number
     * @param id The unique reference number
     */
    private void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the word to be spoken
     * @return  Returns the word to be spoken
     */
    public String getWord() {
        return word;
    }

    /**
     * Sets the word to be spoken
     * @param word  The word to be spoken
     */
    private void setWord(String word) {
        this.word = word;
    }

    /**
     * Gets the path of the image representing the word to be spoken
     * @return  Returns the path of the image representing the word to be spoken
     */
    public String getImage() {
        return image;
    }

    /**
     * Sets the path of the image representing the word to be spoken
     * @param context   The context
     * @param word      The word to be spoken
     * @param category  The category to which the word pertains
     */
    private void setImage(Context context, String word, String category) {
        this.image = context.getFilesDir().getPath() + "/images/"
                + category + "/" + word + ".png";
    }

    /**
     * Gets the path of the audio file corresponding to the word to be spoken
     * @return  Returns the path of the audio file corresponding to the word to be spoken
     */
    public String getAudio() {
        return audio;
    }

    /**
     * Sets the path of the audio file corresponding to the word to be spoken
     * @param context   The context
     * @param word      The word to be spoken
     * @param category  The category to which the word pertains
     */
    private void setAudio(Context context, String word, String category) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.audio = context.getFilesDir().getPath() + "/audio/"
                + sharedPreferences.getString("voice", "female")
                + "/" + category + "/" + word + ".wav";
    }

    /**
     * Gets the category of the word to be spoken
     * @return  Returns the category of the word to be spoken
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category of the word to be spoken
     * @param category  The category of the word to be spoken
     */
    private void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns whether the item is hidden from view
     * @returns Whether the item is hidden from view
     */
    public boolean isHidden() { return hidden; }

    /**
     * Sets whether the item should be hidden from view
     * @param value Whether this item should be hidden or not
     */
    public void setHidden(boolean value) { hidden = value; }

    /****************************************
     *Implementation of "Parcelable" methods*
     ****************************************/

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(word);
        parcel.writeString(image);
        parcel.writeString(audio);
        parcel.writeString(category);
        parcel.writeInt(hidden ? 1 : 0);
    }

    public static final Creator<ImageObject> CREATOR = new Creator<ImageObject>() {
        @Override
        public ImageObject createFromParcel(Parcel in) {
            return new ImageObject(in);
        }

        @Override
        public ImageObject[] newArray(int size) {
            return new ImageObject[size];
        }
    };
}
