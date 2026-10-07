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
import com.example.marvel.ui.common.LoadingButton;
import com.example.marvel.ui.common.Screens;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private AuthRepository auth;
    private View root;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextView errorText;
    private LoadingButton submit;
    private View forgotButton;
    private View signUpLink;
    private boolean busy;

    public static Intent newIntent(Context context) {
        return new Intent(context, LoginActivity.class);
    }

    public static Intent newSignedOutIntent(Context context) {
        return newIntent(context)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_login);

        auth = AuthRepository.getInstance(this);
        root = findViewById(R.id.login_root);
        emailLayout = findViewById(R.id.login_email_layout);
        passwordLayout = findViewById(R.id.login_password_layout);
        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        errorText = findViewById(R.id.login_error);
        forgotButton = findViewById(R.id.login_forgot);
        signUpLink = findViewById(R.id.login_go_sign_up);
        submit = new LoadingButton(findViewById(R.id.login_submit), R.string.login_submit);

        Screens.padForSystemBarsAndKeyboard(root);
        AuthForms.clearErrorsOnEdit(errorText, emailLayout, passwordLayout);

        submit.setOnClickListener(v -> attemptLogin());
        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            attemptLogin();
            return true;
        });
        forgotButton.setOnClickListener(v -> sendPasswordReset());
        signUpLink.setOnClickListener(v -> startActivity(SignUpActivity.newIntent(this)));
    }

    private void attemptLogin() {
        if (busy) return;
        String email = AuthForms.text(emailInput);
        String password = AuthForms.password(passwordInput);
        boolean emailOk = AuthForms.requireEmail(emailLayout, email);
        boolean passwordOk = AuthForms.require(passwordLayout, password);
        if (!emailOk || !passwordOk) return;

        setBusy(true);
        auth.signIn(email, password, new AuthRepository.Callback<AuthRepository.LoginResult>() {
            @Override
            public void onSuccess(AuthRepository.LoginResult result) {
                if (isFinishing() || isDestroyed()) return;
                if (result == AuthRepository.LoginResult.NEEDS_NAME) {
                    startActivity(SignUpActivity.newChooseNameIntent(LoginActivity.this));
                } else {
                    startActivity(AuthForms.homeIntent(LoginActivity.this));
                }
                finish();
            }

            @Override
            public void onError(int messageRes) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                AuthForms.showError(errorText, messageRes);
            }
        });
    }

    private void sendPasswordReset() {
        if (busy) return;
        String email = AuthForms.text(emailInput);
        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.auth_reset_need_email));
            emailInput.requestFocus();
            return;
        }
        if (!AuthForms.requireEmail(emailLayout, email)) return;

        setBusy(true, false);
        auth.sendPasswordReset(email, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                Snackbar.make(root, R.string.auth_reset_sent, Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError(int messageRes) {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                AuthForms.showError(errorText, messageRes);
            }
        });
    }

    private void setBusy(boolean busy) {
        setBusy(busy, true);
    }

    private void setBusy(boolean busy, boolean onSubmit) {
        this.busy = busy;
        submit.setLoading(busy && onSubmit);
        AuthForms.setEnabled(!busy, emailLayout, passwordLayout, forgotButton, signUpLink);
    }
}
