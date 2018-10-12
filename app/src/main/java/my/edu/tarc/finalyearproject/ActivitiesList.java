package my.edu.tarc.finalyearproject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ActivitiesList extends MenuActivity {
    ListView activitiesList;
    ActivityListAdapter adapter;
    List<Activity> activities, displayList;
    List<String> filterStatus;
    Spinner filter;
    FirebaseFirestore db;
    ProgressDialog pd;
    TextView noNetwork;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activities_list);

        noNetwork = findViewById(R.id.textViewNetworkNotOn);
        backgroundChecking();

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
        ArrayAdapter spinnerAdapter = new ArrayAdapter(ActivitiesList.this, R.layout.spinner_items, filterStatus);
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

                    filterActivities(filter.getSelectedItemPosition(), filter.getSelectedItem().toString());

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
                if(pd.isShowing())
                    pd.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadRun=false;
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


    public boolean checkNetwork(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = cm.getActiveNetworkInfo()!=null;
        if(connected)
            noNetwork.setVisibility(View.GONE);
        else
            noNetwork.setVisibility(View.VISIBLE);
        return connected;
    }

    Handler handler = new Handler();
    boolean threadRun = true;

    private void backgroundChecking(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(threadRun){
                    try{
                        Thread.sleep(500);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                checkNetwork();
                            }
                        });
                    }
                    catch(Exception e){}
                }
            }
        }).start();
    }




}
