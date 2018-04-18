package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("checkin")
    private Checkin checkin;
    @SerializedName("picture")
    private String picture;
    @SerializedName("phoneid")
    private String phoneid;
    @SerializedName("role")
    private String role;
    @SerializedName("messages")
    private Message[] messages;
    @SerializedName("accountType")
    private String accountType;

    public User(String id, String name, Checkin checkin, String picture, String phoneid, String role, Message[] messages, String accountType) {
        this.id = id;
        this.name = name;
        this.checkin = checkin;
        this.picture = picture;
        this.phoneid = phoneid;
        this.role = role;
        this.messages = messages;
        this.accountType = accountType;
    }

    public User() {
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

    public Checkin getCheckin() {
        return checkin;
    }

    public void setCheckin(Checkin checkin) {
        this.checkin = checkin;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getPhoneid() {
        return phoneid;
    }

    public void setPhoneid(String phoneid) {
        this.phoneid = phoneid;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Message[] getMessages() {
        return messages;
    }

    public void setMessages(Message[] messages) {
        this.messages = messages;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
