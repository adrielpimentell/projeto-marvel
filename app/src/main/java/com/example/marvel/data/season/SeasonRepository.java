package com.example.marvel.data.season;

import android.content.Context;
import android.util.Log;

import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.auth.Session;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Season;
import com.example.marvel.game.SeasonResult;
import com.example.marvel.game.TrophySync;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SeasonRepository {

    public interface Callback<T> {

        void onSuccess(T value);

        void onError(int messageRes);
    }

    private static final String TAG = "SeasonRepository";

    public static final String SEASONS = "temporadas";
    public static final String PLAYERS = "jogadores";
    public static final String FIELD_TROPHIES = "trofeus";
    public static final String FIELD_UPDATED_AT = "atualizadoEm";
    public static final String FIELD_SEASON = "temporada";
    public static final String FIELD_PRIZES = "premiosRecebidos";

    private static SeasonRepository instance;

    private final Context appContext;
    private final FirebaseFirestore db;
    private boolean refreshing;
    private boolean checkingSeasonEnd;
    private boolean checkSeasonEndAgain;
    private final List<Callback<Boolean>> seasonEndWaiting = new ArrayList<>();

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
        return players(season).document(uid);
    }

    private CollectionReference players(String season) {
        return db.collection(SEASONS).document(season).collection(PLAYERS);
    }

    public void loadRanking(String season, Callback<RankingPage> callback) {
        players(season)
                .orderBy(FIELD_TROPHIES, Query.Direction.DESCENDING)
                .orderBy(FIELD_UPDATED_AT, Query.Direction.ASCENDING)
                .limit(GameBalance.RANKING_SIZE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<RankingEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long trophies = doc.getLong(FIELD_TROPHIES);
                        entries.add(new RankingEntry(doc.getId(), playerName(doc),
                                trophies == null ? 0 : trophies.intValue(), entries.size() + 1));
                    }
                    callback.onSuccess(new RankingPage(entries, snapshot.getMetadata().isFromCache()));
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "ranking", e);
                    callback.onError(messageFor(e));
                });
    }

    public void loadMyPosition(String season, String uid, RankingPage page,
                               Callback<RankingEntry> callback) {
        RankingEntry inTop = page.find(uid);
        if (inTop != null) {
            callback.onSuccess(inTop);
            return;
        }
        playerDoc(season, uid).get()
                .addOnSuccessListener(doc -> {
                    Long trophies = doc.getLong(FIELD_TROPHIES);
                    Timestamp updatedAt = doc.getTimestamp(FIELD_UPDATED_AT,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                    if (!doc.exists() || trophies == null || updatedAt == null) {
                        callback.onSuccess(null);
                        return;
                    }
                    Query all = players(season);
                    Task<AggregateQuerySnapshot> above = all.whereGreaterThan(FIELD_TROPHIES, trophies)
                            .count().get(AggregateSource.SERVER);
                    Task<AggregateQuerySnapshot> tiedBefore = all.whereEqualTo(FIELD_TROPHIES, trophies)
                            .whereLessThan(FIELD_UPDATED_AT, updatedAt)
                            .count().get(AggregateSource.SERVER);
                    Tasks.whenAllSuccess(above, tiedBefore)
                            .addOnSuccessListener(results -> {
                                long ahead = ((AggregateQuerySnapshot) results.get(0)).getCount()
                                        + ((AggregateQuerySnapshot) results.get(1)).getCount();
                                callback.onSuccess(new RankingEntry(uid, playerName(doc),
                                        trophies.intValue(), (int) ahead + 1));
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "position", e);
                                callback.onError(messageFor(e));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "my doc", e);
                    callback.onError(messageFor(e));
                });
    }

    public void checkSeasonEnd(Callback<Boolean> callback) {
        String uid = Session.uid();
        if (uid == null) return;
        if (checkingSeasonEnd) {
            checkSeasonEndAgain = true;
            if (callback != null) seasonEndWaiting.add(callback);
            return;
        }
        checkingSeasonEnd = true;
        String current = Season.currentId();
        DocumentReference userRef = db.collection(AuthRepository.USERS).document(uid);
        userRef.get(Source.SERVER)
                .addOnSuccessListener(userDoc -> {
                    String saved = userDoc.getString(FIELD_SEASON);
                    if (current.equals(saved)) {
                        finishSeasonCheck(callback, false);
                        return;
                    }
                    String ended = saved != null ? saved : Season.previousId(Season.now());
                    if (!Season.isBefore(ended, current) || prizesOf(userDoc).contains(ended)) {
                        moveToSeason(userRef, current, callback);
                        return;
                    }
                    claimPrize(uid, userRef, ended, current, callback);
                })
                .addOnFailureListener(e -> failSeasonCheck(callback, e));
    }

    private void claimPrize(String uid, DocumentReference userRef, String ended, String current,
                            Callback<Boolean> callback) {
        playerDoc(ended, uid).get(Source.SERVER)
                .addOnSuccessListener(myDoc -> {
                    Long trophies = myDoc.getLong(FIELD_TROPHIES);
                    Timestamp updatedAt = myDoc.getTimestamp(FIELD_UPDATED_AT);
                    if (!myDoc.exists() || trophies == null || updatedAt == null) {
                        moveToSeason(userRef, current, callback);
                        return;
                    }
                    Query all = players(ended);
                    Task<AggregateQuerySnapshot> above = all.whereGreaterThan(FIELD_TROPHIES, trophies)
                            .count().get(AggregateSource.SERVER);
                    Task<AggregateQuerySnapshot> tiedBefore = all.whereEqualTo(FIELD_TROPHIES, trophies)
                            .whereLessThan(FIELD_UPDATED_AT, updatedAt)
                            .count().get(AggregateSource.SERVER);
                    Tasks.whenAllSuccess(above, tiedBefore)
                            .addOnSuccessListener(results -> {
                                long ahead = ((AggregateQuerySnapshot) results.get(0)).getCount()
                                        + ((AggregateQuerySnapshot) results.get(1)).getCount();
                                SeasonResult result = SeasonResult.of(ended, (int) ahead + 1,
                                        trophies.intValue());
                                markPrizeAndGrant(userRef, current, result, callback);
                            })
                            .addOnFailureListener(e -> failSeasonCheck(callback, e));
                })
                .addOnFailureListener(e -> failSeasonCheck(callback, e));
    }

    private void markPrizeAndGrant(DocumentReference userRef, String current, SeasonResult result,
                                   Callback<Boolean> callback) {
        String uid = userRef.getId();
        db.runTransaction((Transaction.Function<Boolean>) transaction -> {
            DocumentSnapshot userDoc = transaction.get(userRef);
            Map<String, Object> update = new HashMap<>();
            update.put(FIELD_SEASON, current);
            boolean firstClaim = !prizesOf(userDoc).contains(result.getSeason());
            if (firstClaim) {
                update.put(FIELD_PRIZES, FieldValue.arrayUnion(result.getSeason()));
            }
            transaction.update(userRef, update);
            return firstClaim;
        }).addOnSuccessListener(firstClaim -> {
            boolean granted = firstClaim && uid.equals(Session.uid())
                    && PlayerStore.getInstance(appContext).grantSeasonPrize(result);
            finishSeasonCheck(callback, granted);
        }).addOnFailureListener(e -> failSeasonCheck(callback, e));
    }

    private void moveToSeason(DocumentReference userRef, String current, Callback<Boolean> callback) {
        userRef.update(FIELD_SEASON, current)
                .addOnSuccessListener(ignored -> finishSeasonCheck(callback, false))
                .addOnFailureListener(e -> failSeasonCheck(callback, e));
    }

    private void finishSeasonCheck(Callback<Boolean> callback, boolean granted) {
        checkingSeasonEnd = false;
        if (callback != null) callback.onSuccess(granted);
        runWaitingSeasonCheck();
    }

    private void failSeasonCheck(Callback<Boolean> callback, Exception e) {
        checkingSeasonEnd = false;
        Log.w(TAG, "season end", e);
        if (callback != null) callback.onError(messageFor(e));
        runWaitingSeasonCheck();
    }

    private void runWaitingSeasonCheck() {
        if (!checkSeasonEndAgain) return;
        checkSeasonEndAgain = false;
        List<Callback<Boolean>> waiting = new ArrayList<>(seasonEndWaiting);
        seasonEndWaiting.clear();
        checkSeasonEnd(new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean granted) {
                for (Callback<Boolean> each : waiting) each.onSuccess(granted);
            }

            @Override
            public void onError(int messageRes) {
                for (Callback<Boolean> each : waiting) each.onError(messageRes);
            }
        });
    }

    private static List<String> prizesOf(DocumentSnapshot userDoc) {
        List<String> prizes = new ArrayList<>();
        Object value = userDoc.get(FIELD_PRIZES);
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof String) prizes.add((String) item);
            }
        }
        return prizes;
    }

    private static String playerName(DocumentSnapshot doc) {
        String name = doc.getString(AuthRepository.FIELD_PLAYER_NAME);
        return name == null ? "" : name;
    }

    private static int messageFor(Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return R.string.auth_error_no_connection;
            }
        }
        return R.string.ranking_error;
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
