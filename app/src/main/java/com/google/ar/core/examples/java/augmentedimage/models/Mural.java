package com.google.ar.core.examples.java.augmentedimage.models;

import com.google.android.gms.maps.model.LatLng;

public class Mural {
    String name;
    String refImg;
    String arImg;
    Location location;

    String prompt;
    String type;

    public Mural(String name, String refImg, String arImg, Location location, String prompt, String type) {
        this.name = name;
        this.refImg = refImg;
        this.arImg = arImg;
        this.location = location;
        this.prompt = prompt;
        this.type = type;
    }

    public Mural(String name, String refImg, String arImg, Location location) {
        this.name = name;
        this.refImg = refImg;
        this.arImg = arImg;
        this.location = location;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
