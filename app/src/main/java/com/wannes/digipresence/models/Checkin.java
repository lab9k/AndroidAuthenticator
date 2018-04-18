package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

public class Checkin {
    @SerializedName("time")
    private double time;
    @SerializedName("location")
    private Location location;

    public Checkin(double time, Location location) {
        this.time = time;
        this.location = location;
    }

    public Checkin() {
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
