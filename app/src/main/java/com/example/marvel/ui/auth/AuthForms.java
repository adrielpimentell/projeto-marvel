package com.example.marvel.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.marvel.MainActivity;
import com.example.marvel.R;
import com.google.android.material.textfield.TextInputLayout;

final class AuthForms {

    static final int MIN_PASSWORD_LENGTH = 6;

    private AuthForms() {
    }

    static String text(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    static String password(EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    static boolean requireEmail(TextInputLayout layout, String email) {
        if (email.isEmpty()) {
            layout.setError(layout.getContext().getString(R.string.auth_error_required));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layout.setError(layout.getContext().getString(R.string.auth_error_invalid_email));
            return false;
        }
        return true;
    }

    static boolean require(TextInputLayout layout, String value) {
        if (value.isEmpty()) {
            layout.setError(layout.getContext().getString(R.string.auth_error_required));
            return false;
        }
        return true;
    }

    static void clearErrorsOnEdit(TextView generalError, TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            EditText input = layout.getEditText();
            if (input == null) continue;
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    layout.setError(null);
                    generalError.setVisibility(View.GONE);
                }
            });
        }
    }

    static void showError(TextView generalError, int messageRes) {
        generalError.setText(messageRes);
        generalError.setVisibility(View.VISIBLE);
    }

    static void setEnabled(boolean enabled, View... views) {
        for (View view : views) {
            view.setEnabled(enabled);
        }
    }

    static Intent homeIntent(Context context) {
        return new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }
}
