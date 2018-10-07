package my.edu.tarc.finalyearproject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ActivityDetail extends AppCompatActivity implements OnMapReadyCallback{

    TextView textViewActivityID;
    ImageView imageViewActivityImage;
    StorageReference imageStorage;
    SupportMapFragment mapFragment;
    FirebaseFirestore db;
    private static final String MAP_VIEW_BUNDLE_KEY = "AbnormalMapBundleKey";
    private double longitude, latitude, guardLongitude, guardLatitude;
    String imageName;
    int cctvID, activityID;
    ProgressDialog pd;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent data = getIntent();
        imageName = data.getStringExtra("imageName");
        cctvID = data.getIntExtra("cctvID", 0);
        activityID = data.getIntExtra("activityID", 0);
        textViewActivityID = findViewById(R.id.textViewActivityID);
        imageViewActivityImage = findViewById(R.id.imageViewActivityImage);
        textViewActivityID.setText("Activity ID: " + activityID);
        imageStorage = FirebaseStorage.getInstance().getReference();
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMap);




        db = FirebaseFirestore.getInstance();
        pd = new ProgressDialog(ActivityDetail.this);
        pd.setMessage("Loading...");
        pd.show();

        imageStorage.child(imageName).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                Uri imageUri = task.getResult();
                Glide.with(ActivityDetail.this)
                        .load(imageUri)
                        .into(imageViewActivityImage);
            }
        });
        db.collection("CCTV").whereEqualTo("cctvID", cctvID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                latitude = task.getResult().getDocuments().get(0).getDouble("cctvLatitude");
                longitude = task.getResult().getDocuments().get(0).getDouble("cctvLongtitude");
                pd.dismiss();
                mapFragment.getMapAsync(ActivityDetail.this);
            }
        });


    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        LatLng location = new LatLng(latitude, longitude);

        googleMap.setIndoorEnabled(true);
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        googleMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Location of Abnormal Activity")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))).showInfoWindow();

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(19));
    }
}
