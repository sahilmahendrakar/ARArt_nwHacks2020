package com.google.ar.core.examples.java.augmentedimage.models;

import com.google.android.gms.maps.model.LatLng;

public class Mural {
    String refImg;
    String arImg;
    Location location;

    public Mural(String refImg, String arImg, Location loc) {
        this.refImg = refImg;
        this.arImg = arImg;
        this.location = loc;
    }

    public String getRefImg() {
        return refImg;
    }

    public void setRefImg(String refImg) {
        this.refImg = refImg;
    }

    public String getArImg() {
        return arImg;
    }

    public void setArImg(String arImg) {
        this.arImg = arImg;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
