package my.edu.tarc.finalyearproject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        int activityID = Integer.parseInt(remoteMessage.getData().get("id"));
        String imageName = remoteMessage.getData().get("img");
        String activityStatus = remoteMessage.getData().get("stat");
        int cctvID = Integer.parseInt(remoteMessage.getData().get("cctv"));

        Intent intent = new Intent(this, ActivityDetail.class);
        intent.putExtra("activityID", activityID);
        intent.putExtra("imageName", imageName);
        intent.putExtra("activityStatus", activityStatus);
        intent.putExtra("cctvID", cctvID);
        PendingIntent pi = PendingIntent.getActivity(NotificationService.this,0,intent,PendingIntent.FLAG_ONE_SHOT);
        long[] v = {500,600000};
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+ "://" +getApplicationContext().getPackageName()+"/"+R.raw.alarm);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(NotificationService.this)
                .setContentTitle("Abnormal Activity Detected!")
                .setContentText("Check it now!")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_warning_yellow_24dp)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setVibrate(v)
                .setSound(uri)
                .setPriority(Notification.PRIORITY_HIGH);
        builder.setContentIntent(pi);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MH24_SCREENLOCK");
        wl.acquire(10000);
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());

    }
}
