package mt.edu.um.malteseaacapp.operations;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

public class ImageOps {
    /**
     * Method to calculate inSampleSize so that bitmaps can be
     * downsampled and therefore, avoid out of memory exceptions.
     * Obtained from: https://developer.android.com/topic/performance/graphics/load-bitmap.html
     * @param options       BitmapFactory options
     * @param desiredWidth  The desired width of the bitmap to be loaded
     * @param desiredHeight The desired height of the bitmap to be loaded
     * @return Returns the inSampleSize (to be used for loading downsampled bitmap)
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int desiredWidth, int desiredHeight) {

        // Actual bitmap dimensions obtained from "Options"
        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;
        int inSampleSize = 1;

        // If actual dimensions are greater than desired dimensions, downsampling is required
        if (actualHeight > desiredHeight || actualWidth > desiredWidth) {

            int halfHeight = actualHeight / 2;
            int halfWidth = actualWidth / 2;

            // Calculate inSampleSize (always keeping it as a power of 2), since
            // decoder rounds down to the nearest power of 2 to get the final value
            while ((halfHeight / inSampleSize) >= desiredHeight
                    && (halfWidth / inSampleSize) >= desiredWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Decodes a downsampled bitmap to allow for loading
     * of large images without running out of memory
     * @param uri           The uri from where the image will be obtained
     * @param context       The context
     * @param desiredWidth  The desired width of the bitmap to be loaded
     * @param desiredHeight The desired height of the bitmap to be loaded
     * @return Returns a decoded downsampled bitmap
     */
    public static Bitmap decodeDownSampledBitamp(Uri uri, Context context,
                                                 int desiredWidth, int desiredHeight)
    {
        try {
            // Decode bitmaap with "inJustDecodeBounds = true" to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getContentResolver()
                    .openInputStream(uri), null, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, desiredWidth, desiredHeight);

            // Decode downsampled bitmap
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(context.getContentResolver()
                    .openInputStream(uri), null, options);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
