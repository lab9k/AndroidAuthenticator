package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Segment {
    @SerializedName("_id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("isVergadering")
    private boolean isVergadering;
    @SerializedName("locations")
    private List<Location> locations;

    public Segment(String id, String name, boolean isVergadering, List<Location> locations) {
        this.id = id;
        this.name = name;
        this.isVergadering = isVergadering;
        this.locations = locations;
    }

    public Segment() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVergadering() {
        return isVergadering;
    }

    public void setVergadering(boolean vergadering) {
        isVergadering = vergadering;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public void addLocation(Location location) {
        this.locations.add(location);
    }
}
