package com.example.marvel;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.auth.SignUpActivity;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.ui.season.SeasonEndActivity;
import com.example.marvel.game.Season;
import com.example.marvel.data.season.SeasonRepository;
import com.example.marvel.data.auth.Session;
import com.example.marvel.ui.auth.LoginActivity;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.CharacterPage;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.data.repository.SortOption;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.common.MainNav;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.SearchBar;
import com.example.marvel.ui.common.StateView;
import com.example.marvel.ui.daily.DailyChallengesAdapter;
import com.example.marvel.ui.daily.DailyHeader;
import com.example.marvel.ui.detail.CharacterDetailActivity;
import com.example.marvel.ui.list.CharacterAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayDeque;

public class MainActivity extends AppCompatActivity
        implements CharacterAdapter.Listener {

    private static final int GRID_COLUMNS = 2;
    private static final long SEARCH_DELAY_MS = 500;
    private static final int LOAD_MORE_THRESHOLD = 6;
    private static final int MAX_EMPTY_BATCHES = 3;

    private CharacterRepository repository;
    private PlayerStore playerStore;
    private CharacterAdapter adapter;
    private DailyHeader dailyHeader;
    private DailyChallengesAdapter dailyAdapter;
    private ConcatAdapter listAdapter;

    private View root;
    private RecyclerView list;
    private GridLayoutManager layoutManager;
    private SearchBar searchBar;
    private TextView sectionTitle;
    private View playerHud;
    private TextView subtitleText;
    private StateView stateView;
    private BottomNavigationView bottomNav;
    private boolean askedForName;


    private SortOption sort = SortOption.CLASSIC;
    private String query = "";
    private int nextOffset = 0;
    private boolean hasMore = true;
    private boolean loading = false;
    private boolean autoLoadPaused = false;
    private int emptyBatchesInARow = 0;
    private RequestHandle currentRequest;
    private RequestHandle heroInfoRequest;
    private final ArrayDeque<Integer> heroesMissingDetails = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isSignedIn()) {
            startActivity(LoginActivity.newSignedOutIntent(this));
            finish();
            return;
        }
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_main);

        repository = CharacterRepository.getInstance(this);
        playerStore = PlayerStore.getInstance(this);

        bindViews();
        applyWindowInsets();
        loadHeroDetailsIfNeeded();
        setupList();
        setupSearch();
        setupFilters();
        setupBottomNav();
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerStore.ensureSeason(Season.currentId());
        updatePlayerHud();
        dailyHeader.onResume();
        SeasonRepository seasons = SeasonRepository.getInstance(this);
        seasons.refresh(() -> {
            if (!isFinishing() && !isDestroyed()) updatePlayerHud();
        });
        seasons.checkSeasonEnd(new SeasonRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean granted) {
                if (isFinishing() || isDestroyed()) return;
                updatePlayerHud();
                if (granted) SeasonEndActivity.showIfPending(MainActivity.this);
            }

            @Override
            public void onError(int messageRes) {
            }
        });
        SeasonEndActivity.showIfPending(this);
        askForPlayerNameIfMissing();
    }

    private void askForPlayerNameIfMissing() {
        if (askedForName) return;
        AuthRepository.getInstance(this).loadPlayerName(new AuthRepository.Callback<String>() {
            @Override
            public void onSuccess(String name) {
                if (name != null || askedForName || isFinishing() || isDestroyed()) return;
                askedForName = true;
                startActivity(SignUpActivity.newChooseNameIntent(MainActivity.this));
            }

            @Override
            public void onError(int messageRes) {
            }
        });
    }

    @Override
    protected void onPause() {
        dailyHeader.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (searchBar != null) searchBar.release();
        if (currentRequest != null) currentRequest.cancel();
        if (heroInfoRequest != null) heroInfoRequest.cancel();
        super.onDestroy();
    }

    private void bindViews() {
        root = findViewById(R.id.main);
        list = findViewById(R.id.character_list);
        sectionTitle = findViewById(R.id.section_title);
        playerHud = findViewById(R.id.player_hud);
        subtitleText = findViewById(R.id.list_subtitle_text);
        stateView = new StateView(findViewById(R.id.state_view));
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupList() {
        adapter = new CharacterAdapter(this);
        dailyHeader = new DailyHeader(this, playerStore);
        dailyAdapter = dailyHeader.getAdapter();
        listAdapter = new ConcatAdapter(dailyAdapter, adapter);
        layoutManager = new GridLayoutManager(this, GRID_COLUMNS);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int header = dailyAdapter.getItemCount();
                if (position < header) return GRID_COLUMNS;
                return adapter.isFooter(position - header) ? GRID_COLUMNS : 1;
            }
        });
        list.setLayoutManager(layoutManager);
        list.setAdapter(listAdapter);
        sectionTitle.setVisibility(View.GONE);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) hideKeyboard();
            }

            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) maybeLoadMore();
            }
        });
    }

    private void setupSearch() {
        searchBar = new SearchBar(findViewById(R.id.search_bar), R.string.search_hint,
                SEARCH_DELAY_MS, this::applySearch);
    }

    private void setupFilters() {
        ChipGroup chips = findViewById(R.id.sort_chips);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checked = checkedIds.get(0);
            SortOption newSort;
            if (checked == R.id.chip_updated) {
                newSort = SortOption.UPDATED;
            } else if (checked == R.id.chip_name) {
                newSort = SortOption.NAME;
            } else {
                newSort = SortOption.CLASSIC;
            }
            if (newSort != sort) {
                sort = newSort;
                reload();
            }
        });
    }

    private void setupBottomNav() {
        MainNav.setup(this, bottomNav, R.id.nav_home, this::scrollToTop);
    }

    private void reload() {
        if (currentRequest != null) currentRequest.cancel();
        loading = false;
        nextOffset = 0;
        hasMore = true;
        autoLoadPaused = false;
        emptyBatchesInARow = 0;
        adapter.clear();
        list.scrollToPosition(0);
        loadNextBatch();
    }

    private void loadNextBatch() {
        if (loading || !hasMore) return;
        loading = true;
        autoLoadPaused = false;

        if (adapter.getCharacterCount() == 0) {
            showLoading();
        } else {
            adapter.showFooterLoading();
        }

        currentRequest = repository.loadCharacters(query, sort, nextOffset,
                new RepoCallback<CharacterPage>() {
                    @Override
                    public void onSuccess(CharacterPage page) {
                        loading = false;
                        nextOffset = page.getNextOffset();
                        hasMore = page.hasMore();
                        adapter.hideFooter();
                        int added = adapter.addCharacters(page.getCharacters());
                        emptyBatchesInARow = added > 0 ? 0 : emptyBatchesInARow + 1;
                        onBatchLoaded();
                    }

                    @Override
                    public void onError(String message) {
                        loading = false;
                        autoLoadPaused = true;
                        if (adapter.getCharacterCount() == 0) {
                            showError(message);
                        } else {
                            adapter.showFooterMessage(message, getString(R.string.action_retry));
                        }
                    }
                });
    }

    private void onBatchLoaded() {
        boolean tooManyEmptyBatches = emptyBatchesInARow >= MAX_EMPTY_BATCHES;

        if (adapter.getCharacterCount() > 0) {
            showContent();
            if (hasMore && tooManyEmptyBatches) {
                autoLoadPaused = true;
                adapter.showFooterMessage(getString(R.string.state_no_more_marvel),
                        getString(R.string.action_search_more));
            } else {
                list.post(this::maybeLoadMore);
            }
            return;
        }

        if (!hasMore) {
            showEmpty(query.isEmpty()
                    ? getString(R.string.state_empty_partial)
                    : getString(R.string.state_empty_search, query), false);
        } else if (tooManyEmptyBatches) {
            showEmpty(getString(R.string.state_empty_partial), true);
        } else {
            loadNextBatch();
        }
    }

    private void maybeLoadMore() {
        if (loading || !hasMore || autoLoadPaused || adapter.getCharacterCount() == 0) return;
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible >= listAdapter.getItemCount() - LOAD_MORE_THRESHOLD) {
            loadNextBatch();
        }
    }

    private void applySearch(String text) {
        String newQuery = text.trim();
        if (newQuery.equals(query)) return;
        query = newQuery;
        sectionTitle.setText(query.isEmpty()
                ? getString(R.string.section_all)
                : getString(R.string.section_search, query));
        sectionTitle.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
        dailyAdapter.setVisible(query.isEmpty());
        reload();
    }

    private void showLoading() {
        stateView.hide();
        dailyAdapter.setVisible(query.isEmpty());
        adapter.showFooterSkeleton();
    }

    private void showContent() {
        stateView.hide();
        dailyAdapter.setVisible(query.isEmpty());
    }

    private void showError(String message) {
        showState(R.drawable.ic_error, getString(R.string.state_error_title), message,
                getString(R.string.action_retry), this::loadNextBatch);
    }

    private void showEmpty(String message, boolean canSearchMore) {
        showState(R.drawable.ic_search, getString(R.string.state_empty_title), message,
                canSearchMore ? getString(R.string.action_search_more) : null,
                canSearchMore ? this::searchMore : null);
    }

    private void showState(int iconRes, String title, String message,
                           String buttonText, Runnable action) {
        adapter.hideFooter();
        dailyAdapter.setVisible(false);
        stateView.show(iconRes, title, message, buttonText, action);
    }

    private void searchMore() {
        emptyBatchesInARow = 0;
        loadNextBatch();
    }

    @Override
    public void onCharacterClick(Character character) {
        startActivity(CharacterDetailActivity.newIntent(this, character));
    }

    @Override
    public void onFooterActionClick() {
        searchMore();
    }

    private void updatePlayerHud() {
        PlayerHud.bind(playerHud, playerStore.get());
        OwnedHero hero = playerStore.get().getActiveHero();
        String name = hero.hasInfo() ? hero.getName() : getString(R.string.starter_hero_fallback);
        subtitleText.setText(getString(R.string.active_hero_subtitle,
                name, hero.getAttributes().getOverall()));
    }

    private void loadHeroDetailsIfNeeded() {
        for (OwnedHero hero : playerStore.get().getHeroes()) {
            if (!hero.hasInfo() || !hero.hasTags()) heroesMissingDetails.add(hero.getCharacterId());
        }
        loadNextHeroDetails();
    }

    private void loadNextHeroDetails() {
        Integer id = heroesMissingDetails.poll();
        if (id == null) return;
        heroInfoRequest = repository.loadCharacterDetail(id, new RepoCallback<Character>() {
            @Override
            public void onSuccess(Character c) {
                playerStore.updateHeroDetails(c);
                updatePlayerHud();
                loadNextHeroDetails();
            }

            @Override
            public void onError(String message) {
                heroesMissingDetails.clear();
            }
        });
    }

    private void scrollToTop() {
        if (layoutManager.findFirstVisibleItemPosition() > 20) {
            list.scrollToPosition(0);
        } else {
            list.smoothScrollToPosition(0);
        }
    }

    private void hideKeyboard() {
        searchBar.hideKeyboard();
    }
}
