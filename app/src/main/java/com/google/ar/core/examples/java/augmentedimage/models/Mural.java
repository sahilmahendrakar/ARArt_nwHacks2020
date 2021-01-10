package com.google.ar.core.examples.java.augmentedimage.models;

public class Mural {
    String key;
    String name;
    String refImg;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    String arImg;
    LocationMural location;

    String prompt;
    String type;

    public Mural(String key, String name, String refImg, String arImg, LocationMural location, String prompt, String type) {
        this.key = key;
        this.name = name;
        this.refImg = refImg;
        this.arImg = arImg;
        this.location = location;
        this.prompt = prompt;
        this.type = type;
    }

    public Mural(String name, String refImg, String arImg, LocationMural location) {
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

    public LocationMural getLocation() {
        return location;
    }

    public void setLocation(LocationMural location) {
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
