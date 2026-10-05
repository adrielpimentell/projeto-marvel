package com.example.marvel.data.repository;

public interface RepoCallback<T> {

    void onSuccess(T data);

    void onError(String message);
}
