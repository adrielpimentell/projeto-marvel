package com.example.marvel.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("deck")
    private String deck;

    @SerializedName("image")
    private Image image;

    @SerializedName("publisher")
    private NamedRef publisher;

    @SerializedName("count_of_team_members")
    private int memberCount;

    @SerializedName("characters")
    private List<NamedRef> members;

    public int getId() {
        return id;
    }

    public String getName() {
        return name == null || name.trim().isEmpty() ? "?" : name.trim();
    }

    public String getDeck() {
        return deck == null ? "" : deck.trim();
    }

    public String getCardImageUrl() {
        return image == null ? null : image.getCardUrl();
    }

    public String getLargeImageUrl() {
        return image == null ? null : image.getLargeUrl();
    }

    public boolean isMarvel() {
        return publisher != null && publisher.getId() == Character.MARVEL_PUBLISHER_ID;
    }

    public List<NamedRef> getMembers() {
        return members == null ? Collections.emptyList() : Collections.unmodifiableList(members);
    }

    public List<Integer> getMemberIds() {
        List<Integer> ids = new ArrayList<>();
        for (NamedRef member : getMembers()) {
            if (member != null) ids.add(member.getId());
        }
        return ids;
    }

    public int getMemberCount() {
        return Math.max(memberCount, getMembers().size());
    }
}
