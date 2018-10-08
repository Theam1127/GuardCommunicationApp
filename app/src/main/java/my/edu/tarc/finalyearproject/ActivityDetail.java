package my.edu.tarc.finalyearproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Criteria;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
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

public class ActivityDetail extends AppCompatActivity implements OnMapReadyCallback,LocationListener,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    TextView textViewActivityID;
    ImageView imageViewActivityImage;
    StorageReference imageStorage;
    SupportMapFragment mapFragment;
    FirebaseFirestore db;
    private double longitude, latitude;
    LatLng activityLocation;
    String imageName;
    int cctvID, activityID;
    ProgressDialog pd;
    GoogleMap map;
    ArrayList<LatLng> positionList;
    GoogleApiClient apiClient;
    LocationRequest locationRequest;
    Marker guardLocationMarker;


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
        positionList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        pd = new ProgressDialog(ActivityDetail.this);
        pd.setMessage("Loading...");
        pd.setCancelable(false);
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
                activityLocation = new LatLng(latitude,longitude);
                mapFragment.getMapAsync(ActivityDetail.this);
            }
        });


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setIndoorEnabled(true);
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                map.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            map.setMyLocationEnabled(true);
        }

        // Creating MarkerOptions



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
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters+"&key="+getString(R.string.google_maps_key);

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
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;

            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ex) {}

            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch(Exception ex) {}

            if(!gps_enabled && !network_enabled) {
                // notify user
                AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityDetail.this);
                dialog.setMessage("GPS or Network is not available!");
                dialog.setPositiveButton("Turn on location setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub
                        Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        getApplicationContext().startActivity(myIntent);
                        //get gps
                    }
                });
                dialog.setCancelable(false);
                dialog.show();
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        BitmapDrawable img = (BitmapDrawable)getResources().getDrawable(R.drawable.guard);
        Bitmap b = img.getBitmap();
        Bitmap smallIcon = Bitmap.createScaledBitmap(b, 120,120,false);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
        if(guardLocationMarker==null) {
            guardLocationMarker = map.addMarker(markerOptions);
            pd.dismiss();
        }
        else {
            guardLocationMarker.setPosition(latLng);
        }

        MarkerOptions options = new MarkerOptions();

        // Setting the position of the marker
        options.position(activityLocation);
        options.title("Location of Abnormal Activity");
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        map.addMarker(options);

        String url = getDirectionsUrl(latLng, activityLocation);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);


        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.zoomTo(16));
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
                map.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }



    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (apiClient == null) {
                            buildGoogleApiClient();
                        }
                        map.setMyLocationEnabled(true);
                    }
                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
