package com.example.marvel.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {

    public static final int STATUS_OK = 1;
    public static final int STATUS_INVALID_API_KEY = 100;
    public static final int STATUS_OBJECT_NOT_FOUND = 101;

    @SerializedName("status_code")
    private int statusCode;

    @SerializedName("error")
    private String error;

    @SerializedName("limit")
    private int limit;

    @SerializedName("offset")
    private int offset;

    @SerializedName("number_of_page_results")
    private int numberOfPageResults;

    @SerializedName("number_of_total_results")
    private int numberOfTotalResults;

    @SerializedName("results")
    private T results;

    public boolean isOk() {
        return statusCode == STATUS_OK;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public int getNumberOfPageResults() {
        return numberOfPageResults;
    }

    public int getNumberOfTotalResults() {
        return numberOfTotalResults;
    }

    public T getResults() {
        return results;
    }
}
