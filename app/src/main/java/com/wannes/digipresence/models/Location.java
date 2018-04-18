package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Location {
    @SerializedName("_id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("stickers")
    private List<String> stickers;
    @SerializedName("doNotDisturb")
    private boolean doNotDisturb;

    public Location(String id, String name, List<String> stickers, boolean doNotDisturb) {
        this.id = id;
        this.name = name;
        this.stickers = stickers;
        this.doNotDisturb = doNotDisturb;
    }

    public Location() {
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

    public List<String> getStickers() {
        return stickers;
    }

    public void setStickers(List<String> stickers) {
        this.stickers = stickers;
    }

    public boolean isDoNotDisturb() {
        return doNotDisturb;
    }

    public void setDoNotDisturb(boolean doNotDisturb) {
        this.doNotDisturb = doNotDisturb;
    }

    public void addSticker(String stickerid) {
        this.stickers.add(stickerid);
    }
}
