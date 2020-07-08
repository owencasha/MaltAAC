package mt.edu.um.malteseaacapp.operations;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AudioOps {

    /**
     * Gets audio in the form of an input stream from the specified Uri,
     * and then converts it to a byte array
     * @param uri       The Uri from where to get the audio input stream
     * @param context   The context
     * @return          Returns the audio in a byte array
     */
    public static byte[] getAudio(Uri uri, Context context){
        try
        {
            // Get audio input stream from Uri
            InputStream audioInputStream = context.getContentResolver().openInputStream(uri);

            // To write bytes obtained from input stream into it
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // To store chunks of bytes read from input stream
            byte[] buffer = new byte[1024];

            // The number of bytes being read from the input stream.
            // This value is returned by the "read" method on every read operation
            int noOfBytes = 0;

            // Keep reading from input stream until the "read" method indicates that
            // no more bytes are left to be read
            while ((noOfBytes = audioInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, noOfBytes);
            }

            // Return audio as a byte array
            return outputStream.toByteArray();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the file name of the audio file from the specified Uri
     * @param uri       The Uri of the audio file
     * @param activity  The activity calling the method
     * @return          Returns the file name of the audio file
     */
    public static String getFileName(Uri uri, Activity activity) {

        // Request code to be used for requesting permission to access external storage
        final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 100;

        // Check if permission is granted to read external storage
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Request permission to access external storage
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);

            return "No permission to get file name.";
        }

        String fileName = null;
        Cursor cursor = activity.getApplicationContext().getContentResolver()
                .query(uri, null, null, null, null);
        try {
            // Get file name from query
            if (cursor != null && cursor.moveToNext()) {
                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
            // Close cursor
            if(cursor != null)
                cursor.close();

            return fileName;
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // If the file name has not been returned up to this point
        return "No permission to get file name.";
    }
}
