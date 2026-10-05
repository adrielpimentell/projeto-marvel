package com.example.marvel.ui.common;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.example.marvel.R;

public final class SearchBar {

    public interface Listener {
        void onQueryChanged(String query);
    }

    public static final long DEFAULT_DELAY_MS = 300;

    private final EditText input;
    private final View clear;
    private final long delayMs;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable notifyLater = this::notifyNow;
    private String lastQuery = "";

    public SearchBar(View bar, int hintRes, long delayMs, Listener listener) {
        this.input = bar.findViewById(R.id.search_input);
        this.clear = bar.findViewById(R.id.search_clear);
        this.delayMs = delayMs;
        this.listener = listener;
        input.setHint(hintRes);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                clear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(notifyLater);
                handler.postDelayed(notifyLater, SearchBar.this.delayMs);
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            handler.removeCallbacks(notifyLater);
            notifyNow();
            hideKeyboard();
            return true;
        });
        clear.setOnClickListener(v -> {
            input.setText("");
            handler.removeCallbacks(notifyLater);
            notifyNow();
        });
    }

    public String getQuery() {
        return input.getText().toString().trim();
    }

    public void hideKeyboard() {
        InputMethodManager keyboard = input.getContext().getSystemService(InputMethodManager.class);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
    }

    public void release() {
        handler.removeCallbacks(notifyLater);
    }

    private void notifyNow() {
        String query = getQuery();
        if (query.equals(lastQuery)) return;
        lastQuery = query;
        listener.onQueryChanged(query);
    }
}
