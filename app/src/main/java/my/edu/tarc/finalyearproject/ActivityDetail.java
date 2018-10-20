package my.edu.tarc.finalyearproject;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityDetail extends MenuActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, NetworkStateReceiver.NetworkStateReceiverListener {
    TextView textViewActivityID, textViewFloorLevel, textViewInstruction, textViewNoLocation, textViewCCTVDescription, textViewNoNetwork;
    ImageView imageViewActivityImage;
    Button buttonTakeAction;
    SupportMapFragment mapFragment;
    FirebaseFirestore db;
    private double longitude, latitude;
    LatLng activityLocation;
    String imageName, activityStatus;
    int cctvID, activityID;
    ProgressDialog pd;
    GoogleMap map;
    ArrayList<LatLng> positionList;
    GoogleApiClient apiClient;
    LocationRequest locationRequest;
    Marker guardLocationMarker;
    boolean incharge = false, resolved = false;
    SharedPreferences preferences;
    String guardID;
    ListView guardListView;
    GuardsListAdapter adapter;
    List<Guard> guardList;
    ScrollView scrollView;
    NetworkStateReceiver networkStateReceiver;
    FusedLocationProviderClient locationProviderClient;
    LocationCallback locationCallback;
    Polyline polyline;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent data = getIntent();
        imageName = data.getStringExtra("imageName");
        cctvID = data.getIntExtra("cctvID", 0);
        activityID = data.getIntExtra("activityID", 0);
        activityStatus = data.getStringExtra("activityStatus");
        textViewNoLocation = findViewById(R.id.textViewNoLocation);
        textViewCCTVDescription = findViewById(R.id.textViewCCTVDescription);
        textViewNoNetwork = findViewById(R.id.textViewNoNetwork);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings dbSetting = new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
        db.setFirestoreSettings(dbSetting);
        textViewActivityID = findViewById(R.id.textViewActivityID);
        imageViewActivityImage = findViewById(R.id.imageViewActivityImage);
        textViewFloorLevel = findViewById(R.id.textViewFloorLevel);
        textViewActivityID.setText("Activity ID: " + activityID);
        textViewInstruction = findViewById(R.id.textViewInstruction);
        mapFragment = (MyMapView) getSupportFragmentManager().findFragmentById(R.id.fragmentMap);
        scrollView = findViewById(R.id.scrollView);
        positionList = new ArrayList<>();
        buttonTakeAction = findViewById(R.id.buttonTakeAction);
        guardListView = findViewById(R.id.guardsList);
        guardList = new ArrayList<>();
        adapter = new GuardsListAdapter(guardList, ActivityDetail.this);
        guardListView.setAdapter(adapter);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        guardID = preferences.getString("guardID", "");

        ((MyMapView) getSupportFragmentManager().findFragmentById(R.id.fragmentMap)).setListener(new MyMapView.OnTouchListener() {
            @Override
            public void onTouch() {
                scrollView.requestDisallowInterceptTouchEvent(true);
            }
        });

        backgroundChecking();
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        MarkerOptions options = new MarkerOptions();

        // Setting the position of the marker
        options.position(activityLocation);
        options.title("Location of Abnormal Activity");
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        map.addMarker(options).showInfoWindow();


        map.moveCamera(CameraUpdateFactory.newLatLng(activityLocation));
        map.animateCamera(CameraUpdateFactory.zoomTo(16));

        if(checkLocationPermission()) {
            buildGoogleApiClient();
            map.setMyLocationEnabled(true);
        }


        buttonTakeAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkLocationPermission()) {
                    if (incharge) {
                        Intent intent = new Intent(ActivityDetail.this, ActivityStatus.class);
                        intent.putExtra("activityID", activityID);
                        intent.putExtra("activityImage", imageName);
                        intent.putExtra("activityStatus", activityStatus);
                        startActivity(intent);
                    } else {
                        pd.show();
                        textViewInstruction.setText("Please follow the route to the location of abnormal activity");
                        textViewInstruction.setVisibility(View.VISIBLE);
                        buttonTakeAction.setBackgroundColor(getResources().getColor(R.color.holo_green_dark));
                        buttonTakeAction.setText("Update Status");
                        incharge = true;
                        Map<String, Object> newActivity = new HashMap<>();
                        newActivity.put("activityID", activityID);
                        newActivity.put("dateTime", FieldValue.serverTimestamp());
                        newActivity.put("guardID", guardID);
                        db.collection("GuardActivity").add(newActivity);
                        db.collection("AbnormalActivity").whereEqualTo("activityID", activityID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                Map<String, Object> updateStatus = new HashMap<>();
                                updateStatus.put("activityStatus", "Processing");
                                String docID = task.getResult().getDocuments().get(0).getId();
                                db.collection("AbnormalActivity").document(docID).update(updateStatus);
                                activityStatus = "Processing";
                            }
                        });
                        String url = getDirectionsUrl(guardLocationMarker.getPosition(), activityLocation);
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute(url);
                    }
                } else
                    Toast.makeText(ActivityDetail.this, "Please grant location permission first!", Toast.LENGTH_SHORT).show();
            }
        });




    }

    protected synchronized void buildGoogleApiClient() {
        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        apiClient.connect();
    }


    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);

        return url;
    }


    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1500);
        locationRequest.setFastestInterval(1500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                double lat = 0, lon = 0;
                if (guardLocationMarker != null) {
                    lat = guardLocationMarker.getPosition().latitude;
                    lon = guardLocationMarker.getPosition().longitude;
                }
                for (Location l : locationResult.getLocations()) {
                    lat = l.getLatitude();
                    lon = l.getLongitude();
                }
                LatLng latLng = new LatLng(lat, lon);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                BitmapDrawable img = (BitmapDrawable) getDrawable(R.drawable.guard);
                Bitmap b = img.getBitmap();
                Bitmap smallIcon = Bitmap.createScaledBitmap(b, 120, 120, false);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
                if (guardLocationMarker == null) {
                    if(checkLocationPermission())
                        map.setMyLocationEnabled(true);
                    map.setIndoorEnabled(true);
                    UiSettings uiSettings = map.getUiSettings();
                    uiSettings.setIndoorLevelPickerEnabled(true);
                    uiSettings.setMyLocationButtonEnabled(true);
                    uiSettings.setMapToolbarEnabled(true);
                    uiSettings.setCompassEnabled(true);
                    uiSettings.setZoomControlsEnabled(true);

                    guardLocationMarker = map.addMarker(markerOptions);

                } else {
                    guardLocationMarker.setPosition(latLng);
                }

                if (incharge && !resolved && checkNetwork()) {
                    String url = getDirectionsUrl(latLng, activityLocation);
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                }
            }
        };

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings requirements are satisfied. We now initialize location requests here.
                try {
                    locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                } catch (SecurityException unlikely) {
                }
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                locationProviderClient.removeLocationUpdates(locationCallback);
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(ActivityDetail.this, 103);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            //Location settings check result
            case 103:
                switch (resultCode) {
                    case RESULT_OK:
                        map.moveCamera(CameraUpdateFactory.newLatLng(activityLocation));
                        map.animateCamera(CameraUpdateFactory.zoomTo(16));
                        break;
                    case RESULT_CANCELED:
                        Toast.makeText(this, "GPS is essential for this application.", Toast.LENGTH_SHORT).show();;
                        break;
                    default:
                        break;
                }
                break;
        }
    }





    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }


        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    Log.d("lat", ""+lat);
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }
                Log.d("points", ""+points);
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                if(pd.isShowing())
                    pd.dismiss();
                if(polyline!=null)
                    polyline.remove();
                polyline = map.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(map!=null && checkLocationPermission())
            buildGoogleApiClient();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadRun=false;
        networkStateReceiver.removeListener(this);
        this.unregisterReceiver(networkStateReceiver);
    }

    @Override
    public void networkAvailable() {
        textViewCCTVDescription.setVisibility(View.VISIBLE);
        textViewNoNetwork.setVisibility(View.GONE);
        pd = new ProgressDialog(ActivityDetail.this);
        pd.setMessage("Loading...");
        pd.setCancelable(false);
        pd.show();
        pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(scrollView.getScrollY()!=0)
                    scrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        });


        StorageReference imageStorage = FirebaseStorage.getInstance().getReference();
        imageStorage.child(imageName).getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                Uri imageUri = task.getResult();
                Glide.with(ActivityDetail.this)
                        .load(imageUri)
                        .into(imageViewActivityImage);
            }
        });

        db.collection("AbnormalActivity").whereEqualTo("activityID", activityID).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots.getDocuments().get(0).get("activityStatus").toString().equals("Resolved")) {
                    resolved = true;
                    textViewInstruction.setVisibility(View.VISIBLE);
                    textViewInstruction.setText("This activity has been resolved");
                    textViewInstruction.setTextColor(getResources().getColor(R.color.holo_green_dark));
                    buttonTakeAction.setVisibility(View.GONE);
                    pd.dismiss();
                }
                db.collection("CCTV").whereEqualTo("cctvID", cctvID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        latitude = task.getResult().getDocuments().get(0).getDouble("cctvLatitude");
                        longitude = task.getResult().getDocuments().get(0).getDouble("cctvLongtitude");
                        activityLocation = new LatLng(latitude, longitude);
                        textViewCCTVDescription.setText("CCTV Description: "+task.getResult().getDocuments().get(0).getString("cctvDescription"));
                        textViewFloorLevel.setText("Floor Level: " + task.getResult().getDocuments().get(0).get("cctvFloorLevel"));
                        db.collection("GuardActivity").whereEqualTo("activityID", activityID).whereEqualTo("guardID", guardID).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (!task.getResult().isEmpty() && !resolved) {
                                    incharge = true;
                                    textViewInstruction.setText("Please follow the route to the location of abnormal activity");
                                    textViewInstruction.setVisibility(View.VISIBLE);
                                    buttonTakeAction.setBackgroundColor(getResources().getColor(R.color.holo_green_dark));
                                    buttonTakeAction.setText("Update Status");
                                }
                                if(!incharge || !checkLocationPermission())
                                    pd.dismiss();
                            }
                        });
                        mapFragment.getMapAsync(ActivityDetail.this);

                    }
                });


            }

        });

        db.collection("GuardActivity").whereEqualTo("activityID", activityID).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                guardList.clear();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    db.collection("Users").whereEqualTo("guardID", doc.getString("guardID")).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (!task.getResult().isEmpty()) {
                                Guard guard = new Guard(task.getResult().getDocuments().get(0).getString("guardID"), task.getResult().getDocuments().get(0).getString("guardName"), task.getResult().getDocuments().get(0).get("phone").toString());
                                guardList.add(guard);
                                adapter.notifyDataSetChanged();
                                if(scrollView.getScrollY()!=0)
                                    scrollView.fullScroll(ScrollView.FOCUS_UP);
                            }
                        }
                    });
                }

            }
        });

    }

    @Override
    public void networkUnavailable() {
        buttonTakeAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ActivityDetail.this, "Please turn on internet connection!", Toast.LENGTH_SHORT).show();
            }
        });
        textViewNoNetwork.setVisibility(View.VISIBLE);
    }

    private boolean checkLocationPermission(){
        boolean turnedOn = ContextCompat.checkSelfPermission(ActivityDetail.this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        if(turnedOn) {
            textViewNoLocation.setVisibility(View.GONE);
        }
        else
            textViewNoLocation.setVisibility(View.VISIBLE);
        return turnedOn;
    }

    Handler handler = new Handler();
    boolean threadRun = true;

    private void backgroundChecking(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(threadRun){
                    try{
                        Thread.sleep(1000);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    checkLocationPermission();
                                }
                            }
                        });
                    }
                    catch(Exception e){}
                }
            }
        }).start();
    }

    public boolean checkNetwork(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo()!=null;
    }
}
