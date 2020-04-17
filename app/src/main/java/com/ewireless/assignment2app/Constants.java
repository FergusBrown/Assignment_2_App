package com.ewireless.assignment2app;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Stores some constants used for geofencing
 * @author Tianyi Zhang
 */
public class Constants {

    //Location
    public static final String GEOFENCE_ID = "home";

    /**
     * Map for storing information about Edinburgh
     */
    public static final HashMap<String, LatLng> AREA_LANDMARKS = new HashMap<String, LatLng>();

    static {

        AREA_LANDMARKS.put(GEOFENCE_ID, new LatLng(55.953251, -3.188267));
    }
}