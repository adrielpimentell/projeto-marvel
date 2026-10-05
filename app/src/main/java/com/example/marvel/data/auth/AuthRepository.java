package com.example.marvel.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.marvel.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.Transaction;

import java.util.Collections;

public final class AuthRepository {

    public interface Callback<T> {

        void onSuccess(T value);

        void onError(int messageRes);
    }

    public enum LoginResult { READY, NEEDS_NAME }

    public static final String USERS = "users";
    public static final String USERNAMES = "usernames";
    public static final String FIELD_PLAYER_NAME = "nomeJogador";
    public static final String FIELD_UID = "uid";

    private static final String PREFS_NAME = "conta";
    private static final String KEY_NAME_PREFIX = "nome_";

    private static AuthRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final SharedPreferences prefs;

    public static synchronized AuthRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AuthRepository(context.getApplicationContext());
        }
        return instance;
    }

    private AuthRepository(Context appContext) {
        auth = FirebaseAuth.getInstance();
        auth.useAppLanguage();
        db = FirebaseFirestore.getInstance();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void signIn(String email, String password, Callback<LoginResult> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> fetchProfile(result.getUser(), callback))
                .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
    }

    public void signUp(String email, String playerName, String password, Callback<Void> callback) {
        db.collection(USERNAMES).document(PlayerName.key(playerName)).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onError(R.string.auth_error_name_taken);
                        return;
                    }
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(result ->
                                    claimName(result.getUser(), playerName, true, callback))
                            .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
                })
                .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
    }

    public void chooseName(String playerName, Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        claimName(user, playerName, false, callback);
    }

    public void sendPasswordReset(String email, Callback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(ignored -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
    }

    public void signOut() {
        auth.signOut();
    }

    public String cachedPlayerName() {
        String uid = Session.uid();
        return uid == null ? null : prefs.getString(KEY_NAME_PREFIX + uid, null);
    }

    public void loadPlayerName(Callback<String> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        String cached = cachedPlayerName();
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }
        String uid = user.getUid();
        db.collection(USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString(FIELD_PLAYER_NAME);
                    if (name != null) {
                        saveName(uid, name);
                    }
                    callback.onSuccess(name);
                })
                .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
    }

    private void fetchProfile(FirebaseUser user, Callback<LoginResult> callback) {
        String uid = user.getUid();
        db.collection(USERS).document(uid).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    String name = doc.getString(FIELD_PLAYER_NAME);
                    if (name == null) {
                        callback.onSuccess(LoginResult.NEEDS_NAME);
                        return;
                    }
                    saveName(uid, name);
                    callback.onSuccess(LoginResult.READY);
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    callback.onError(AuthErrors.messageFor(e));
                });
    }

    private void claimName(FirebaseUser user, String playerName, boolean undoAccountOnFailure,
                           Callback<Void> callback) {
        String uid = user.getUid();
        DocumentReference nameRef = db.collection(USERNAMES).document(PlayerName.key(playerName));
        DocumentReference userRef = db.collection(USERS).document(uid);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            if (transaction.get(nameRef).exists()) {
                throw new FirebaseFirestoreException(PlayerName.key(playerName),
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }
            transaction.set(nameRef, Collections.singletonMap(FIELD_UID, uid));
            transaction.set(userRef, Collections.singletonMap(FIELD_PLAYER_NAME, playerName));
            return null;
        }).addOnSuccessListener(ignored -> {
            saveName(uid, playerName);
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            int message = AuthErrors.messageFor(e);
            if (!undoAccountOnFailure) {
                callback.onError(message);
                return;
            }
            user.delete().addOnCompleteListener(done -> {
                auth.signOut();
                callback.onError(message);
            });
        });
    }

    private void saveName(String uid, String name) {
        prefs.edit().putString(KEY_NAME_PREFIX + uid, name).apply();
    }
}
