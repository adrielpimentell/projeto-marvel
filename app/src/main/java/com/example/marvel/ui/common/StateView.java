package com.example.marvel.ui.common;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.marvel.R;
import com.google.android.material.button.MaterialButton;

public final class StateView {

    private final View root;
    private final ImageView icon;
    private final TextView title;
    private final TextView message;
    private final MaterialButton button;

    public StateView(View root) {
        this.root = root;
        this.icon = root.findViewById(R.id.state_icon);
        this.title = root.findViewById(R.id.state_title);
        this.message = root.findViewById(R.id.state_message);
        this.button = root.findViewById(R.id.state_button);
    }

    public void show(int iconRes, CharSequence titleText, CharSequence messageText,
                     CharSequence buttonText, Runnable action) {
        icon.setImageResource(iconRes);
        title.setText(titleText);
        title.setVisibility(titleText == null || titleText.length() == 0 ? View.GONE : View.VISIBLE);
        message.setText(messageText);
        button.setVisibility(buttonText == null ? View.GONE : View.VISIBLE);
        button.setText(buttonText);
        button.setOnClickListener(buttonText == null || action == null ? null : v -> action.run());
        root.setVisibility(View.VISIBLE);
    }

    public void hide() {
        root.setVisibility(View.GONE);
    }

    public boolean isVisible() {
        return root.getVisibility() == View.VISIBLE;
    }
}
