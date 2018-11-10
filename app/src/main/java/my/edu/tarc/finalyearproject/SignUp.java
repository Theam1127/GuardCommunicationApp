package my.edu.tarc.finalyearproject;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SignUp extends AppCompatActivity {

    Button buttonSignUp;
    EditText editTextUsername, editTextName, editTextPhone;
    SharedPreferences preferences;
    FirebaseFirestore db;
    boolean existingAcc = false;
    String docID;
    AlertDialog.Builder alert;
    Spinner scheduleList;
    List<String> schedules;
    ArrayAdapter scheduleAdapter;
    String registrationToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        db = FirebaseFirestore.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String guardID = preferences.getString("guardID", "");

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( SignUp.this,  new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                registrationToken = instanceIdResult.getToken();
                Log.e("newToken",registrationToken);
            }
        });

        scheduleList = findViewById(R.id.spinner);
        schedules = new ArrayList<>();
        scheduleAdapter = new ArrayAdapter(getApplicationContext(), R.layout.spinner_items, schedules);
        scheduleList.setAdapter(scheduleAdapter);
        buttonSignUp = findViewById(R.id.buttonRegister);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextName = findViewById(R.id.editTextName);
        editTextPhone = findViewById(R.id.editTextPhone);


        db.collection("Schedule").orderBy("dutyStart").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.getResult().isEmpty()){
                    schedules.clear();
                    for(DocumentSnapshot doc : task.getResult()){
                        Timestamp dutyStart = doc.getTimestamp("dutyStart");
                        Timestamp dutyEnd = doc.getTimestamp("dutyEnd");
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(dutyStart.toDate());
                        cal.add(Calendar.HOUR_OF_DAY,8);
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
                        String start = sdf.format(cal.getTime());
                        cal.setTime(dutyEnd.toDate());
                        cal.add(Calendar.HOUR_OF_DAY,8);
                        String end = sdf.format(cal.getTime());
                        String schedule = start+" - "+end;
                        schedules.add(schedule);
                        scheduleAdapter.notifyDataSetChanged();
                    }

                }
            }
        });

        if (guardID.equals("")) {
            alert = new AlertDialog.Builder(SignUp.this);
            alert.setMessage("Login to your account?");
            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    showInputDialog();
                }
            });
            alert.setNegativeButton("Sign Up New Account", null);
            alert.setCancelable(false);
            alert.show();

            checkLocationPermission();


            buttonSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!checkNetwork())
                        Toast.makeText(SignUp.this, "No internet access",Toast.LENGTH_LONG).show();
                    else {
                        if (editTextUsername.getText().toString().equals(""))
                            editTextUsername.setError("Guard ID is required!");
                        else if (editTextPhone.getText().toString().equals(""))
                            editTextPhone.setError("Phone is required!");
                        else if (editTextName.getText().toString().equals(""))
                            editTextName.setError("Name is required!");
                        else {
                            final ProgressDialog pd = new ProgressDialog(SignUp.this);
                            pd.setCancelable(false);
                            pd.setMessage("Checking...");
                            pd.show();
                            final String username = editTextUsername.getText().toString();
                            final String phone = editTextPhone.getText().toString();
                            final String guardname = editTextName.getText().toString();
                            final int scheduleID = scheduleList.getSelectedItemPosition();
                            db.collection("Users").whereEqualTo("guardID", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (!task.getResult().isEmpty()) {
                                        editTextUsername.setError("Guard ID is taken!");
                                        pd.dismiss();
                                    } else {
                                        pd.setMessage("Information accepted! Registering...");
                                        Map<String, Object> newGuard = new HashMap<>();
                                        newGuard.put("guardID", username);
                                        newGuard.put("phone", phone);
                                        newGuard.put("guardName", guardname);
                                        newGuard.put("registrationToken", registrationToken);
                                        newGuard.put("scheduleID", scheduleID);
                                        db.collection("Users").add(newGuard).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                pd.dismiss();
                                                SharedPreferences.Editor editor = preferences.edit();
                                                editor.putString("guardID", username);
                                                editor.apply();
                                                Intent intent = new Intent(SignUp.this, ActivitiesList.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });

                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
        else {
            Intent intent = new Intent(this, ActivitiesList.class);
            startActivity(intent);
            finish();
        }

    }

    private void showErrorDialog(){
        AlertDialog.Builder errorMessage = new AlertDialog.Builder(SignUp.this);
        errorMessage.setMessage("Invalid guard ID!").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showInputDialog();
            }
        });
        errorMessage.setCancelable(false);
        errorMessage.show();
    }

    private void showInputDialog(){
        final AlertDialog.Builder inputID = new AlertDialog.Builder(SignUp.this);
        inputID.setMessage("Enter your guard ID:");
        inputID.setCancelable(false);
        final EditText editTextGuardID = new EditText(SignUp.this);
        editTextGuardID.setInputType(InputType.TYPE_CLASS_TEXT);
        inputID.setView(editTextGuardID);
        inputID.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(checkNetwork()) {
                    final ProgressDialog pd = new ProgressDialog(SignUp.this);
                    pd.setMessage("Loading...");
                    pd.setCancelable(false);
                    pd.show();
                    final String guardID = editTextGuardID.getText().toString();
                    db.collection("Users").whereEqualTo("guardID", guardID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.getResult().isEmpty()) {
                                showErrorDialog();
                            } else {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("guardID", guardID);
                                editor.apply();
                                if (!registrationToken.equals(task.getResult().getDocuments().get(0).getString("registrationToken"))) {
                                    String docID = task.getResult().getDocuments().get(0).getId();
                                    Map<String, Object> newToken = new HashMap<>();
                                    newToken.put("registrationToken", registrationToken);
                                    db.collection("Users").document(docID).update(newToken).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Intent intent = new Intent(SignUp.this, ActivitiesList.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                }

                            }
                            pd.dismiss();
                        }
                    });
                }
                else {
                    Toast.makeText(SignUp.this, "No internet access!", Toast.LENGTH_SHORT).show();
                    alert.show();
                }
            }
        });
        inputID.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alert.show();
            }
        });
        inputID.show();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Permission to access location")
                        .setMessage("Permission to access location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(SignUp.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(SignUp.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        return;
    }


    public boolean checkNetwork(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo()!=null;
    }



}
