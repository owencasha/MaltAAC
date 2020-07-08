package mt.edu.um.malteseaacapp.operations;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StorageOps {

    /**
     * Save the specified image to the specified directory by the specified name
     * @param context   The context
     * @param image     Specified image
     * @param directory Specified directory
     * @param word      Specified file name
     */
    public static void saveImage(Context context, Bitmap image, String directory, String word){

        // Set path where to save image
        String path = context.getFilesDir().getPath()
                + "/images/" + directory + "/" + word + ".png";

        FileOutputStream fileOutputStream =  null;
        try {
            // Save image to file
            fileOutputStream = new FileOutputStream(new File(path));
            image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the specified audio to the specified directory by the specified name
     * @param context   The context
     * @param audio     Specified audio in the form of byte array
     * @param directory Specified directory
     * @param word      Specified file name
     */
    public static void saveAudio(Context context, byte[] audio, String directory, String word){

        // Set paths where to save audio
        String malePath = context.getFilesDir().getPath() + "/audio/male/"
                + directory + "/" + word + ".wav";
        String femalePath = context.getFilesDir().getPath() + "/audio/female/"
                + directory + "/" + word + ".wav";

        FileOutputStream fileOutputStream =  null;
        try {
            // Write to male directory
            fileOutputStream = new FileOutputStream(new File(malePath));
            fileOutputStream.write(audio);
            fileOutputStream.close();

            // Write to female directory
            fileOutputStream = new FileOutputStream(new File(femalePath));
            fileOutputStream.write(audio);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates directories in the "images" and "audio" directories for the specified category
     * @param context       The context
     * @param categoryName  The specified category
     */
    public static void createCategoryDirectories(Context context, String categoryName)
    {
        // Set directory paths
        String imageDirectoryPath = context.getFilesDir().getPath()
                + "/images/" + categoryName;
        String maleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/male/" + categoryName;
        String femaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/female/" + categoryName;

        // Create directories
        File imageDirectory = new File(imageDirectoryPath);
        File maleAudioDirectory = new File(maleAudioDirectoryPath);
        File femaleAudioDirectory = new File(femaleAudioDirectoryPath);
        if (!imageDirectory.exists())
            imageDirectory.mkdir();
        if (!maleAudioDirectory.exists())
            maleAudioDirectory.mkdir();
        if (!femaleAudioDirectory.exists())
            femaleAudioDirectory.mkdir();
    }

    /**
     * Renames the directories in the "images" and "audio" directories for the specified category
     * @param context           The context
     * @param oldCategoryName   The old category name (old directory name)
     * @param newCategoryName   The new category name (new directory name)
     */
    public static void renameCategoryDirectories(Context context,
                                                 String oldCategoryName, String newCategoryName)
    {
        // Set old directory paths
        String oldImageDirectoryPath = context.getFilesDir().getPath()
                + "/images/" + oldCategoryName;
        String oldMaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/male/" + oldCategoryName;
        String oldFemaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/female/" + oldCategoryName;
        // Set new directory paths
        String newImageDirectoryPath = context.getFilesDir().getPath()
                + "/images/" + newCategoryName;
        String newMaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/male/" + newCategoryName;
        String newFemaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/female/" + newCategoryName;

        // Rename directories
        File oldImageDirectory = new File(oldImageDirectoryPath);
        File oldMaleAudioDirectory = new File(oldMaleAudioDirectoryPath);
        File oldFemaleAudioDirectory = new File(oldFemaleAudioDirectoryPath);
        File newImageDirectory = new File(newImageDirectoryPath);
        File newMaleAudioDirectory = new File(newMaleAudioDirectoryPath);
        File newFemaleAudioDirectory = new File(newFemaleAudioDirectoryPath);
        if (oldImageDirectory.exists())
            oldImageDirectory.renameTo(newImageDirectory);
        if (oldMaleAudioDirectory.exists())
            oldMaleAudioDirectory.renameTo(newMaleAudioDirectory);
        if (oldFemaleAudioDirectory.exists())
            oldFemaleAudioDirectory.renameTo(newFemaleAudioDirectory);
    }
    /**
     * Renames the image in the "Kategoriji" directory for the specified category
     * @param context           The context
     * @param oldCategoryName   The old category name (old directory name)
     * @param newCategoryName   The new category name (new directory name)
     */
    public static void renameCategoryImage(Context context,
                                           String oldCategoryName, String newCategoryName)
    {
        // Directory where image is contained
        String directory = context.getFilesDir().getPath() + "/images/Kategoriji";

        // Rename image
        File oldImage = new File(directory, oldCategoryName + ".png");
        File newImage = new File(directory, newCategoryName + ".png");
        if(oldImage.exists())
            oldImage.renameTo(newImage);
    }

    /**
     * Deletes the directories in the "images" and "audio" directories for the specified category
     * @param context       The context
     * @param categoryName  The name of the specified category
     */
    public static void deleteCategoryDirectories(Context context, String categoryName)
    {
        // Set directory paths
        String imageDirectoryPath = context.getFilesDir().getPath()
                + "/images/" + categoryName;
        String maleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/male/" + categoryName;
        String femaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/female/" + categoryName;

        // Delete directories
        File imageDirectory = new File(imageDirectoryPath);
        File maleAudioDirectory = new File(maleAudioDirectoryPath);
        File femaleAudioDirectory = new File(femaleAudioDirectoryPath);
        if (imageDirectory.exists())
            deleteRecursively(imageDirectory);
        if (maleAudioDirectory.exists())
            deleteRecursively(maleAudioDirectory);
        if (femaleAudioDirectory.exists())
            deleteRecursively(femaleAudioDirectory);

    }

    /**
     * Deletes file, or if argument is directory, deletes it recursively
     * @param fileOrDir     The file or directory to delete
     */
    public static void deleteRecursively(File fileOrDir) {

        // Check if argument is directory
        if (fileOrDir.isDirectory()) {
            // If argument is directory, go through each child and delete recursively
            for (File childFileOrDir : fileOrDir.listFiles()) {
                deleteRecursively(childFileOrDir);
            }
        }
        // Delete file or directory
        fileOrDir.delete();
    }

    /**
     * Deletes the image in the "Kategoriji" directory for the specified category
     * @param context       The context
     * @param categoryName  The name of the specified category
     */
    public static void deleteCategoryImage(Context context, String categoryName)
    {
        // Directory where image is contained
        String directory = context.getFilesDir().getPath() + "/images/Kategoriji";

        // Delete image
        File image = new File(directory, categoryName + ".png");
        if(image.exists())
            image.delete();
    }

    /**
     * Renames the the image and audio files corresponding to the specified word
     * @param context       The context
     * @param categoryName  The category to which the word pertains
     * @param oldWord       The old word
     * @param newWord       The new word
     */
    public static void changeWord(Context context,
                                  String categoryName, String oldWord, String newWord)
    {
        // Image and audio directories
        String imageDirectory = context.getFilesDir().getPath()
                + "/images/" + categoryName;
        String maleAudioDirectory = context.getFilesDir().getPath()
                + "/audio/male/" + categoryName;
        String femaleAudioDirectory = context.getFilesDir().getPath()
                + "/audio/female/" + categoryName;

        // Rename image and audio files
        File oldImage = new File(imageDirectory, oldWord + ".png");
        File newImage = new File(imageDirectory, newWord + ".png");
        File oldMaleAudio = new File(maleAudioDirectory, oldWord + ".wav");
        File newMaleAudio = new File(maleAudioDirectory, newWord + ".wav");
        File oldFemaleAudio = new File(femaleAudioDirectory, oldWord + ".wav");
        File newFemaleAudio = new File(femaleAudioDirectory, newWord + ".wav");
        if(oldImage.exists())
            oldImage.renameTo(newImage);
        if(oldMaleAudio.exists())
            oldMaleAudio.renameTo(newMaleAudio);
        if(oldFemaleAudio.exists())
            oldFemaleAudio.renameTo(newFemaleAudio);
    }

    /**
     * Moves the sound and image files corresponding to the specified word from the directory
     * associated with the old category to the directory associated with the new category
     * @param context       The context
     * @param oldCategory   The name of the old category
     * @param newCategory   The name of the new category
     * @param word          The specified word
     */
    public static void changeCategory(Context context,
                                      String oldCategory, String newCategory, String word)
    {
        // Set old image and audio directories (needed for deleting files)
        String oldImageDirectory = context.getFilesDir().getPath()
                + "/images/" + oldCategory;
        String oldMaleAudioDirectory = context.getFilesDir().getPath()
                + "/audio/male/" + oldCategory;
        String oldFemaleAudioDirectory = context.getFilesDir().getPath()
                + "/audio/female/" + oldCategory;
        // Set old and new image and audio paths
        String oldImagePath = context.getFilesDir().getPath()
                + "/images/" + oldCategory + "/" + word + ".png";
        String newImagePath = context.getFilesDir().getPath()
                + "/images/" + newCategory + "/" + word + ".png";
        String oldMaleAudioPath = context.getFilesDir().getPath()
                + "/audio/male/" + oldCategory + "/" + word + ".wav";
        String newMaleAudioPath = context.getFilesDir().getPath()
                + "/audio/male/" + newCategory + "/" + word + ".wav";
        String oldFemaleAudioPath = context.getFilesDir().getPath()
                + "/audio/female/" + oldCategory + "/" + word + ".wav";
        String newFemaleAudioPath = context.getFilesDir().getPath()
                + "/audio/female/" + newCategory + "/" + word + ".wav";

        // To move files
        FileInputStream oldFileStream;
        FileOutputStream newFileStream;

        /*  Move image and audio files
            --------------------------
            1. Put old file into FileInputStream
            2. Copy contents from FileInputStream to FileOutputStream (with new file)
            3. Delete old file
         */
        try{
            // Move image file
            oldFileStream = new FileInputStream(oldImagePath);
            newFileStream = new FileOutputStream(newImagePath);
            // To store chunks of bytes read from input stream
            byte[] buffer = new byte[1024];
            // The number of bytes being read from the input stream.
            // This value is returned by the "read" method on every read operation
            int noOfBytes = 0;
            // Keep reading from input stream until the "read" method indicates that
            // no more bytes are left to be read
            while ((noOfBytes = oldFileStream.read(buffer)) != -1) {
                newFileStream.write(buffer, 0, noOfBytes);
            }
            // Close file streams
            oldFileStream.close();
            newFileStream.flush();
            newFileStream.close();
            // Delete old image file
            File oldImageFile = new File(oldImageDirectory, word + ".png");
            if (oldImageFile.exists())
                oldImageFile.delete();

            // Move audio file
            oldFileStream = new FileInputStream(oldMaleAudioPath);
            newFileStream = new FileOutputStream(newMaleAudioPath);
            // To store chunks of bytes read from input stream
            buffer = new byte[1024];
            // Keep reading from input stream until the "read" method indicates that
            // no more bytes are left to be read
            while ((noOfBytes = oldFileStream.read(buffer)) != -1) {
                newFileStream.write(buffer, 0, noOfBytes);
            }
            // Close file streams
            oldFileStream.close();
            newFileStream.flush();
            newFileStream.close();
            // Delete old audio file
            File oldMaleAudioFile = new File(oldMaleAudioDirectory, word + ".wav");
            if (oldMaleAudioFile.exists())
                oldMaleAudioFile.delete();

            oldFileStream = new FileInputStream(oldFemaleAudioPath);
            newFileStream = new FileOutputStream(newFemaleAudioPath);
            // To store chunks of bytes read from input stream
            buffer = new byte[1024];
            // Keep reading from input stream until the "read" method indicates that
            // no more bytes are left to be read
            while ((noOfBytes = oldFileStream.read(buffer)) != -1) {
                newFileStream.write(buffer, 0, noOfBytes);
            }
            // Close file streams
            oldFileStream.close();
            newFileStream.flush();
            newFileStream.close();
            // Delete old audio file
            File oldFemaleAudioFile = new File(oldFemaleAudioDirectory, word + ".wav");
            if (oldFemaleAudioFile.exists())
                oldFemaleAudioFile.delete();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Deletes the image and audio files associated with the specified word
     * @param context   The context
     * @param category  The category of the specified word
     * @param word      The specified word
     */
    public static void deleteWord (Context context, String category, String word)
    {
        // Set directory paths
        String imageDirectoryPath = context.getFilesDir().getPath()
                + "/images/" + category;
        String maleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/male/" + category;
        String femaleAudioDirectoryPath = context.getFilesDir().getPath()
                + "/audio/female/" + category;

        // Delete image and audio file corresponding to the word
        File image = new File(imageDirectoryPath, word + ".png");
        File maleAudio = new File(maleAudioDirectoryPath, word + ".wav");
        File femaleAudio = new File(femaleAudioDirectoryPath, word + ".wav");
        if (image.exists())
            deleteRecursively(image);
        if (maleAudio.exists())
            deleteRecursively(maleAudio);
        if (femaleAudio.exists())
            deleteRecursively(femaleAudio);
    }

    /**
     * Deletes the temporary local backup zip files
     * @param context   The context
     */
    public static void deleteLocalBackup(Context context)
    {
        // Delete database zip file
        File dbBackup = new File(context.getFilesDir().getPath(),"/AAC.dbbackup");
        if (dbBackup.exists())
            dbBackup.delete();

        // Delete full backup zip file
        File backup = new File(context.getFilesDir().getParent(),"/profile.aacbackup");
        if (backup.exists())
            backup.delete();
    }

    /**
     * Method to copy a directory recursively from Assets folder to internal storage
     * Method obtained from: https://gist.github.com/tylerchesley/6198074
     * @param context   The context
     * @param dirOrFile The file or directory to copy recursively from Assets to internal storage
     */
    public static void copyAssetsSubdirectoryRecursively(Context context, String dirOrFile) {
        AssetManager assetManager = context.getAssets();
        String contents[] = null;
        try {
            // Get the contents of the directory (if it is a directory)
            contents = assetManager.list(dirOrFile);
            // If it is a file, copy it to internal storage
            if (contents.length == 0) {
                copyFile(context, dirOrFile);
            }
            else {
                // Set the directory path to use in internal storage
                String directoryPath = context.getFilesDir().getPath() + "/" + dirOrFile;
                File directory = new File(directoryPath);
                // Create the directory in internal storage if it does not exist
                if (!directory.exists())
                    directory.mkdir();
                // Copy all files/folders in the subdirectory recursively
                for (int i = 0; i < contents.length; ++i) {
                    copyAssetsSubdirectoryRecursively(context,
                            dirOrFile + "/" + contents[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to copy a file from Assets folder to internal storage
     * Method obtained from: https://gist.github.com/tylerchesley/6198074
     * @param context   The context
     * @param filename  The name of the file to copy
     */
    private static void copyFile(Context context, String filename) {
        AssetManager assetManager = context.getAssets();

        // To copy file
        InputStream inputStream = null;
        OutputStream outputStream = null;

        /*  Copy file from Assets to internal storage
            -----------------------------------------
            1. Put old file into InputStream
            2. Copy contents from InputStream to OutputStream (with new file)
         */
        try {
            inputStream = assetManager.open(filename);
            String filePath = context.getFilesDir().getPath() + "/" + filename;
            outputStream = new FileOutputStream(filePath);

            // To store chunks of bytes read from input stream
            byte[] buffer = new byte[1024];
            // The number of bytes being read from the input stream.
            // This value is returned by the "read" method on every read operation
            int noOfBytes = 0;
            // Keep reading from input stream until the "read" method indicates that
            // no more bytes are left to be read
            while ((noOfBytes = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, noOfBytes);
            }
            // Close file streams
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
