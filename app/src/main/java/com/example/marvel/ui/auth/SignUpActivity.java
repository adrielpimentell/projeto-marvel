package com.example.marvel.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.auth.PlayerName;
import com.example.marvel.ui.common.LoadingButton;
import com.example.marvel.ui.common.Screens;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpActivity extends AppCompatActivity {

    private static final String EXTRA_CHOOSE_NAME = "choose_name";

    private AuthRepository auth;
    private TextInputLayout emailLayout;
    private TextInputLayout nameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText emailInput;
    private TextInputEditText nameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private TextView errorText;
    private LoadingButton submit;
    private View loginLink;
    private boolean chooseNameOnly;
    private boolean busy;

    public static Intent newIntent(Context context) {
        return new Intent(context, SignUpActivity.class);
    }

    public static Intent newChooseNameIntent(Context context) {
        return newIntent(context).putExtra(EXTRA_CHOOSE_NAME, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_sign_up);

        auth = AuthRepository.getInstance(this);
        chooseNameOnly = getIntent().getBooleanExtra(EXTRA_CHOOSE_NAME, false);
        emailLayout = findViewById(R.id.sign_up_email_layout);
        nameLayout = findViewById(R.id.sign_up_name_layout);
        passwordLayout = findViewById(R.id.sign_up_password_layout);
        confirmLayout = findViewById(R.id.sign_up_confirm_layout);
        emailInput = findViewById(R.id.sign_up_email);
        nameInput = findViewById(R.id.sign_up_name);
        passwordInput = findViewById(R.id.sign_up_password);
        confirmInput = findViewById(R.id.sign_up_confirm);
        errorText = findViewById(R.id.sign_up_error);
        loginLink = findViewById(R.id.sign_up_go_login);
        submit = new LoadingButton(findViewById(R.id.sign_up_submit), R.string.sign_up_submit);

        Screens.padForSystemBarsAndKeyboard(findViewById(R.id.sign_up_root));
        AuthForms.clearErrorsOnEdit(errorText, emailLayout, nameLayout, passwordLayout, confirmLayout);

        if (chooseNameOnly) {
            showChooseNameMode();
        }

        submit.setOnClickListener(v -> attemptSubmit());
        TextInputEditText lastInput = chooseNameOnly ? nameInput : confirmInput;
        lastInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        lastInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            attemptSubmit();
            return true;
        });
        loginLink.setOnClickListener(v -> backToLogin());
    }

    private void showChooseNameMode() {
        ((TextView) findViewById(R.id.sign_up_title)).setText(R.string.choose_name_title);
        ((TextView) findViewById(R.id.sign_up_subtitle)).setText(R.string.choose_name_subtitle);
        emailLayout.setVisibility(View.GONE);
        passwordLayout.setVisibility(View.GONE);
        confirmLayout.setVisibility(View.GONE);
        loginLink.setVisibility(View.GONE);
        submit.setLabel(R.string.choose_name_submit);
    }

    private void attemptSubmit() {
        if (busy) return;
        String name = PlayerName.clean(AuthForms.text(nameInput));
        boolean nameOk = PlayerName.isValid(name);
        if (!nameOk) {
            nameLayout.setError(getString(name.isEmpty()
                    ? R.string.auth_error_required
                    : R.string.auth_error_name_format));
        }
        if (chooseNameOnly) {
            if (nameOk) submitName(name);
            return;
        }

        String email = AuthForms.text(emailInput);
        String password = AuthForms.password(passwordInput);
        String confirm = AuthForms.password(confirmInput);
        boolean emailOk = AuthForms.requireEmail(emailLayout, email);
        boolean passwordOk = password.length() >= AuthForms.MIN_PASSWORD_LENGTH;
        if (!passwordOk) {
            passwordLayout.setError(getString(R.string.auth_error_weak_password));
        }
        boolean confirmOk = AuthForms.require(confirmLayout, confirm);
        if (confirmOk && !confirm.equals(password)) {
            confirmLayout.setError(getString(R.string.auth_error_password_mismatch));
            confirmOk = false;
        }
        if (!emailOk || !nameOk || !passwordOk || !confirmOk) return;

        setBusy(true);
        auth.signUp(email, name, password, finishCallback());
    }

    private void submitName(String name) {
        setBusy(true);
        auth.chooseName(name, finishCallback());
    }

    private AuthRepository.Callback<Void> finishCallback() {
        return new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                if (isFinishing() || isDestroyed()) return;
                if (!chooseNameOnly || isTaskRoot()) {
                    startActivity(AuthForms.homeIntent(SignUpActivity.this));
                }
                finish();
            }

            @Override
            public void onError(int messageRes) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                if (messageRes == R.string.auth_error_name_taken) {
                    nameLayout.setError(getString(messageRes));
                } else if (messageRes == R.string.auth_error_email_in_use
                        || messageRes == R.string.auth_error_invalid_email) {
                    emailLayout.setError(getString(messageRes));
                } else {
                    AuthForms.showError(errorText, messageRes);
                }
            }
        };
    }

    private void backToLogin() {
        if (busy) return;
        startActivity(LoginActivity.newIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        submit.setLoading(busy);
        AuthForms.setEnabled(!busy, emailLayout, nameLayout, passwordLayout, confirmLayout, loginLink);
    }
}
