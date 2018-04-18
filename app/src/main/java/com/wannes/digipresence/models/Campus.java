package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Campus {
    @SerializedName("_id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("isLunch")
    private boolean isLunch;
    @SerializedName("isThuiswerk")
    private boolean isThuiswerk;
    @SerializedName("segments")
    private List<Segment> segments;

    public Campus(String id, String name, boolean isLunch, boolean isThuiswerk, List<Segment> segments) {
        this.id = id;
        this.name = name;
        this.isLunch = isLunch;
        this.isThuiswerk = isThuiswerk;
        this.segments = segments;
    }

    public Campus() {
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

    public boolean isLunch() {
        return isLunch;
    }

    public void setLunch(boolean lunch) {
        isLunch = lunch;
    }

    public boolean isThuiswerk() {
        return isThuiswerk;
    }

    public void setThuiswerk(boolean thuiswerk) {
        isThuiswerk = thuiswerk;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }

    public void addSegment(Segment segment) {
        this.segments.add(segment);
    }
}
