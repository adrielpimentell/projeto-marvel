package com.example.marvel.data.repository;

import retrofit2.Call;

public final class RequestHandle {

    private Call<?> currentCall;
    private boolean cancelled;

    void setCurrentCall(Call<?> call) {
        this.currentCall = call;
    }

    public void cancel() {
        cancelled = true;
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
