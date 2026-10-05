package com.example.marvel.data.repository;

public enum SortOption {
    CLASSIC("id:asc"),
    UPDATED("date_last_updated:desc"),
    NAME("name:asc");

    private final String apiValue;

    SortOption(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }
}
