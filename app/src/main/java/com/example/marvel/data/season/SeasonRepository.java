package com.example.marvel.data.season;

import android.content.Context;

import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.auth.Session;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.TrophySync;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

public final class SeasonRepository {

    public static final String SEASONS = "temporadas";
    public static final String PLAYERS = "jogadores";
    public static final String FIELD_TROPHIES = "trofeus";
    public static final String FIELD_UPDATED_AT = "atualizadoEm";

    private static SeasonRepository instance;

    private final Context appContext;
    private final FirebaseFirestore db;
    private boolean refreshing;

    public static synchronized SeasonRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SeasonRepository(context.getApplicationContext());
        }
        return instance;
    }

    private SeasonRepository(Context appContext) {
        this.appContext = appContext;
        db = FirebaseFirestore.getInstance();
    }

    public DocumentReference playerDoc(String season, String uid) {
        return db.collection(SEASONS).document(season).collection(PLAYERS).document(uid);
    }

    public void sync() {
        String uid = Session.uid();
        if (uid == null) return;
        AuthRepository auth = AuthRepository.getInstance(appContext);
        String name = auth.cachedPlayerName();
        if (name == null) {
            auth.loadPlayerName(new AuthRepository.Callback<String>() {
                @Override
                public void onSuccess(String loaded) {
                    if (loaded != null && uid.equals(Session.uid())) sync();
                }

                @Override
                public void onError(int messageRes) {
                }
            });
            return;
        }
        TrophySync sync = PlayerStore.getInstance(appContext).claimTrophySync();
        if (sync.isEmpty()) return;

        DocumentReference doc = playerDoc(sync.getSeason(), uid);
        boolean create = sync.isCreate();
        for (int trophies : sync.getValues()) {
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_TROPHIES, trophies);
            data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
            if (create) {
                data.put(AuthRepository.FIELD_PLAYER_NAME, name);
                doc.set(data);
                create = false;
            } else {
                doc.update(data);
            }
        }
    }

    public void refresh(Runnable onChanged) {
        String uid = Session.uid();
        PlayerStore store = PlayerStore.getInstance(appContext);
        String season = store.get().getSeason();
        if (uid == null || season == null || refreshing) return;
        refreshing = true;
        sync();
        db.waitForPendingWrites().addOnCompleteListener(written ->
                playerDoc(season, uid).get(Source.SERVER).addOnCompleteListener(task -> {
                    refreshing = false;
                    if (!task.isSuccessful() || !uid.equals(Session.uid())) return;
                    Long trophies = task.getResult().exists()
                            ? task.getResult().getLong(FIELD_TROPHIES)
                            : null;
                    boolean changed = PlayerStore.getInstance(appContext).adoptServerTrophies(season,
                            trophies == null ? null : trophies.intValue());
                    if (changed) {
                        sync();
                        if (onChanged != null) onChanged.run();
                    }
                }));
    }
}
