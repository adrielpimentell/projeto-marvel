package com.example.marvel.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Character implements Serializable {

    public static final int MARVEL_PUBLISHER_ID = 31;

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("real_name")
    private String realName;

    @SerializedName("aliases")
    private String aliases;

    @SerializedName("deck")
    private String deck;

    @SerializedName("site_detail_url")
    private String siteDetailUrl;

    @SerializedName("image")
    private Image image;

    @SerializedName("publisher")
    private NamedRef publisher;

    @SerializedName("count_of_issue_appearances")
    private int issueAppearances;

    @SerializedName("first_appeared_in_issue")
    private NamedRef firstAppearance;

    @SerializedName("origin")
    private NamedRef origin;

    @SerializedName("powers")
    private List<NamedRef> powers;

    @SerializedName("teams")
    private List<NamedRef> teams;

    @SerializedName("movies")
    private List<NamedRef> movies;

    public boolean isMarvel() {
        if (publisher == null) return false;
        if (publisher.getId() == MARVEL_PUBLISHER_ID) return true;
        String publisherName = publisher.getName();
        return publisherName.equalsIgnoreCase("Marvel")
                || publisherName.equalsIgnoreCase("Marvel Comics");
    }

    public boolean hasDetails() {
        return powers != null || teams != null || movies != null;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return isBlank(name) ? "Sem nome" : name.trim();
    }

    public String getRealName() {
        return isBlank(realName) ? "" : realName.trim();
    }

    public List<String> getAliases() {
        List<String> result = new ArrayList<>();
        if (isBlank(aliases)) return result;
        for (String alias : aliases.split("\\r?\\n")) {
            if (!alias.trim().isEmpty()) result.add(alias.trim());
        }
        return result;
    }

    public String getDeck() {
        return isBlank(deck) ? "" : deck.trim();
    }

    public String getSiteDetailUrl() {
        return isBlank(siteDetailUrl) ? null : siteDetailUrl.trim();
    }

    public String getCardImageUrl() {
        return image == null ? null : image.getCardUrl();
    }

    public String getLargeImageUrl() {
        return image == null ? null : image.getLargeUrl();
    }

    public NamedRef getPublisher() {
        return publisher;
    }

    public int getIssueAppearances() {
        return Math.max(0, issueAppearances);
    }

    public NamedRef getFirstAppearance() {
        return firstAppearance;
    }

    public NamedRef getOrigin() {
        return origin;
    }

    public List<NamedRef> getPowers() {
        return safe(powers);
    }

    public List<NamedRef> getTeams() {
        return safe(teams);
    }

    public List<NamedRef> getMovies() {
        return safe(movies);
    }

    private static List<NamedRef> safe(List<NamedRef> list) {
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
