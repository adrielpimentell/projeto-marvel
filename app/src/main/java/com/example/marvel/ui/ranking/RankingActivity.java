package com.example.marvel.ui.ranking;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.auth.Session;
import com.example.marvel.data.season.RankingEntry;
import com.example.marvel.data.season.RankingPage;
import com.example.marvel.data.season.SeasonRepository;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Season;
import com.example.marvel.ui.common.MainNav;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.PullToRefreshLayout;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.ui.common.Skeleton;
import com.example.marvel.ui.common.StateView;
import com.example.marvel.ui.common.UiTokens;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RankingActivity extends AppCompatActivity {

    private static final long COUNTDOWN_TICK_MS = 30_000;
    private static final DateTimeFormatter DAY_MONTH =
            DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT).withZone(Season.ZONE);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdownTick = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            handler.postDelayed(this, COUNTDOWN_TICK_MS);
        }
    };

    private PlayerStore playerStore;
    private SeasonRepository seasons;
    private View playerHud;
    private TextView countdown;
    private TextView subtitle;
    private View offlineNote;
    private PullToRefreshLayout pull;
    private RecyclerView list;
    private View skeleton;
    private StateView stateView;
    private View myRow;
    private RankingAdapter adapter;
    private LinearLayoutManager layoutManager;
    private String loadedSeason;
    private String myUid;
    private boolean myRowShown = true;
    private int loadToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_ranking);

        playerStore = PlayerStore.getInstance(this);
        seasons = SeasonRepository.getInstance(this);
        playerHud = findViewById(R.id.player_hud);
        countdown = findViewById(R.id.ranking_countdown);
        subtitle = findViewById(R.id.ranking_subtitle);
        offlineNote = findViewById(R.id.ranking_offline);
        pull = findViewById(R.id.ranking_pull);
        list = findViewById(R.id.ranking_list);
        skeleton = findViewById(R.id.ranking_skeleton);
        stateView = new StateView(findViewById(R.id.ranking_state));
        myRow = findViewById(R.id.ranking_me);

        Screens.padForSystemBars(findViewById(R.id.ranking_root));
        adapter = new RankingAdapter();
        layoutManager = new LinearLayoutManager(this);
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);
        list.setItemAnimator(null);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateMyRowVisibility();
            }
        });
        list.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateMyRowVisibility());
        pull.setOnRefreshListener(this::load);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        MainNav.setup(this, bottomNav, R.id.nav_ranking, () -> list.smoothScrollToPosition(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerStore.ensureSeason(Season.currentId());
        PlayerHud.bind(playerHud, playerStore.get());
        seasons.sync();
        handler.post(countdownTick);
        load();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(countdownTick);
        super.onPause();
    }

    private void updateCountdown() {
        Instant now = Season.now();
        countdown.setText(SeasonCountdown.format(this, Duration.between(now, Season.endOf(now))));
        if (loadedSeason != null && !loadedSeason.equals(Season.idAt(now))) {
            playerStore.ensureSeason(Season.idAt(now));
            PlayerHud.bind(playerHud, playerStore.get());
            load();
        }
    }

    private void load() {
        String uid = Session.uid();
        if (uid == null) return;
        Instant now = Season.now();
        String season = Season.idAt(now);
        int token = ++loadToken;
        subtitle.setText(getString(R.string.ranking_subtitle, GameBalance.RANKING_SIZE,
                DAY_MONTH.format(Season.startOf(now)),
                DAY_MONTH.format(Season.endOf(now).minusSeconds(1))));
        if (!season.equals(loadedSeason)) {
            adapter.clear();
        }
        loadedSeason = season;
        myUid = uid;
        stateView.hide();
        if (!adapter.hasContent() && !pull.isRefreshing()) {
            Skeleton.show(skeleton);
        }
        RankingViews.bindPillMessage(myRow, myName(), R.string.ranking_me_loading);
        updateMyRowVisibility();

        seasons.loadRanking(season, new SeasonRepository.Callback<RankingPage>() {
            @Override
            public void onSuccess(RankingPage page) {
                if (token != loadToken || isDestroyed()) return;
                Skeleton.hide(skeleton);
                pull.setRefreshing(false);
                adapter.submit(page.getEntries(), uid);
                offlineNote.setVisibility(page.isFromCache() ? View.VISIBLE : View.GONE);
                list.post(RankingActivity.this::updateMyRowVisibility);
                loadMyPosition(season, uid, page, token);
            }

            @Override
            public void onError(int messageRes) {
                if (token != loadToken || isDestroyed()) return;
                Skeleton.hide(skeleton);
                pull.setRefreshing(false);
                if (!adapter.hasContent()) {
                    stateView.show(R.drawable.ic_error, null, getString(messageRes),
                            getString(R.string.action_retry), RankingActivity.this::load);
                }
                RankingViews.bindPillMessage(myRow, myName(), messageRes);
                updateMyRowVisibility();
            }
        });
    }

    private void loadMyPosition(String season, String uid, RankingPage page, int token) {
        seasons.loadMyPosition(season, uid, page, new SeasonRepository.Callback<RankingEntry>() {
            @Override
            public void onSuccess(RankingEntry me) {
                if (token != loadToken || isDestroyed()) return;
                if (me == null) {
                    RankingViews.bindPillMessage(myRow, myName(), R.string.ranking_me_not_played);
                } else {
                    RankingViews.bindPill(myRow, me, true);
                }
                updateMyRowVisibility();
            }

            @Override
            public void onError(int messageRes) {
                if (token != loadToken || isDestroyed()) return;
                RankingViews.bindPillMessage(myRow, myName(), messageRes);
                updateMyRowVisibility();
            }
        });
    }

    private void updateMyRowVisibility() {
        int position = myUid == null ? RecyclerView.NO_POSITION : adapter.adapterPositionOf(myUid);
        boolean show = position == RecyclerView.NO_POSITION || !isOnScreen(position);
        if (show == myRowShown) return;
        myRowShown = show;
        myRow.animate().cancel();
        if (show) {
            myRow.setVisibility(View.VISIBLE);
            myRow.animate().alpha(1f).setDuration(UiTokens.DURATION_SHORT_MS).start();
        } else {
            myRow.animate().alpha(0f).setDuration(UiTokens.DURATION_SHORT_MS)
                    .withEndAction(() -> {
                        if (!myRowShown) myRow.setVisibility(View.INVISIBLE);
                    })
                    .start();
        }
    }

    private boolean isOnScreen(int position) {
        View item = layoutManager.findViewByPosition(position);
        if (item == null || item.getHeight() == 0) return false;
        int myRowTopInList = myRow.getTop() - pull.getTop() - list.getTop();
        return item.getBottom() > 0 && item.getTop() < myRowTopInList;
    }

    private String myName() {
        return AuthRepository.getInstance(this).cachedPlayerName();
    }
}
