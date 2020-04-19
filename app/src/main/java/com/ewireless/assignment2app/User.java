package com.ewireless.assignment2app;

/**
 * User type to store in the database
 * @author Fergus Brown s1525959
 */
public class User {

    // variables must be public for firebase

    public String userKey;
    public String patientName;
    public String carerName;
    public String carerPhone;
    public String carerEmail;
    public String postCode;
    public int geofenceRadius;
    public double latitude;
    public double longitude;

    public User(String userKey,
                String patientName,
                String carerName,
                String carerPhone,
                String carerEmail,
                String postCode,
                int geofenceRadius,
                double latitude,
                double longitude) {

        this.userKey = userKey;
        this.patientName = patientName;
        this.carerName = carerName;
        this.carerPhone = carerPhone;
        this.carerEmail = carerEmail;
        this.postCode = postCode;
        this.geofenceRadius = geofenceRadius;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // TODO: create getters and setters

}
