package com.example.marvel.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.marvel.R;
import com.example.marvel.data.season.SeasonRepository;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Season;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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
    public static final String FIELD_AVATAR = "avatarHeroId";

    private static final String PREFS_NAME = "conta";
    private static final String KEY_NAME_PREFIX = "nome_";
    private static final String KEY_AVATAR_PREFIX = "avatar_";

    private static AuthRepository instance;

    private final Context appContext;
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
        this.appContext = appContext;
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
                    saveAvatar(uid, avatarOf(doc));
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

    private void saveAvatar(String uid, int heroId) {
        prefs.edit().putInt(KEY_AVATAR_PREFIX + uid, heroId).apply();
    }

    private static int avatarOf(DocumentSnapshot doc) {
        Long heroId = doc.getLong(FIELD_AVATAR);
        return heroId == null ? 0 : heroId.intValue();
    }

    public int cachedAvatarHeroId() {
        String uid = Session.uid();
        return uid == null ? 0 : prefs.getInt(KEY_AVATAR_PREFIX + uid, 0);
    }

    public void refreshProfile(Runnable onChanged) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        db.collection(USERS).document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists() || !uid.equals(Session.uid())) return;
            String before = cachedPlayerName() + "/" + cachedAvatarHeroId();
            String name = doc.getString(FIELD_PLAYER_NAME);
            if (name != null) saveName(uid, name);
            saveAvatar(uid, avatarOf(doc));
            String after = name + "/" + avatarOf(doc);
            if (!before.equals(after) && onChanged != null) onChanged.run();
        });
    }

    public void setAvatar(int heroId) {
        String uid = Session.uid();
        if (uid == null) return;
        saveAvatar(uid, heroId);
        db.collection(USERS).document(uid)
                .update(FIELD_AVATAR, heroId > 0 ? heroId : FieldValue.delete());
    }

    public void renamePlayer(String newName, Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        String uid = user.getUid();
        String newKey = PlayerName.key(newName);
        DocumentReference userRef = db.collection(USERS).document(uid);
        DocumentReference newNameRef = db.collection(USERNAMES).document(newKey);
        DocumentReference seasonRef = SeasonRepository.getInstance(appContext).playerDoc(Season.currentId(), uid);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot me = transaction.get(userRef);
            String oldName = me.getString(FIELD_PLAYER_NAME);
            if (oldName == null) {
                throw new FirebaseFirestoreException(uid, FirebaseFirestoreException.Code.NOT_FOUND);
            }
            String oldKey = PlayerName.key(oldName);
            boolean keyChanges = !oldKey.equals(newKey);
            DocumentReference oldNameRef = db.collection(USERNAMES).document(oldKey);
            DocumentSnapshot season = transaction.get(seasonRef);
            DocumentSnapshot oldReservation = keyChanges ? transaction.get(oldNameRef) : null;
            if (keyChanges && transaction.get(newNameRef).exists()) {
                throw new FirebaseFirestoreException(newKey, FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }
            if (keyChanges) {
                if (oldReservation.exists() && uid.equals(oldReservation.getString(FIELD_UID))) {
                    transaction.delete(oldNameRef);
                }
                transaction.set(newNameRef, Collections.singletonMap(FIELD_UID, uid));
            }
            transaction.update(userRef, FIELD_PLAYER_NAME, newName);
            if (season.exists()) {
                transaction.update(seasonRef, FIELD_PLAYER_NAME, newName);
            }
            return null;
        }).addOnSuccessListener(ignored -> {
            saveName(uid, newName);
            callback.onSuccess(null);
        }).addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
    }

    public void changePassword(String currentPassword, String newPassword, Callback<Void> callback) {
        reauthenticate(currentPassword, new Callback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser user) {
                user.updatePassword(newPassword)
                        .addOnSuccessListener(ignored -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
            }

            @Override
            public void onError(int messageRes) {
                callback.onError(messageRes);
            }
        });
    }

    public void deleteAccount(String password, Callback<Void> callback) {
        reauthenticate(password, new Callback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser user) {
                deleteCloudData(user, callback);
            }

            @Override
            public void onError(int messageRes) {
                callback.onError(messageRes);
            }
        });
    }

    private void deleteCloudData(FirebaseUser user, Callback<Void> callback) {
        String uid = user.getUid();
        DocumentReference userRef = db.collection(USERS).document(uid);
        SeasonRepository seasons = SeasonRepository.getInstance(appContext);
        DocumentReference seasonRef = seasons.playerDoc(Season.currentId(), uid);
        DocumentReference previousSeasonRef = seasons.playerDoc(Season.previousId(Season.now()), uid);
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot me = transaction.get(userRef);
            String name = me.getString(FIELD_PLAYER_NAME);
            DocumentReference nameRef = name == null ? null
                    : db.collection(USERNAMES).document(PlayerName.key(name));
            DocumentSnapshot reservation = nameRef == null ? null : transaction.get(nameRef);
            DocumentSnapshot season = transaction.get(seasonRef);
            DocumentSnapshot previousSeason = transaction.get(previousSeasonRef);
            if (reservation != null && reservation.exists() && uid.equals(reservation.getString(FIELD_UID))) {
                transaction.delete(nameRef);
            }
            if (season.exists()) transaction.delete(seasonRef);
            if (previousSeason.exists()) transaction.delete(previousSeasonRef);
            if (me.exists()) transaction.delete(userRef);
            return null;
        }).addOnSuccessListener(ignored -> user.delete()
                .addOnSuccessListener(done -> {
                    forgetLocalData(uid);
                    auth.signOut();
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e))))
                .addOnFailureListener(e -> callback.onError(AuthErrors.messageFor(e)));
    }

    private void forgetLocalData(String uid) {
        prefs.edit().remove(KEY_NAME_PREFIX + uid).remove(KEY_AVATAR_PREFIX + uid).apply();
        PlayerStore.deleteLocalData(appContext, uid);
    }

    private void reauthenticate(String password, Callback<FirebaseUser> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnSuccessListener(ignored -> callback.onSuccess(user))
                .addOnFailureListener(e -> {
                    int message = AuthErrors.messageFor(e);
                    callback.onError(message == R.string.auth_error_wrong_credentials
                            ? R.string.account_wrong_password
                            : message);
                });
    }
}
