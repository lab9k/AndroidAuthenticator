package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

public class CheckinPost {
    @SerializedName("userid")
    private String userid;
    @SerializedName("locationid")
    private String locationid;

    public CheckinPost(String userid, String locationid) {
        this.userid = userid;
        this.locationid = locationid;
    }

    public CheckinPost() {
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getLocationid() {
        return locationid;
    }

    public void setLocationid(String locationid) {
        this.locationid = locationid;
    }
}
