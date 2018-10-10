package my.edu.tarc.finalyearproject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ActivitiesList extends AppCompatActivity {
    ListView activitiesList;
    ActivityListAdapter adapter;
    List<Activity> activities, displayList;
    List<String> filterStatus;
    Spinner filter;
    FirebaseFirestore db;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activities_list);

        activitiesList = findViewById(R.id.listViewActivities);
        activitiesList.setDivider(new ColorDrawable(0xFF000000));
        activitiesList.setDividerHeight(4);
        filter = findViewById(R.id.spinnerFilterStatus);
        db = FirebaseFirestore.getInstance();
        activities = new ArrayList<>();
        displayList = new ArrayList<>();
        filterStatus = new ArrayList<>();
        filterStatus.add("All");
        filterStatus.add("Unsolved/Need Backup");
        filterStatus.add("Processing");
        filterStatus.add("Resolved");
        ArrayAdapter spinnerAdapter = new ArrayAdapter(ActivitiesList.this, android.R.layout.simple_dropdown_item_1line, filterStatus);
        filter.setAdapter(spinnerAdapter);
        pd = new ProgressDialog(ActivitiesList.this);
        pd.setCancelable(false);
        pd.setMessage("Loading...");
        pd.show();

        db.collection("AbnormalActivity").orderBy("activityID").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                activities.clear();
                if(!queryDocumentSnapshots.isEmpty()) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Activity activity = new Activity(Integer.parseInt(doc.get("activityID").toString()), doc.getString("activityStatus"), doc.getString("activityImage"), Integer.parseInt(doc.get("cctvID").toString()));
                        activities.add(activity);
                    }
                    //sort
                    int position = 0;
                    for (int a = 0; a < activities.size(); a++) {
                        if (activities.get(a).getActivityStatus().equals("Need Backup") || activities.get(a).getActivityStatus().equals("Unsolved")) {
                            Activity temp = activities.get(position);
                            activities.set(position, activities.get(a));
                            activities.set(a, temp);
                            position++;
                        }
                    }
                    for (int b = position; b < activities.size(); b++) {
                        if (activities.get(b).getActivityStatus().equals("Processing")) {
                            Activity temp = activities.get(position);
                            activities.set(position, activities.get(b));
                            activities.set(b, temp);
                            position++;
                        }
                    }
                    for (int c = position; c < activities.size(); c++) {
                        if (activities.get(c).getActivityStatus().equals("Resolved")) {
                            Activity temp = activities.get(position);
                            activities.set(position, activities.get(c));
                            activities.set(c, temp);
                            position++;
                        }
                    }
                }
                adapter = new ActivityListAdapter(activities, ActivitiesList.this);
                activitiesList.setAdapter(adapter);
                if(filter.getSelectedItemPosition()!=0){
                    filterActivities(filter.getSelectedItemPosition(), filter.getSelectedItem().toString());
                }

                pd.dismiss();

                filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        filterActivities(i,adapterView.getSelectedItem().toString());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
        });





        activitiesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ActivitiesList.this,ActivityDetail.class);
                intent.putExtra("activityID", displayList.get(i).getActivityID());
                intent.putExtra("imageName", displayList.get(i).getActivityImage());
                intent.putExtra("activityStatus", displayList.get(i).getActivityStatus());
                intent.putExtra("cctvID", displayList.get(i).getCctvID());
                startActivity(intent);
            }
        });
    }

    public void filterActivities(int position, String selectedItem){
        displayList.clear();
        if(position==0)
            displayList.addAll(activities);
        else {
            for (Activity a : activities) {
                if(position==1) {
                    if (a.getActivityStatus().equals("Need Backup") || a.getActivityStatus().equals("Unsolved"))
                        displayList.add(a);
                }
                else if(selectedItem.equals(a.getActivityStatus()))
                    displayList.add(a);
            }
        }
        adapter = new ActivityListAdapter(displayList, ActivitiesList.this);
        activitiesList.setAdapter(adapter);
    }
}
