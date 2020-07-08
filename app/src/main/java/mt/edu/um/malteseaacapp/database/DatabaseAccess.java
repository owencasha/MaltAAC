package mt.edu.um.malteseaacapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mt.edu.um.malteseaacapp.ImageObject;

public class DatabaseAccess {
    private final String TABLE_WORDS = "Unigrams";
    private final String TABLE_CATEGORIES = "Kategoriji";

    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;

    /**
     * Private constructor to prevent outside classes to create instances (Singleton pattern)
     * @param context The context
     */
    private DatabaseAccess(Context context) {
        this.openHelper = new DatabaseHelper(context);
    }

    /**
     * Create a single instance of DatabaseAccess (Singleton Pattern)
     * @param context The Context
     * @return Returns the only instance of DabaseAccess
     */
    public static DatabaseAccess getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    /**
     * Open database connection.
     */
    public void open() {
        database = openHelper.getWritableDatabase();
    }

    /**
     * Close database connection.
     */
    public void close() {
        if (database != null) {
            database.close();
        }
    }

    /**
     * Queries the Kategoriji table for all items
     * @return Returns a cursor containing all records from the Kategoriji table
     */
    public Cursor getKategoriji() {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters
        String table = "Kategoriji";
        String[] columns = {"_id", "Word", "Category", "IsHidden"};
        String orderBy = "Word COLLATE UNICODE ASC";

        // Execute query
        return database.query(table, columns, null, null,
                null, null, orderBy);
    }

    /**
     * Queries the Unigrams table for all items of the specified category
     * @param category The category of items to obtain
     * @param excludeVariants Whether variants of the same word should be excluded from the result
     * @return Returns a cursor containing all the items pertaining to the specified category
     */
    public Cursor getUnigrams(String category, boolean excludeVariants) {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters
        String table = "Unigrams";
        String columns[] = {"_id", "Word", "Category", "IsHidden"};
        String selection = excludeVariants ? "Category = ? AND Root IS NULL" : "Category = ?";
        String selectionArgs[] = {category};
        String orderBy = "Word COLLATE UNICODE ASC";

        // Execute query
        return database.query(table, columns, selection, selectionArgs, null, null, orderBy);
    }

    /**
     * Queries the Unigrams table for category of the word
     * @param wordId The ID of the word for which the category is needed
     * @return Returns the name of the word's category
     */
    public String getCategoryName(int wordId) {
        // Open database if not already open
        if (!database.isOpen())
            open();

        final String categoryName;

        // Set query parameters
        String table = "Unigrams";
        String columns[] = {"Category"};
        String selection = "_id = ?";
        String selectionArgs[] = {Integer.toString(wordId)};

        // Execute query and load result into cursor
        Cursor cursor =  database.query(table, columns, selection, selectionArgs, null, null, null);

        cursor.moveToFirst();
        categoryName = cursor.getString(0);
        cursor.close();

        return categoryName;
    }

    /**
     * Queries the Unigrams table for the category of the specified word
     * and returns the corresponding ImageObject
     * @param context   The context
     * @param word      The word whose corresponding ImageObject to obtain
     * @return          Returns an ImageObject
     */
    public ImageObject getImageObject(Context context, String word) {
        // Open database if not already open
        if (!database.isOpen())
            open();

        ImageObject imageObject = null;
        String category;
        boolean hidden;
        int id;

        // Set query parameters
        String table = "Unigrams";
        String columns[] = {"_id", "Category", "IsHidden"};
        String selection = "Word = ?";
        String selectionArgs[] = {word};

        // Obtain category
        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);
        while (cursor.moveToNext()) {
            id = cursor.getInt(0);
            category = cursor.getString(1);
            hidden = cursor.getInt(2) == 1;
            imageObject = new ImageObject(context, id, word, category, hidden);
        }

        cursor.close();
        return imageObject;
    }

    /**
     * Gets the normalised names of all the categories existing in the "Kategoriji" table
     * @return  Returns an ArrayList containing the normalised
     *          names of all categories in th e"Kategoriji" table
     */
    public ArrayList<String> getNormalisedCategoryNames() {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Initialise ArrayList to hold category names
        ArrayList<String> categoryNames = new ArrayList<>();

        // Set query parameters
        String table = "Kategoriji";
        String columns[] = {"NormalisedWord"};

        // Obtain category names from table
        Cursor cursor = database.query(table, columns, null, null,
                null, null, null);
        // Continously read items from the table
        while (cursor.moveToNext()) {
            categoryNames.add(cursor.getString(0));
        }

        cursor.close();
        return categoryNames;
    }

    /**
     * Gets the names of all the categories existing in the "Kategoriji" table
     * @return Returns an ArrayList containing the names of all categories in the "Kategoriji" table
     */
    public ArrayList<String> getCategoryNames() {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Initialise ArrayList to hold category names
        ArrayList<String> categoryNames = new ArrayList<>();

        // Set query parameters
        String table = "Kategoriji";
        String columns[] = {"Word"};

        // Obtain category names from table
        Cursor cursor = database.query(table, columns, null, null,
                null, null, null);
        // Continously read items from the table
        while (cursor.moveToNext()) {
            categoryNames.add(cursor.getString(0));
        }

        cursor.close();
        return categoryNames;
    }

    /**
     * Gets the normalised words found in the "Unigrams" table
     * @return Returns a Collection containing pairs of normalised words and their ids from the "Unigrams" table
     */
    public Collection<Pair<Integer, String>> getNormalisedWords() {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Initialise ArrayList to hold normalised words
        ArrayList<Pair<Integer, String>> result = new ArrayList<>();

        // Set query parameters
        String table = "Unigrams";
        String[] columns = {"_id", "NormalisedWord", "Root"};

        // Obtain normalised words from table
        Cursor cursor = database.query(table, columns, null, null, null, null, null);

        // Continuously read items from the table
        while (cursor.moveToNext()) {
            result.add(new Pair<>(cursor.getInt(0), cursor.getString(1)));
        }

        cursor.close();

        return result;
    }

    /**
     * Insert a new category record in the "Kategoriji" table
     * @param categoryName           The name of the new category
     * @param normalisedCategoryName The normalised name of the new category
     */
    public void insertCategory(String categoryName, String normalisedCategoryName) {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters
        String table = "Kategoriji";
        ContentValues contentValues = new ContentValues();
        contentValues.put("Word", categoryName);
        contentValues.put("NormalisedWord", normalisedCategoryName);
        contentValues.put("Category", "Kategoriji");

        // Insert new record with the provided details
        database.insert(table, null, contentValues);
    }

    /**
     * Insert a new word in the "Unigrams" table
     * @param word           The new word
     * @param normalisedWord The normalised version of the new word
     * @param category       The category to which the new word corresponds
     * @param rootId         The root id of the new word
     */
    public void insertWord(String word, String normalisedWord, String category, int rootId) {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters
        String table = "Unigrams";
        ContentValues contentValues = new ContentValues();
        contentValues.put("Word", word);
        contentValues.put("NormalisedWord", normalisedWord);
        contentValues.put("Category", category);

        if (rootId > 0 ) {
            contentValues.put("Root", rootId);
        }

        // Insert new record with the provided details
        database.insert(table, null, contentValues);
    }

    /**
     * Updates the specified category name in the "Kategoriji" table
     * and updates the category field for all words associated with
     * the specified category in the "Unigrams" table
     * @param currentWord       Old category name
     * @param newWord           New category name
     * @param newNormalisedWord Normalised category name
     */
    public void editCategoryName(String currentWord, String newWord, String newNormalisedWord) {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters for changing the category name
        String table = "Kategoriji";
        ContentValues contentValues = new ContentValues();
        contentValues.put("Word", newWord);
        contentValues.put("NormalisedWord", newNormalisedWord);
        String whereClause = "Word = ?";
        String[] whereArgs = {currentWord};
        // Update category name
        database.update(table, contentValues, whereClause, whereArgs);

        // Set query parameters for changing the category attribute
        // of words which were associated with the old category name
        table = "Unigrams";
        contentValues = new ContentValues();
        contentValues.put("Category", newWord);
        whereClause = "Category = ?";
        // Update category attribute
        database.update(table, contentValues, whereClause, whereArgs);
    }

    /**
     * Deletes the specified category from the "Kategoriji" table and
     * deletes all words associated with the specified category
     * @param categoryName  The name of the category to delete
     */
    public void deleteCategory(String categoryName)
    {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters for deleting the category
        String table = "Kategoriji";
        String whereClause = "Word = ?";
        String[] whereArgs = {categoryName};
        // Delete category
        database.delete(table, whereClause, whereArgs);

        // Set query parameters for deleting words associated with category
        table = "Unigrams";
        whereClause = "Category = ?";
        whereArgs[0] = categoryName;
        // Delete category
        database.delete(table, whereClause, whereArgs);
    }

    /**
     * Updates the specified word in the "Unigrams" table
     * @param oldWord           The old word
     * @param newWord           The new word
     * @param newNormalisedWord The normalised version of the new word
     * @param newRootId         The new root id
     */
    public void changeWord(String oldWord, String newWord, String newNormalisedWord, int newRootId) {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Set query parameters for changing the word
        ContentValues contentValues = new ContentValues();
        contentValues.put("Word", newWord);
        contentValues.put("NormalisedWord", newNormalisedWord);

        if (newRootId > 0) {
            contentValues.put("Root", newRootId);
        } else {
            contentValues.putNull("Root");
        }

        // Update word
        database.update(TABLE_WORDS, contentValues, "Word = ?", new String[] {oldWord});
    }

    /**
     * Changes the category attribute of the specified word
     * @param word      The specified word
     * @param category  The new category to which the word will correspond
     */
    public void changeCategory(String word, String category)
    {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters for changing the word category
        String table = "Unigrams";
        ContentValues contentValues = new ContentValues();
        contentValues.put("Category", category);
        String whereClause = "Word = ?";
        String[] whereArgs = {word};
        // Update word category
        database.update(table, contentValues, whereClause, whereArgs);
    }

    /**
     * Deletes the specified word from the "Unigrams" table
     * @param word  The specified word to delete
     */
    public void deleteWord(String word)
    {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters for deleting the category
        String table = "Unigrams";
        String whereClause = "Word = ?";
        String[] whereArgs = {word};
        // Delete word
        database.delete(table, whereClause, whereArgs);
    }

    /**
     * Sets the IsHidden column to true for the specified category
     * @param category The category to hide
     */
    public boolean hideCategory(String category) {
        return hide(category, TABLE_CATEGORIES);
    }

    /**
     * Sets the IsHidden column to true for the specified word
     * @param word The word to hide
     */
    public boolean hideWord (String word) {
        return hide(word, TABLE_WORDS);
    }

    private boolean hide(String word, String table) {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Set query parameters
        String where = "Word = ?";
        String[] arg = {word};

        ContentValues values = new ContentValues();
        values.put("IsHidden", 1);

        // Update row
        return database.update(table, values, where, arg) == 1;
    }

    public int getId(String word) {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Set query parameters
        String[] columns = {"ROWID"};
        String[] args = {word};

        // Send query
        try (Cursor cursor = database.query(TABLE_WORDS, columns, "Word = ?", args, null, null, null)) {
            return cursor.moveToNext() ? cursor.getInt(0) : 0;
        }
    }

    public String getWord(int id) {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Set query parameters
        String[] columns = {"Word"};
        String[] args = {String.valueOf(id)};

        // Send query
        try (Cursor cursor = database.query(TABLE_WORDS, columns, "ROWID = ?", args, null, null, null)) {
            return cursor.moveToNext() ? cursor.getString(0) : null;
        }
    }

    public Cursor getAllVariants() {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Set query parameters
        String table = "Unigrams";
        String[] columns = {"_id", "Root"};
        String where = "Root NOT NULL";

        // Send query
        return database.query(table, columns, where, null, null, null, null);
    }

    public Cursor getVariants(int id) {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        // Set query parameters
        String table = "Unigrams";
        String[] columns = {"_id", "Word", "Category", "IsHidden"};
        String where = "_id = ? OR Root = ?";
        String param = String.valueOf(id);
        String[] arg = {param, param};

        // Send query
        return database.query(table, columns, where, arg, null, null, null);
    }

    public boolean hasCoreUnigrams() {
        try (Cursor cursor = getCoreUnigrams()) {
            return cursor.getCount() > 0;
        }
    }

    public Cursor getCoreUnigrams() {
        // Open database if not already open
        if (!database.isOpen())
            open();

        // Set query parameters
        String table = "Unigrams";
        String[] columns = {"_id", "Word", "Category", "IsHidden"};
        String selection = "IsCore = ?";
        String[] selectionArgs = {"1"};
        String orderBy = "Word COLLATE UNICODE ASC";

        // Send query
        return database.query(table, columns, selection, selectionArgs, null, null, orderBy);
    }

    public ArrayList<String> getHiddenWords() {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        ArrayList<String> words = new ArrayList<>();

        // Set query parameters
        String table = "Unigrams";
        String[] columns = {"Word"};
        String where = "IsHidden = ?";
        String[] arg = {"1"};
        String order = "Word ASC";

        // Send query
        Cursor cursor = database.query(table, columns, where, arg, null, null, order);

        // Continuously read items from the table
        while (cursor.moveToNext()) {
            words.add(cursor.getString(0));
        }

        cursor.close();

        return words;
    }

    public ArrayList<String> getHiddenCategories() {
        // Open database if not already open
        if (!database.isOpen()) {
            open();
        }

        ArrayList<String> categories = new ArrayList<>();

        // Set query parameters
        String table = "Kategoriji";
        String[] columns = {"Word"};
        String where = "IsHidden = ?";
        String[] arg = {"1"};
        String order = "Word ASC";

        // Send query
        Cursor cursor = database.query(table, columns, where, arg, null, null, order);

        // Continuously read items from the table
        while (cursor.moveToNext()) {
            categories.add(cursor.getString(0));
        }

        cursor.close();

        return categories;
    }

    public boolean showWords(List<String> words) {
        return show(words, "Unigrams");
    }

    public boolean showCategories(List<String> categories) {
        return show(categories, "Kategoriji");
    }

    private boolean show(List<String> words, String table) {
        if (words != null && words.size() > 0) {
            // Open database if not already open
            if (!database.isOpen()) {
                open();
            }

            database.beginTransaction();

            try {
                String where = "Word = ?";
                String[] args = new String[1];

                ContentValues values = new ContentValues();
                values.put("IsHidden", 0);

                for (String s : words) {
                    args[0] = s;

                    if (database.update(table, values, where,  args) != 1) {
                        return false;
                    }
                }

                database.setTransactionSuccessful();
                return true;
            }
            finally {
                database.endTransaction();
            }
        }

        return false;
    }
}