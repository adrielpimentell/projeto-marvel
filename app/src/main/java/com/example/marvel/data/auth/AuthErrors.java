package com.example.marvel.data.auth;

import com.example.marvel.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;

public final class AuthErrors {

    private static final String INVALID_EMAIL = "ERROR_INVALID_EMAIL";

    private AuthErrors() {
    }

    public static int messageFor(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof FirebaseNetworkException) {
                return R.string.auth_error_no_connection;
            }
            if (t instanceof FirebaseAuthUserCollisionException) {
                return R.string.auth_error_email_in_use;
            }
            if (t instanceof FirebaseAuthWeakPasswordException) {
                return R.string.auth_error_weak_password;
            }
            if (t instanceof FirebaseAuthInvalidUserException) {
                return R.string.auth_error_wrong_credentials;
            }
            if (t instanceof FirebaseAuthInvalidCredentialsException) {
                String code = ((FirebaseAuthInvalidCredentialsException) t).getErrorCode();
                return INVALID_EMAIL.equals(code)
                        ? R.string.auth_error_invalid_email
                        : R.string.auth_error_wrong_credentials;
            }
            if (t instanceof FirebaseTooManyRequestsException) {
                return R.string.auth_error_too_many;
            }
            if (t instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) t).getCode();
                if (code == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                    return R.string.auth_error_name_taken;
                }
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                        || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                    return R.string.auth_error_no_connection;
                }
            }
        }
        return R.string.auth_error_generic;
    }
}
