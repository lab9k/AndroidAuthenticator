package com.wannes.digipresence.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("_id")
    private String id;
    @SerializedName("sender")
    private User sender;
    @SerializedName("subject")
    private String subject;
    @SerializedName("content")
    private String content;
    @SerializedName("isRead")
    private boolean isRead;

    public Message(String id, User sender, String subject, String content, boolean isRead) {
        this.id = id;
        this.sender = sender;
        this.subject = subject;
        this.content = content;
        this.isRead = isRead;
    }

    public Message() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
