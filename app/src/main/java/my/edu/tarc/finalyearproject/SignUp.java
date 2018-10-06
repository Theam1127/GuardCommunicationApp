package my.edu.tarc.finalyearproject;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    Button buttonSignUp;
    EditText editTextUsername, editTextName, editTextPhone;
    SharedPreferences preferences;
    FirebaseFirestore db;
    boolean existingAcc = false;
    String docID;
    AlertDialog.Builder alert;
    String registrationToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String guardID = preferences.getString("guardID", "");
        registrationToken = FirebaseInstanceId.getInstance().getToken();
        if (guardID.equals("")) {
            setContentView(R.layout.activity_sign_up);
            buttonSignUp = findViewById(R.id.buttonRegister);
            editTextUsername = findViewById(R.id.editTextUsername);
            editTextName = findViewById(R.id.editTextName);
            editTextPhone = findViewById(R.id.editTextPhone);
            final ProgressDialog pd = new ProgressDialog(SignUp.this);
            pd.setMessage("Loading...");
            pd.show();
            db = FirebaseFirestore.getInstance();
            db.collection("Users").whereEqualTo("registrationToken", registrationToken).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(!task.getResult().isEmpty()){
                        existingAcc=true;
                        docID = task.getResult().getDocuments().get(0).getId();
                        alert = new AlertDialog.Builder(SignUp.this);
                        alert.setMessage("This phone has been registered in our database. Do you want to login your account?");
                        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                showInputDialog();
                            }
                        });
                        alert.setNegativeButton("Sign Up New Account", null);
                        alert.show();
                    }
                    pd.dismiss();
                }
            });
            buttonSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String username = editTextUsername.getText().toString();
                    final String phone = editTextPhone.getText().toString();
                    final String guardname = editTextName.getText().toString();
                    db.collection("Users").whereEqualTo("guardID", username).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(!task.getResult().isEmpty()){
                                editTextUsername.setError("Guard ID is taken!");
                            }
                            else if(editTextUsername.getText().equals(""))
                                editTextUsername.setError("Guard ID is required!");
                            else{
                                Map<String, Object> newGuard = new HashMap<>();
                                newGuard.put("guardID", username);
                                newGuard.put("phone", phone);
                                newGuard.put("guardName", guardname);
                                newGuard.put("registrationToken", registrationToken);
                                if(existingAcc)
                                    db.collection("Users").document(docID).update(newGuard);
                                else
                                    db.collection("Users").add(newGuard);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("guardID", username);
                                editor.apply();
                                Intent intent = new Intent(SignUp.this, ActivitiesList.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
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
        errorMessage.setMessage("Invalid guard ID!").setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showInputDialog();
            }
        });
        errorMessage.show();
    }

    private void showInputDialog(){
        final AlertDialog.Builder inputID = new AlertDialog.Builder(SignUp.this);
        inputID.setMessage("Enter your guard ID:");
        final EditText editTextGuardID = new EditText(SignUp.this);
        editTextGuardID.setInputType(InputType.TYPE_CLASS_TEXT);
        inputID.setView(editTextGuardID);
        inputID.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String guardID = editTextGuardID.getText().toString();
                db.collection("Users").whereEqualTo("guardID", guardID).whereEqualTo("registrationToken", registrationToken).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.getResult().isEmpty()) {
                            showErrorDialog();
                        }
                        else{
                            Intent intent = new Intent(SignUp.this, ActivitiesList.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
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
}