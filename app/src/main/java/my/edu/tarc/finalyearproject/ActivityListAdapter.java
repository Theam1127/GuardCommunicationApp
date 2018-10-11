package my.edu.tarc.finalyearproject;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class ActivityListAdapter extends BaseAdapter implements ListAdapter{
    List<Activity> activities;
    Context context;

    public ActivityListAdapter(List<Activity> activities, Context context) {
        this.activities = activities;
        this.context = context;
    }

    @Override
    public int getCount() {
        return activities.size();
    }

    @Override
    public Object getItem(int i) {
        return activities.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View v, ViewGroup viewGroup) {
        View view = v;
        if(view==null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.activity_list_view,null);
        }
        TextView activityDetail = view.findViewById(R.id.tvActivityDetail);
        final ImageView activityImage = view.findViewById(R.id.ivActivityImage);
        activityDetail.setText(activities.get(i).toString());
        if(activities.get(i).getActivityStatus().equals("Unsolved")||activities.get(i).getActivityStatus().equals("Need Backup"))
            activityDetail.setBackgroundColor(context.getResources().getColor(R.color.holo_red_light));
        else if(activities.get(i).getActivityStatus().equals("Processing"))
            activityDetail.setBackgroundColor(context.getResources().getColor(R.color.holo_orange_dark));
        else
            activityDetail.setBackgroundColor(context.getResources().getColor(R.color.holo_green_dark));

        StorageReference imageStorage = FirebaseStorage.getInstance().getReference();
        imageStorage.child(activities.get(i).getActivityImage()).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                Uri imageUri = task.getResult();
                Glide.with(context.getApplicationContext())
                        .load(imageUri)
                        .into(activityImage);
            }
        });

        return view;
    }

}
