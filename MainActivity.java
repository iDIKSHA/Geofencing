package com.example.github.geofencingmainactivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MapFragment mapFragment;
    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private static final int REQ_PERMISSION = 1;

    private LocationRequest locationRequest;

    private final int UPDATE_INTERVAL = 30 * 1000; // 1/2 minutes(30 seconds)
    private final int FASTEST_INTERVAL = (1 / 3) * 30 * 1000;  // 30 secs

    ArrayList<LatLng> locs = new ArrayList<LatLng>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize GoogleMaps
        initGMaps();

        // create GoogleApiClient
        createGoogleApi();
    }

    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void initGMaps() {
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mmap);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick(" + latLng + ")");
        markerForGeofence(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition());
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        map = googleMap;
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }

    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
        getLastKnownLocation();
    }

    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended()");

    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed()");
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if (checkPermission()) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        } else askPermission();
    }


    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
  /*  private final int UPDATE_INTERVAL =  1000;
    private final int FASTEST_INTERVAL = 900;
*/
    // Start location Updates
    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (checkPermission())
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged [" + location + "]");
        lastLocation = location;
    }

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
    }

    private Marker locationMarker;

    // Create a Location Marker
    private void markerLocation(LatLng latLng) {
        Log.i(TAG, "markerLocation(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if (map != null) {
            // Remove the anterior marker
            if (locationMarker != null)
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }
    }

    private Marker geoFenceMarker;
    private ArrayList<Marker> gfmarkr = new ArrayList<Marker>();

    // Create a marker for the geofence creation
    private void markerForGeofence(final LatLng latLng) {
        Log.i(TAG, "markerForGeofence(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title)
                .draggable(false);
        if (map != null) {
            // Remove last geoFenceMarker
          /*  if (geoFenceMarker != null)
                geoFenceMarker.remove();
            */
            geoFenceMarker = map.addMarker(markerOptions);
            gfmarkr.add(geoFenceMarker);
            // locs.add(latLng);

        }
    }

    private static final long GEO_DURATION = 3 * 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 350.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence(Marker latLng, float radius, String id) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(latLng.getPosition().latitude, latLng.getPosition().longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    // private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        //if (geoFencePendingIntent != null)
        //  return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request, final Marker mark) {
        Log.d(TAG, "addGeofence");
        if (checkPermission()) {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    Log.i(TAG, "onResult: " + status);
                    if (status.isSuccess()) {
                        drawGeofence(mark);
                        gfmarkrTracker.add(mark);
                    } else {
                        // inform about fail
                        Toast.makeText(getBaseContext(),"Could not addGeofence()",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits;

    private void drawGeofence(Marker mark) {
        Log.d(TAG, "drawGeofence()");

/*        if (geoFenceLimits != null)
            geoFenceLimits.remove();
*/
        CircleOptions circleOptions = new CircleOptions()
                .center(mark.getPosition())                    //888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEOFENCE_RADIUS);
        geoFenceLimits = map.addCircle(circleOptions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mmap: {
                startGeofence();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // Start Geofence creation process
    private String idd = "My Geofence";

    private void startGeofence() {
        Log.i(TAG, "startGeofence()");
        if (geoFenceMarker != null) {

            for (int i = 0; i < gfmarkr.size(); i++) {
                Geofence geofence = createGeofence(gfmarkr.get(i), GEOFENCE_RADIUS, idd + i);   //geoFenceMarker.getPosition()
                //  Geofence geofence = createGeofence(geoFenceMarker.getPosition(), GEOFENCE_RADIUS);
                GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
                addGeofence(geofenceRequest, gfmarkr.get(i));
            }
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    static Intent makeNotificationIntent(Context geofenceService, String msg) {
        Log.d(TAG, msg);
        return new Intent(geofenceService, MainActivity.class);
    }
}
