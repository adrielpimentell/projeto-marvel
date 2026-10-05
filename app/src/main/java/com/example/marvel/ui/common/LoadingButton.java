package com.example.marvel.ui.common;

import android.view.View;

import com.example.marvel.R;
import com.google.android.material.button.MaterialButton;

public final class LoadingButton {

    private final MaterialButton button;
    private final View progress;
    private CharSequence label;
    private boolean loading;

    public LoadingButton(View root, int labelRes) {
        button = root.findViewById(R.id.loading_button);
        progress = root.findViewById(R.id.loading_progress);
        setLabel(labelRes);
    }

    public void setLabel(int labelRes) {
        label = button.getContext().getString(labelRes);
        if (!loading) {
            button.setText(label);
        }
    }

    public void setOnClickListener(View.OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
        button.setClickable(!loading);
        button.setText(loading ? "" : label);
        button.setContentDescription(loading ? button.getContext().getString(R.string.auth_loading) : null);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
