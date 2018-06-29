package com.example.android.shushme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import static android.content.Context.NOTIFICATION_SERVICE;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = GeofenceBroadcastReceiver.class.getSimpleName();



    /***
     * Handles the Broadcast message sent when the Geofence Transition is triggered
     * Careful here though, this is running on the main thread so make sure you start an AsyncTask for
     * anything that takes longer than say 10 second to run
     *
     * @param context
     * @param intent
     *
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive called");
        //Get the Geofence Event from the Intent sent through
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Log.e(TAG, String.format("Error code : %d", geofencingEvent.getErrorCode()));
            return;
        }

        // Get Transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
            setRingerMode(context,AudioManager.RINGER_MODE_SILENT);

        else if (geofenceTransition==Geofence.GEOFENCE_TRANSITION_EXIT)
            setRingerMode(context,AudioManager.RINGER_MODE_NORMAL);
        else {
            // Log the error.
            Log.e(TAG, String.format("Unknown transition : %d", geofenceTransition));
            // No need to do anything else
            return;
        }

        // Send the notification
        sendNotification(context, geofenceTransition);
    }
    /**
     * Posts a notification in the notification bar when a transition is detected
     * Uses different icon drawables for different transition types
     * If the user clicks the notification, control goes to the MainActivity
     *
     * @param context        The calling context for building a task stack
     * @param geofenceTransition The geofence transition type, can be Geofence.GEOFENCE_TRANSITION_ENTER
     *                       or Geofence.GEOFENCE_TRANSITION_EXIT
     */

    private void sendNotification(Context context, int geofenceTransition) {

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);
        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);
        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Check the transition type to display the relevant icon image
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle("silent mode activated");
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle("back to normal");
        }


        // Continue building the notification
        builder.setContentText("touch to relaunch");
        builder.setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());

    }
    /**
     * Changes the ringer mode on the device to either silent or back to normal
     *
     * @param context The context to access AUDIO_SERVICE
     * @param mode    The desired mode to switch device to, can be AudioManager.RINGER_MODE_SILENT or
     *                AudioManager.RINGER_MODE_NORMAL
     */

    private void setRingerMode(Context context,int mode){

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT < 24 || (Build.VERSION.SDK_INT >= 24 && !notificationManager.isNotificationPolicyAccessGranted()))
        {

//            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            audioManager.setRingerMode(mode);
            Log.d(TAG, "setRingerMode: Should be silent now");
        }
    }
}
