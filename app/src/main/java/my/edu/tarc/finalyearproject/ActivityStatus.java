package my.edu.tarc.finalyearproject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ActivityStatus extends MenuActivity {
    ImageView activityImage;
    TextView textViewActivityID, textViewActivityStatus;
    Button buttonResolved, buttonContinue, buttonFurther, buttonSubmit;
    EditText editTextFeedback;
    StorageReference imageStorage;
    String currentStatus = "";
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        Intent intent = getIntent();
        final int activityID = intent.getIntExtra("activityID", 0);
        String imageName = intent.getStringExtra("activityImage");
        String activityStatus = intent.getStringExtra("activityStatus");

        activityImage = findViewById(R.id.imageView);
        textViewActivityID = findViewById(R.id.textViewID);
        textViewActivityStatus = findViewById(R.id.textViewCurrentStatus);
        buttonResolved = findViewById(R.id.buttonResolved);
        buttonContinue = findViewById(R.id.buttonContinue);
        buttonFurther = findViewById(R.id.buttonFurther);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        editTextFeedback = findViewById(R.id.editTextFeedback);
        imageStorage = FirebaseStorage.getInstance().getReference();
        pd=new ProgressDialog(ActivityStatus.this);
        pd.setMessage("Loading...");
        pd.setCancelable(false);
        pd.show();


        imageStorage.child(imageName).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                Uri imageUri = task.getResult();
                Glide.with(ActivityStatus.this)
                        .load(imageUri)
                        .into(activityImage);
                pd.dismiss();
            }
        });

        textViewActivityID.setText("Activity ID: "+activityID);
        textViewActivityStatus.setText("Current Status: "+activityStatus);
        buttonResolved.setBackgroundColor(getResources().getColor(R.color.holo_green_dark));
        buttonFurther.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
        buttonContinue.setBackgroundColor(getResources().getColor(R.color.holo_orange_dark));

        buttonResolved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonFurther.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
                buttonContinue.setBackgroundColor(getResources().getColor(R.color.holo_orange_dark));
                buttonResolved.setBackgroundColor(Color.GRAY);
                currentStatus = buttonResolved.getText().toString();
            }
        });

        buttonFurther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonResolved.setBackgroundColor(getResources().getColor(R.color.holo_green_dark));
                buttonContinue.setBackgroundColor(getResources().getColor(R.color.holo_orange_dark));
                buttonFurther.setBackgroundColor(Color.GRAY);
                currentStatus = buttonFurther.getText().toString();
            }
        });

        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonResolved.setBackgroundColor(getResources().getColor(R.color.holo_green_dark));
                buttonFurther.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
                buttonContinue.setBackgroundColor(Color.GRAY);
                currentStatus = buttonContinue.getText().toString();
            }
        });

        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if(cm.getActiveNetworkInfo()==null){
                    Toast.makeText(ActivityStatus.this, "Please enable network connection!", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (currentStatus.equals(""))
                        Toast.makeText(ActivityStatus.this, "Please select a feedback!", Toast.LENGTH_SHORT).show();
                    else {
                        pd.show();
                        final String feedback = editTextFeedback.getText().toString().isEmpty() ? "" : editTextFeedback.getText().toString();
                        final FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("Feedback").orderBy("feedbackID").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                int id = 1;
                                if (!task.getResult().isEmpty())
                                    id = Integer.parseInt(task.getResult().getDocuments().get(task.getResult().getDocuments().size() - 1).get("feedbackID").toString());
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                String guardID = preferences.getString("guardID", "");
                                Map<String, Object> newFeedback = new HashMap<>();
                                newFeedback.put("activityID", activityID);
                                newFeedback.put("feedbackContent", feedback);
                                newFeedback.put("feedbackID", id);
                                newFeedback.put("guardID", guardID);
                                db.collection("Feedback").add(newFeedback);
                                db.collection("AbnormalActivity").whereEqualTo("activityID", activityID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        String docID = task.getResult().getDocuments().get(0).getId();
                                        Map<String, Object> updateStatus = new HashMap<>();
                                        updateStatus.put("activityStatus", currentStatus);
                                        db.collection("AbnormalActivity").document(docID).update(updateStatus);
                                        pd.dismiss();
                                        Toast.makeText(ActivityStatus.this, "Feedback Submitted", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                }
            }
        });
    }
}
