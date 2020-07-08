package mt.edu.um.malteseaacapp;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import mt.edu.um.malteseaacapp.database.DatabaseHelper;

public class SearchSuggestionsProvider extends ContentProvider {

    // Define Authority and Content URI
    static final String AUTHORITY = "mt.edu.um.malteseaacapp";

    // Create UriMatcher definitions
    static final int UNIGRAMS = 1;
    static final int UNIGRAM_ID = 2;
    static final UriMatcher URI_MATCHER;
    static{
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, "Unigrams", UNIGRAMS);
        URI_MATCHER.addURI(AUTHORITY, "Unigrams/#", UNIGRAM_ID);
    }

    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        // Get read access from the database
        Context context = getContext();
        SQLiteOpenHelper openHelper = new DatabaseHelper(context);
        database = openHelper.getReadableDatabase();
        return database != null;
    }

    /**
     * Resets the database (called when database is restored)
     * @param context The context
     */
    public static void resetDB(Context context)
    {
        SQLiteOpenHelper openHelper = new DatabaseHelper(context);
        database = openHelper.getReadableDatabase();
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        // Set query parameters
        String table = "Unigrams";
        String[] columns = {"_id", "Word"};
        String selectionClause = "Word LIKE ? OR NormalisedWord LIKE ?";
        String[] selectionArguments = {selectionArgs[0] + "%", selectionArgs[0] + "%"};
        String orderBy = "NormalisedWord ASC";

        // Get words which start with the text typed by the user
        Cursor cursor = database.query(table, columns, selectionClause,
                selectionArguments, null, null, orderBy);

        // Observe changes in database cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the _id, word and intent data for each record
        String columnNames [] = {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA};
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);
        while(cursor.moveToNext())
        {
            Object columnValues[] = {cursor.getInt(0),
                    cursor.getString(1), cursor.getString(1)};
            matrixCursor.addRow(columnValues);
        }
        cursor.close();

        return matrixCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)){
            case UNIGRAMS:
                return "vnd.android.cursor.dir/vnd.edu.um.Unigrams";
            case UNIGRAM_ID:
                return "vnd.android.cursor.item/vnd.edu.um.Unigrams";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues,
                      @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
