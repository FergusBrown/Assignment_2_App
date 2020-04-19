package com.ewireless.assignment2app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

/**
 * Activity to open Google maps and places a marker on the chosen postcode
 * @author Jialin Fan s1952282
 */
public class StartMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    String bestProvider;
    double tLat;
    double tLong;
    double homeLat;
    double homeLnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_map);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new myLocationListener();

        //judge the weather the gps open
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this,"Open GPS",Toast.LENGTH_LONG).show();
        }
        // judge the android version weather > M , and then it will remind wether need to access location information
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        // judge the android version weather > M and weather allow the app use the online service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
            }
        }

        // obtain the provider , provide information for manager
        bestProvider = locationManager.getBestProvider(getCriteria(), true);
        locationManager.requestLocationUpdates(bestProvider, 1000, 5, locationListener);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        // register location listener
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume(){
        super.onResume();
        setUpMapIfNeeded();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Reopen the app, reacquire location information and network permissions
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        bestProvider=locationManager.getBestProvider(getCriteria(),true);
        locationManager.requestLocationUpdates(bestProvider, 1000, 5, locationListener);
    }





    class myLocationListener implements LocationListener {

        //if location changed , access lat and long
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                tLat = location.getLatitude();
                tLong = location.getLongitude();
                // update map again
                updateMap();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    // update map and create lat and long object
    private void updateMap() {
        LatLng latLng = new LatLng(tLat, tLong);
        mMap.addMarker(new MarkerOptions().position(latLng).title("New location"));

        // control the zoom of map , v:13 means zoom size
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,13));
    }

    //The current app switches the background and then opens, the role of reloading
    private void setUpMapIfNeeded() {
        if(mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync( this);
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        // show the map
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        Intent intent = getIntent();

        //according the package name to access location information
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(this);
        try{
            //Address into coordinates
            addressList = geocoder.getFromLocationName(message, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // create lat and long
        Address address = addressList.get(0);
        homeLat = address.getLatitude();
        homeLnt = address.getLongitude();

        // Add a marker ---- home
        LatLng LatLngInput = new LatLng(address.getLatitude(),address.getLongitude());
        mMap.addMarker(new MarkerOptions().position(LatLngInput).title("My Home"));

        // zoom of size
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLngInput,12));

        // allow access location
        mMap.setMyLocationEnabled(true);

        // map can change at any time
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
    }

    // selecting a location provider
    private Criteria getCriteria() {

        // create a criteria object
        Criteria criteria = new Criteria();

        // settings does not require elevation data
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        //set to allow produce cost
        criteria.setCostAllowed(true);
        // request low power consumption
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }
}
