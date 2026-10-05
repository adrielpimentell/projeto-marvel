package com.example.marvel.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NamedRef implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("issue_number")
    private String issueNumber;

    public NamedRef() {
    }

    public NamedRef(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name == null ? "" : name.trim();
    }

    public String getIssueNumber() {
        return issueNumber;
    }
}
