package mt.edu.um.malteseaacapp.activities.settings;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.chooser.android.DbxChooser;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionLookupErrorException;
import com.dropbox.core.v2.files.WriteMode;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.SearchSuggestionsProvider;
import mt.edu.um.malteseaacapp.activities.Home;
import mt.edu.um.malteseaacapp.operations.StorageOps;

/**
 * Dropbox related functionality obtained by using Dropbox API v2.
 * Methods obtained from: https://github.com/dropbox/dropbox-sdk-java
 */

public class Profile extends AppCompatActivity {

    // To check whether the pin has been entered
    private boolean pinEntered;
    // To check whether pin has been entered successfully
    private final int ENTER_PIN_REQUEST_CODE = 0;
    // To be used for requesting permission to access network state
    private final int NETWORK_STATE_PERMISSION_REQUEST_CODE = 100;
    // Request code to be used for requesting permission to access external storage
    final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 101;

    // To access Dropbox
    final static private String APP_KEY = "YOUR_KEY_HERE";
    private static DbxClientV2 dbxClientV2;
    private static String accessToken;

    // To save the backup with the specified profile name in Dropbox
    private static String profileName;

    // To choose restore file from Dropbox
    private static final int DBX_CHOOSER_REQUEST_CODE = 1;
    private DbxChooser dbxChooser;

    private int taskCode; // The code associated to the task which will be carried out after sign in
    private final int BACKUP_CODE = 2; // Code to indicate that the backup button was clicked
    private final int RESTORE_CODE = 3; // Code to indicate that the restore button was clicked

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initially the pin is not entered
        pinEntered = false;

        // If no pin has been set, show profile menu without prompting for pin
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getString("pin", "").equals(""))
            setUpLayout();
        else {
            // If the correct pin has been entered, show the profile menu
            // Otherwise, open the EnterPin activity
            if (pinEntered)
                setUpLayout();
            else {
                Intent enterPin = new Intent(this, EnterPin.class);
                startActivityForResult(enterPin, ENTER_PIN_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Launch Home activity
        Intent home = new Intent(getApplicationContext(), Home.class);
        startActivity(home);
        finish();
    }

    /**
     * Set up the layout of the "Profile" activity (including onClickListeners)
     */
    private void setUpLayout() {
        // Initialise Dropbox chooser (used for restore)
        dbxChooser = new DbxChooser(APP_KEY);

        // Get access token from shared preferences if available
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        accessToken = sharedPreferences.getString("access_token", null);

        // If the user has not signed in to Dropbox yet, show sign in prompt
        if (accessToken == null)
            Auth.startOAuth2Authentication(Profile.this, APP_KEY);

        setContentView(R.layout.activity_profile);

        // Check if the last backup was successful and indicate if it wasn't
        if (!PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("backup_complete", true))
        {
            TextView infoMessage = this.findViewById(R.id.profileMessage);
            String sad = "\uD83D\uDE1E";
            String text = "The last attempted profile backup was not completed successfully " + sad;
            infoMessage.setText(text);

            // Delete local backup files since they are no longer useful
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StorageOps.deleteLocalBackup(getApplicationContext());
                }
            }).start();
        }

        // Find EditText from layout to check that the profile name was not left blank
        EditText profileEditText = this.findViewById(R.id.profileEditText);

        // Find buttons from layout and set onClickListeners
        final Button backupButton = this.findViewById(R.id.backupButton);
        final Button restoreButton = this.findViewById(R.id.restoreButton);
        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Animate button when clicked
                Animation bounce
                        = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scale);
                bounce.setInterpolator(new DecelerateInterpolator());
                backupButton.startAnimation(bounce);

                // Check if profile name was left blank
                if (profileEditText.getText().toString().equals(""))
                    Toast.makeText(getApplicationContext(), "Please enter a profile name " +
                            "so that you can identify your backup.", Toast.LENGTH_SHORT).show();
                else {
                    // Set profile name (to save file with the given name) and start backup
                    profileName = profileEditText.getText().toString();
                    backup();
                }
            }
        });
        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Animate button when clicked
                Animation bounce
                        = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scale);
                bounce.setInterpolator(new DecelerateInterpolator());
                restoreButton.startAnimation(bounce);

                // Ensure Dropbox chooser has been initialised
                if (dbxChooser == null)
                    Toast.makeText(getApplicationContext(), "Cannot initialise Dropbox " +
                            "file chooser. Please try again later.", Toast.LENGTH_SHORT).show();
                else
                {
                    // Check if permission is granted to read external storage
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Request permission to access external storage
                        ActivityCompat.requestPermissions(Profile.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                    } else
                        restore();
                }
            }
        });
    }

    /**
     * Backup the current profile to Dropbox after checking for network connectivity
     */
    private void backup() {
        // Set task code
        taskCode = BACKUP_CODE;

        // Check for network connectivity first
        if (!networkAvailable()) {
            // Show info message
            final TextView infoMessage = this.findViewById(R.id.profileMessage);
            // Indicate that backup has failed
            String sad = "\uD83D\uDE1E";
            String text = "Backup failed " + sad;
            infoMessage.setText(text);
        } else {
            dropboxTask();
        }
    }

    /**
     * Restore profile from Dropbox after checking for network connectivity
     */
    private void restore() {
        // Set task code
        taskCode = RESTORE_CODE;

        // Check for network connectivity first
        if (!networkAvailable()) {
            // Show info message
            final TextView infoMessage = this.findViewById(R.id.profileMessage);
            // Indicate that restore has failed
            String sad = "\uD83D\uDE1E";
            String text = "Restore failed " + sad;
            infoMessage.setText(text);
        } else {
            dropboxTask();
        }
    }

    /**
     * Method that checks whether the user has an available network connection
     * @return Returns true if a network connection is available, and false otherwise
     */
    private boolean networkAvailable() {

        // Check if permission is granted to read network state
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Request permission to read the network state
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                    NETWORK_STATE_PERMISSION_REQUEST_CODE);
            Toast.makeText(getApplicationContext(),
                    "No permission to check for network connectivity." +
                            " Backup/Restore cancelled.",
                    Toast.LENGTH_SHORT).show();

            return false;
        }

        // Check for network connectivity
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting())
            return true;
        else {
            Toast.makeText(getApplicationContext(),
                    "No network connectivity. Please turn on Wi-Fi to backup/restore profile.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Set up Dropbox client from access token
     */
    private void setUpDropboxClient() {
        // Get access token from shared preferences if available
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        accessToken = sharedPreferences.getString("access_token", null);

        // If the user has not signed in to Dropbox yet,
        // sign in and add token to shared preferences
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                sharedPreferences.edit().putString("access_token", accessToken).apply();
            }
        }
        // Set up client if it is null
        if (dbxClientV2 == null) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("AAC/1.0")
                    .build();

            // If the access token is null, Dropbox access has not been successfully granted
            if (accessToken == null) {
                Toast.makeText(this,
                        "Cannot access Dropbox. Please make sure you have granted permission.",
                        Toast.LENGTH_SHORT).show();
                onBackPressed();
            } else
                dbxClientV2 = new DbxClientV2(requestConfig, accessToken);
        }
    }

    /**
     * If the Dropbox client is initialised successfully, backup or restore (according to task code)
     */
    private void dropboxTask() {
        setUpDropboxClient();

        // If the client was not successfully initialised, set token to null so that
        // the authentication window pops up again when opening the Profile activity
        if (dbxClientV2 == null) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putString("access_token", null).apply();
            Toast.makeText(this,
                    "Cannot access Dropbox. Please make sure you have granted permission.",
                    Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        switch (taskCode) {
            case BACKUP_CODE:
                try {
                    // Notify user that upload has started
                    Toast.makeText(this,
                            "Profile backup started. " +
                                    "You will be notified when the profile has been" +
                                    " backed up to Dropbox.",
                            Toast.LENGTH_LONG).show();
                    // Indicate that backup is not yet successful in shared preferences
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit().putBoolean("backup_complete", false).commit();
                    // Start Dropbox upload service
                    Intent dropboxService
                            = new Intent(this, DropboxUploadService.class);
                    // Add profile name to intent
                    dropboxService.putExtra("ProfileName", profileName);
                    getApplicationContext().startService(dropboxService);
                    // Close Profile activity after starting upload
                    onBackPressed();

                } catch (Exception e) {
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit().putString("access_token", null).apply();
                    Toast.makeText(this,
                            "Cannot access Dropbox. " +
                                    "Please make sure you have granted permission.",
                            Toast.LENGTH_SHORT).show();
                    onBackPressed();
                    return;
                }
                break;
            case RESTORE_CODE:
                // Launch Dropbox file chooser
                dbxChooser.forResultType(DbxChooser.ResultType.FILE_CONTENT)
                        .launch(Profile.this, DBX_CHOOSER_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ENTER_PIN_REQUEST_CODE:
                // Show profile menu if pin was entered successfully and
                // go back to Home activity if pin was not entered successfully
                if (resultCode == RESULT_OK) {
                    pinEntered = true;
                    setUpLayout();
                } else
                    onBackPressed();
                break;
            case DBX_CHOOSER_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    // Notify user that restore has started
                    Toast.makeText(this,
                            "Restoring profile\u2026" ,
                            Toast.LENGTH_LONG).show();
                    // Indicate that restore is not yet successful in shared preferences
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit().putBoolean("restore_complete", false).commit();

                    // Get file path from chooser result
                    DbxChooser.Result result = new DbxChooser.Result(data);
                    String path = result.getLink().getPath();

                    // Start restore service
                    Intent restoreService = new Intent(this, RestoreService.class);
                    // Add file path to intent
                    restoreService.putExtra("Path", path);
                    getApplicationContext().startService(restoreService);
                    // Close Profile activity after starting restore
                    finish();

                } else
                    // Indicate that no profile has been selected if chooser activity was cancelled
                    Toast.makeText(this,
                            "No profile selected. " +
                                    "Please choose a profile to restore from Dropbox.",
                            Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == NETWORK_STATE_PERMISSION_REQUEST_CODE) {
            // If permission is granted
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Proceed with Dropbox task
                dropboxTask();

            } else {
                // Indicate that permission was not granted
                Toast.makeText(getApplicationContext(),
                        "No permission to check for network connectivity." +
                                " Backup/Restore cancelled.",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE)
        {
            // If permission is granted
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Proceed to restore
                restore();

            } else {
                // Indicate that permission was not granted
                Toast.makeText(getApplicationContext(),
                        "No permission to access external storage. Restore cancelled.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Static class used for uploading backup file to Dropbox
     */
    public static class DropboxUploadService extends IntentService {

        // Decimal 4 = Binary 0100
        // Signed shift to the left by 20 bits gives: 0100 0000 0000 0000 0000 0000
        // Which is equivalent to Decimal 4194304
        // Converting to MiB: 4194304/(1024*1024)= 4MiB
        private final long CHUNKED_UPLOAD_CHUNK_SIZE = 4L << 20; // 4MiB
        private final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 10;

        // For showing progress notification
        private static int FOREGROUND_ID = (int) System.currentTimeMillis()%10000;
        private static int NOTIFY_ID = FOREGROUND_ID;
        private static String CHANNEL_ID = "Backup";

        private String profileName; // Name of profile to use for saving backup file
        private String errorMessage; // Error message to show in notification
        private boolean stopService; // To check whether to stop execution

        /**
         * Required default constructor
         */
        public DropboxUploadService() {
            super("DropboxUploadService");
        }


        @Override
        public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
            // Stop service upon request
            if (intent.getAction() != null && intent.getAction().equals("STOPSELF"))
            {
                // Indicate in shared preferences that backup is complete so that service
                // finishes and does not restart
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putBoolean("backup_complete", true).commit();
                stopService = true;

                // Indicate that the backup is being cancelled
                raiseCancelNotification();
            }
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String happy = "\uD83D\uDE0A";
            String sad = "\uD83D\uDE1E";
            String text;

            // Get profile name from intent
            this.profileName = intent.getStringExtra("ProfileName");

            // If upload is successful, do nothing
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("backup_complete", false))
                return;


            // Check if service was stopped
            if (stopService)
            {
                errorMessage = "Cancelled by user.";
                text = "Backup failed " + sad + ". " + errorMessage;
                // Delete local backup files after upload
                StorageOps.deleteLocalBackup(this);
                // Indicate that upload is complete
                raiseCompletionNotification(text);
                return;
            }

            // Create notification for backup progress
            buildNotification();

            // Start upload and indicate if successful or not.
            if (upload())
                text = "Backup completed successfully " + happy;
            else
                text = "Backup failed " + sad + ". " + errorMessage;

            // Delete local backup files after upload
            StorageOps.deleteLocalBackup(this);

            // Indicate in shared preferences that upload was completed successfully
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putBoolean("backup_complete", true).commit();

            // Indicate that upload is complete
            raiseCompletionNotification(text);
        }

        /**
         * Zip app files and upload archive to dropbox
         * @return  Returns true if the upload was successful and false otherwise
         */
        private boolean upload() {
            try {
                // Check if service was stopped
                if (stopService) {
                    errorMessage = "Cancelled by user.";
                    return false;
                }

                // Zip "databases" folder and store in "files" folder
                ZipUtil.pack(new File(this
                                .getDatabasePath("AAC.db").getParent()),
                        new File(this.
                                getFilesDir().getPath() + "/AAC.dbbackupv2"));

                // Check if service was stopped
                if (stopService) {
                    errorMessage = "Cancelled by user.";
                    return false;
                }

                // Zip "files" folder and store in app folder
                ZipUtil.pack(new File(this
                                .getFilesDir().getPath()),
                        new File(this
                                .getFilesDir().getParent() + "/profile.aacbackup"));

                // Check if service was stopped
                if (stopService) {
                    errorMessage = "Cancelled by user.";
                    return false;
                }

            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            }

            File backup =
                    new File(this
                            .getFilesDir().getParent() + "/profile.aacbackup");

            try {
                // Upload file to Dropbox (in chunks since it will be large)
                // Return true if successful and false otherwise
                return chunkedUploadFile(dbxClientV2, backup,
                        "/" + getString(R.string.app_name) + "/" +
                                profileName + ".aacbackup");
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Method to upload large backup file in chunks
         * Obtained from: https://github.com/dropbox/dropbox-sdk-java/
         * @param dbxClient     Dropbox client
         * @param localFile     File to upload
         * @param dropboxPath   Dropbox path to upload in
         * @return  Returns true if the upload was successful and false otherwise
         */
        private boolean chunkedUploadFile(DbxClientV2 dbxClient,
                                          File localFile, String dropboxPath) {
            long size = localFile.length();
            long uploaded = 0L;

            // Chunked uploads have 3 phases, each of which can accept uploaded bytes:
            //
            //    (1) Start: initiate the upload and get an upload session ID
            //    (2) Append: upload chunks of the file to append to our session
            //    (3) Finish: commit the upload and close the session
            //
            // We track how many bytes we uploaded to determine which phase we should be in.
            String sessionId = null;
            for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
                // Check if service was stopped
                if (stopService)
                {
                    errorMessage = "Cancelled by user.";
                    return false;
                }

                try (InputStream in = new FileInputStream(localFile)) {
                    // if this is a retry, make sure seek to the correct offset
                    in.skip(uploaded);

                    // (1) Start
                    if (sessionId == null) {
                        sessionId = dbxClient.files().uploadSessionStart()
                                .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
                                .getSessionId();
                        uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                        printProgress(uploaded, size);
                    }

                    UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

                    // (2) Append
                    while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {

                        // Check if service was stopped
                        if (stopService)
                        {
                            errorMessage = "Cancelled by user.";
                            return false;
                        }

                        dbxClient.files().uploadSessionAppendV2(cursor)
                                .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                        uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                        printProgress(uploaded, size);
                        cursor = new UploadSessionCursor(sessionId, uploaded);
                    }

                    // (3) Finish
                    long remaining = size - uploaded;
                    CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath)
                            .withMode(WriteMode.OVERWRITE)
                            .withClientModified(new Date(localFile.lastModified()))
                            .build();
                    dbxClient.files().uploadSessionFinish(cursor, commitInfo)
                            .uploadAndFinish(in, remaining);
                    return true;
                } catch (RetryException ex) {
                    // RetryExceptions are never automatically retried by the client for uploads.
                    // Must catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
                    sleepQuietly(ex.getBackoffMillis());
                    continue;
                } catch (NetworkIOException ex) {
                    // network issue with Dropbox (maybe a timeout?) try again
                    continue;
                } catch (UploadSessionLookupErrorException ex) {
                    if (ex.errorValue.isIncorrectOffset()) {
                        // server offset into the stream doesn't match our offset (uploaded).
                        // Seek to the expected offset according to the server and try again.
                        uploaded = ex.errorValue
                                .getIncorrectOffsetValue()
                                .getCorrectOffset();
                        continue;
                    } else {
                        // Some other error occurred, give up.
                        errorMessage = ex.getMessage();
                        return false;
                    }
                } catch (UploadSessionFinishErrorException ex) {
                    if (ex.errorValue.isLookupFailed()
                            && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                        // server offset into the stream doesn't match our offset (uploaded).
                        // Seek to the expected offset according to the server and try again.
                        uploaded = ex.errorValue
                                .getLookupFailedValue()
                                .getIncorrectOffsetValue()
                                .getCorrectOffset();
                        continue;
                    } else {
                        // Some other error occurred, give up.
                        errorMessage = ex.getMessage();
                        return false;
                    }
                } catch (DbxException ex) {
                    // Some other error occurred, give up.
                    errorMessage = ex.getMessage();
                    return false;
                } catch (IOException ex) {
                    // Some other error occurred, give up.
                    errorMessage = ex.getMessage();
                    return false;
                }
            }

            // if we made it here, then we must have run out of attempts
            errorMessage = "Please make sure you have an Internet connection";
            return false;
        }

        /**
         * Show the upload progress
         * @param uploaded  The number of bytes uploaded
         * @param size      The total number of bytes that need to be uploaded
         */
        private void printProgress(long uploaded, long size) {
            int progress = (int) (100 * (uploaded / (double) size));

            // Show progress in notification bar
            raiseNotification(progress);
        }

        /**
         * Sleep
         * @param millis    The number of milliseconds to sleep for
         */
        private void sleepQuietly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                // just exit
                errorMessage = "Interrupted during backoff";
                System.exit(1);
            }
        }

        /**
         * Create notification for backup messages
         */
        private void buildNotification() {
            String title = "AAC profile backup";
            String content = "0%";
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(
                            this, CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setContentText(content)
                            .setTicker(content)
                            .setDefaults(0)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            // Start Home when notification is clicked
            Intent startHome = new Intent(this, Home.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            // Allow user to cancel service
            Intent stopSelf = new Intent(this, DropboxUploadService.class);
            stopSelf.setAction("STOPSELF");
            PendingIntent pendingIntent1
                    = PendingIntent.getService(this,
                    (int) System.currentTimeMillis() % 10000,
                    stopSelf,PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(android.R.drawable.ic_delete,
                    getString(R.string.cancel), pendingIntent1);

            // Set notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "AAC Profile Backup";
                String description = "Notifications related to profile backup";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel =
                        new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                // Register the channel with the system
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }

            // Set progress and show notification
            builder.setProgress(100, 0, false);
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        /**
         * Shows a notification with the progress
         * @param progress  Progress as a percentage
         */
        private void raiseNotification(int progress) {
            String title = "AAC profile backup";
            String content = progress + "%";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setDefaults(0)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setContentText(content)
                            .setTicker(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(100, progress, false);

            // Start Home when notification is clicked
            Intent startHome = new Intent(this, Home.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            // Allow user to cancel service
            Intent stopSelf = new Intent(this, DropboxUploadService.class);
            stopSelf.setAction("STOPSELF");
            PendingIntent pendingIntent1
                    = PendingIntent.getService(this,
                    (int) System.currentTimeMillis() % 10000,
                    stopSelf,PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(android.R.drawable.ic_delete,
                    getString(R.string.cancel), pendingIntent1);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        /**
         * Raise a notification to indicate that the upload is complete
         * @param content   The content to show in the notification
         */
        private void raiseCompletionNotification(String content) {
            String title = "AAC profile backup";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setAutoCancel(true)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(content))
                            .setContentText(content)
                            .setTicker(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(0, 0, false);

            // Start Home when notification is clicked
            Intent startHome = new Intent(this, Home.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        /**
         * Raise a notification to indicate that the upload is being cancelled
         */
        private void raiseCancelNotification() {
            String title = "AAC profile backup";
            String content = "Cancelling backup\u2026";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setAutoCancel(true)
                            .setDefaults(0)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(content))
                            .setContentText(content)
                            .setTicker(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(0, 0, false);

            // Start Home when notification is clicked
            Intent startHome = new Intent(this, Home.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            super.onTaskRemoved(rootIntent);
            // Restart upload service if backup was not successful and the service has stopped
            if (!PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("backup_complete", false)) {
                // Send intent to restart the Dropbox service
                Intent broadcast = new Intent("RestartService");
                broadcast.putExtra("ProfileName", profileName);
                broadcast.putExtra("Service", 0);
                sendBroadcast(broadcast);
            }
        }
    }

    /**
     * Static class used for restoring profile from Dropbox
     */
    public static class RestoreService extends IntentService {

        // For showing progress notification
        private static int FOREGROUND_ID = (int) System.currentTimeMillis()%10000;
        private static int NOTIFY_ID = FOREGROUND_ID;
        private static String CHANNEL_ID = "Restore";

        private String path; // Path of restore file
        private String errorMessage; // Error message to show in notification

        /**
         * Required default constructor
         */
        public RestoreService() {
            super("RestoreService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String happy = "\uD83D\uDE0A";
            String sad = "\uD83D\uDE1E";
            String text;

            // Get path of downloaded file from intent
            this.path = intent.getStringExtra("Path");

            // If restore is successful, do nothing
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("restore_complete", false))
                return;

            // Create notification for restore progress
            buildNotification();

            // Start restore and indicate if successful or not.
            if (restore())
                text = "Restore completed successfully " + happy;
            else
                text = "Restore failed " + sad + ". " + errorMessage;


            // Indicate in shared preferences that restore was completed successfully
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putBoolean("restore_complete", true).commit();

            // Indicate that restore is complete
            raiseCompletionNotification(text);

            // Reset search suggestion provider and start Home activity
            SearchSuggestionsProvider.resetDB(this);
            Intent startHome = new Intent(this, Home.class);
            startHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startHome);
        }

        /**
         * Delete old app directories and restore them from backup file
         * @return  Returns true if restore was successful and false otherwise
         */
        private boolean restore()
        {
            try {
                File selectedFile = new File(path);
                // Check that the selected file is a valid profile by checking that it
                // contains all the required entries in the zip file
                if (!ZipUtil.containsEntry(selectedFile, "images") ||
                        !ZipUtil.containsEntry(selectedFile, "audio") ||
                        !ZipUtil.containsEntry(selectedFile, "predictions") ||
                        !ZipUtil.containsEntry(selectedFile, "AAC.dbbackupv2")) {
                    errorMessage = "Invalid file selected. "
                            + "Please choose a valid profile to restore"
                            + " from the " + getString(R.string.app_name)
                            + " folder. The file should"
                            + " have the extension '.aacbackup'";
                    return false;
                } else
                {
                    // Delete old database directory
                    StorageOps.deleteRecursively(
                            new File (this.getFilesDir().getParent() + "/databases"));
                    // Restore the database directory
                    File dbZip =
                            new File(this.getFilesDir().getParent() + "/AAC.dbbackupv2");
                    ZipUtil.unpackEntry(selectedFile, "AAC.dbbackupv2", dbZip);
                    ZipUtil.unpack(dbZip,
                            new File(this.getFilesDir().getParent() + "/databases"));
                    // Delete temporary database zip file
                    new File(this.getFilesDir().getParent(), "/AAC.dbbackupv2").delete();
                    raiseNotification(10);

                    // Delete the old files directory
                    StorageOps.deleteRecursively(
                            new File (this.getFilesDir().getPath()));
                    raiseNotification(25);

                    // Restore the files directory
                    ZipUtil.unpack(selectedFile, new File(this.getFilesDir().getPath()));
                    // Delete database zip file from files directory
                    new File(this.getFilesDir().getPath(), "/AAC.dbbackupv2").delete();
                    raiseNotification(100);

                    return true;
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                // If exception is encountered, then an incompatible restore file was selected
                errorMessage = "Invalid file selected. Please choose a valid profile to restore"
                        + " from the " + getString(R.string.app_name) + " folder. The file should"
                        + " have the extension '.aacbackup'";
                return false;
            }
        }

        /**
         * Create the notification for restore messages
         */
        private void buildNotification() {
            String title = "AAC profile restore";
            String content = "0%";
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(
                            this, CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setContentText(content)
                            .setTicker(content)
                            .setDefaults(0)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            // Set notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "AAC Profile Restore";
                String description = "Notifications related to profile restore";
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(description);
                // Register the channel with the system
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }

            // Show notification
            builder.setProgress(100, 0, false);
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        /**
         * Shows a notification with the progress
         * @param progress  Progress as a percentage
         */
        private void raiseNotification(int progress) {
            String title = "AAC profile restore";
            String content = progress + "%";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setDefaults(0)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setContentText(content)
                            .setTicker(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(100, progress, false);

            // Start Home when notification is clicked
            Intent startHome = new Intent(this, Home.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        /**
         * Raise a notification to indicate that the restore is complete
         * @param content   The content to show in the notification
         */
        private void raiseCompletionNotification(String content) {
            String title = "AAC profile restore";

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setAutoCancel(true)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(content))
                            .setContentText(content)
                            .setTicker(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(0, 0, false);

            // Start Home when notification is clicked
            Intent startHome = new Intent(this, Home.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFY_ID, builder.build());
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            super.onTaskRemoved(rootIntent);
            // Restart service if restore was not successful and the service has stopped
            if (!PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("restore_complete", false)) {
                // Send intent to restart the restore service
                Intent broadcast = new Intent("RestartService");
                broadcast.putExtra("Path", path);
                broadcast.putExtra("Service", 1);
                sendBroadcast(broadcast);
            }
        }
    }
}