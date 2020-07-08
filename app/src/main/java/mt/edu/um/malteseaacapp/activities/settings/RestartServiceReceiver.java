package mt.edu.um.malteseaacapp.activities.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RestartServiceReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        // Which service to restart? 0 = backup, 1 = restore
        int service = intent.getIntExtra("Service", 0);

        if (service == 0) {
            // Start profile backup service and pass the obtained profile name
            Intent profileBackup = new Intent(context, Profile.DropboxUploadService.class);
            profileBackup.putExtra("ProfileName",
                    intent.getStringExtra("ProfileName"));
            context.startService(profileBackup);
        }

        if (service == 1)
        {
            // Start profile restore service and pass the obtained file path and size
            Intent profileRestore = new Intent(context, Profile.RestoreService.class);
            profileRestore.putExtra("Path", intent.getStringExtra("Path"));
            context.startService(profileRestore);
        }
    }
}
