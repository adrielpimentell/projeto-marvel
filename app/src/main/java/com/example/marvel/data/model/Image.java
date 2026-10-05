package com.example.marvel.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Locale;

public class Image implements Serializable {

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("thumb_url")
    private String thumbUrl;

    @SerializedName("small_url")
    private String smallUrl;

    @SerializedName("medium_url")
    private String mediumUrl;

    @SerializedName("screen_url")
    private String screenUrl;

    @SerializedName("screen_large_url")
    private String screenLargeUrl;

    @SerializedName("super_url")
    private String superUrl;

    @SerializedName("original_url")
    private String originalUrl;

    public String getCardUrl() {
        return firstValid(smallUrl, mediumUrl, thumbUrl, screenUrl, iconUrl);
    }

    public String getLargeUrl() {
        return firstValid(superUrl, screenLargeUrl, originalUrl, mediumUrl, smallUrl);
    }

    private static String firstValid(String... urls) {
        for (String url : urls) {
            if (url != null && !url.trim().isEmpty() && !isPlaceholder(url)) {
                return url;
            }
        }
        return null;
    }

    private static boolean isPlaceholder(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return fileName.contains("blank");
    }
}
