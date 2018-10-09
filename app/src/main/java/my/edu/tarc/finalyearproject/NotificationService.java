package my.edu.tarc.finalyearproject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationService extends FirebaseMessagingService {
    SharedPreferences preferences;
    FirebaseFirestore db;
    int activityID;
    String imageName;
    String activityStatus;
    int cctvID;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        activityID = Integer.parseInt(remoteMessage.getData().get("id"));
        imageName = remoteMessage.getData().get("img");
        activityStatus = remoteMessage.getData().get("stat");
        cctvID = Integer.parseInt(remoteMessage.getData().get("cctv"));


        db = FirebaseFirestore.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String guardID = preferences.getString("guardID", "");
        db.collection("Users").whereEqualTo("guardID", guardID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                int scheduleID = Integer.parseInt(task.getResult().getDocuments().get(0).get("scheduleID").toString());
                db.collection("Schedule").whereEqualTo("scheduleID", scheduleID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Timestamp start = task.getResult().getDocuments().get(0).getTimestamp("dutyStart");
                        Timestamp end = task.getResult().getDocuments().get(0).getTimestamp("dutyEnd");

                        Date startDate = start.toDate();
                        Date endDate = end.toDate();
                        Date now = new Date();
                        if (now.getHours()>=startDate.getHours() && now.getHours()<endDate.getHours()) {
                            if(activityStatus.equals("Require Further Action")){
                                sendNotification();
                            }
                            else {
                                String floorLevel = task.getResult().getDocuments().get(0).getString("dutyFloorLevel");
                                db.collection("CCTV").whereEqualTo("cctvFloorLevel", floorLevel).whereEqualTo("cctvID", cctvID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (!task.getResult().isEmpty()) {
                                            {
                                                sendNotification();
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }

                });

            }
        });




    }

    private void sendNotification(){
        long[] v = {500, 600000};
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/" + R.raw.alarm);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(NotificationService.this)
                .setContentTitle("Abnormal Activity Detected!")
                .setContentText("Check it now!")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_warning_yellow_24dp)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setVibrate(v)
                .setSound(uri)
                .setPriority(Notification.PRIORITY_HIGH);
        Intent intent = new Intent(NotificationService.this, ActivityDetail.class);
        intent.putExtra("activityID", activityID);
        intent.putExtra("imageName", imageName);
        intent.putExtra("activityStatus", activityStatus);
        intent.putExtra("cctvID", cctvID);
        PendingIntent pi = PendingIntent.getActivity(NotificationService.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        builder.setContentIntent(pi);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MH24_SCREENLOCK");
        wl.acquire(10000);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
