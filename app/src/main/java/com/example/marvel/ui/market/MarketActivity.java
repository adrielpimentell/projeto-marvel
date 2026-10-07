package com.example.marvel.ui.market;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.CharacterPage;
import com.example.marvel.data.model.NamedRef;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.data.repository.SortOption;
import com.example.marvel.game.AttributeCalculator;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.common.Families;
import com.example.marvel.ui.common.FamilyFilter;
import com.example.marvel.ui.common.MainNav;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.SearchBar;
import com.example.marvel.ui.common.SearchText;
import com.example.marvel.ui.common.StateView;
import com.example.marvel.ui.detail.CharacterDetailActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketActivity extends AppCompatActivity
        implements MarketAdapter.Listener, RoulettePanel.Host {

    private static final SortOption MARKET_SORT = SortOption.CLASSIC;
    private static final int MAX_PAGES_PER_BATCH = 3;
    private CharacterRepository repository;
    private PlayerStore playerStore;
    private MarketAdapter adapter;
    private SearchBar searchBar;
    private FamilyFilter familyFilter;
    private String query = "";

    private View root;
    private View playerHud;
    private StateView stateView;
    private BottomNavigationView bottomNav;
    private View heroesPanel;
    private View artifactsPanel;

    private RoulettePanel roulettes;

    private final ArrayDeque<Character> candidates = new ArrayDeque<>();
    private final Set<Integer> shownIds = new HashSet<>();
    private int nextOffset = 0;
    private boolean hasMorePages = true;
    private boolean loadingBatch = false;
    private RequestHandle pageRequest;

    private final ArrayDeque<MarketItem> pendingDetails = new ArrayDeque<>();
    private boolean loadingDetail = false;
    private RequestHandle detailRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_market);

        repository = CharacterRepository.getInstance(this);
        playerStore = PlayerStore.getInstance(this);

        root = findViewById(R.id.market_root);
        playerHud = findViewById(R.id.player_hud);
        stateView = new StateView(findViewById(R.id.market_state));
        bottomNav = findViewById(R.id.bottom_nav);
        heroesPanel = findViewById(R.id.market_heroes_panel);
        artifactsPanel = findViewById(R.id.market_artifacts_panel);

        Screens.padForSystemBars(root);

        RecyclerView list = findViewById(R.id.market_list);
        adapter = new MarketAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        searchBar = new SearchBar(findViewById(R.id.market_search), R.string.search_hint_hero,
                SearchBar.DEFAULT_DELAY_MS, this::applySearch);
        familyFilter = new FamilyFilter(findViewById(R.id.market_family), familyId -> {
            adapter.setFamilyFilter(familyId,
                    getString(R.string.family_no_results, familyFilter.getSelectedName()));
            list.scrollToPosition(0);
        });
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) searchBar.hideKeyboard();
            }
        });

        MainNav.setup(this, bottomNav, R.id.nav_market, () -> list.smoothScrollToPosition(0));

        roulettes = new RoulettePanel(this, findViewById(R.id.roulette_list), playerStore, this);
        ChipGroup tabs = findViewById(R.id.market_tabs);
        tabs.setOnCheckedStateChangeListener((group, checked) ->
                showTab(checked.contains(R.id.tab_artifacts)));
        showMore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        roulettes.onResume();
        refreshCoins();
    }

    @Override
    protected void onDestroy() {
        searchBar.release();
        if (pageRequest != null) pageRequest.cancel();
        if (detailRequest != null) detailRequest.cancel();
        super.onDestroy();
    }

    private void applySearch(String text) {
        if (text.equals(query)) return;
        query = text;
        if (pageRequest != null) pageRequest.cancel();
        if (detailRequest != null) detailRequest.cancel();
        loadingBatch = false;
        loadingDetail = false;
        candidates.clear();
        shownIds.clear();
        pendingDetails.clear();
        nextOffset = 0;
        hasMorePages = true;
        adapter.clear();
        updateFamilies();
        showMore();
    }

    private void updateFamilies() {
        List<List<NamedRef>> teams = new ArrayList<>();
        for (MarketItem item : adapter.getAll()) {
            if (item.isReady()) teams.add(item.character.getTeams());
        }
        familyFilter.setFamilies(Families.count(teams));
    }

    private String apiQuery() {
        return query.isEmpty() ? null : SearchText.withoutAccents(query);
    }

    private String emptyMessage() {
        return query.isEmpty() ? getString(R.string.market_empty)
                : getString(R.string.search_no_results, query);
    }

    private void showTab(boolean artifacts) {
        heroesPanel.setVisibility(artifacts ? View.GONE : View.VISIBLE);
        artifactsPanel.setVisibility(artifacts ? View.VISIBLE : View.GONE);
    }

    private void showMore() {
        if (loadingBatch) return;
        loadingBatch = true;
        stateView.hide();
        if (adapter.isEmpty()) {
            adapter.showFooterSkeleton();
        } else {
            adapter.showFooterLoading();
        }
        fillBatch(new ArrayList<>(), 0);
    }

    private void fillBatch(List<MarketItem> batch, int pagesLoaded) {
        while (batch.size() < GameBalance.MARKET_BATCH_SIZE && !candidates.isEmpty()) {
            Character c = candidates.poll();
            if (shownIds.contains(c.getId()) || playerStore.get().owns(c.getId())) continue;
            shownIds.add(c.getId());
            batch.add(new MarketItem(c));
        }
        boolean full = batch.size() >= GameBalance.MARKET_BATCH_SIZE;
        if (full || !hasMorePages || pagesLoaded >= MAX_PAGES_PER_BATCH) {
            deliverBatch(batch);
            return;
        }

        pageRequest = repository.loadCharacters(apiQuery(), MARKET_SORT, nextOffset,
                new RepoCallback<CharacterPage>() {
                    @Override
                    public void onSuccess(CharacterPage page) {
                        nextOffset = page.getNextOffset();
                        hasMorePages = page.hasMore();
                        candidates.addAll(page.getCharacters());
                        fillBatch(batch, pagesLoaded + 1);
                    }

                    @Override
                    public void onError(String message) {
                        loadingBatch = false;
                        if (!batch.isEmpty()) {
                            adapter.addItems(batch);
                            enqueueDetails(batch);
                        }
                        if (adapter.isEmpty()) {
                            showState(R.drawable.ic_error, message, getString(R.string.action_retry));
                        } else {
                            adapter.showFooterMessage(message, getString(R.string.action_retry));
                        }
                    }
                });
    }

    private void deliverBatch(List<MarketItem> batch) {
        loadingBatch = false;
        adapter.addItems(batch);
        enqueueDetails(batch);

        boolean moreAvailable = !candidates.isEmpty() || hasMorePages;
        if (adapter.isEmpty()) {
            showState(emptyIcon(), emptyMessage(),
                    moreAvailable ? getString(R.string.market_more_action) : null);
        } else if (moreAvailable) {
            adapter.showFooterMessage(getString(R.string.market_more_message),
                    getString(R.string.market_more_action));
        } else {
            adapter.hideFooter();
        }
    }

    private void enqueueDetails(List<MarketItem> items) {
        pendingDetails.addAll(items);
        loadNextDetail();
    }

    private void loadNextDetail() {
        if (loadingDetail || pendingDetails.isEmpty()) return;
        MarketItem item = pendingDetails.poll();
        loadingDetail = true;
        detailRequest = repository.loadCharacterDetail(item.character.getId(),
                new RepoCallback<Character>() {
                    @Override
                    public void onSuccess(Character detail) {
                        loadingDetail = false;
                        item.setDetails(detail, AttributeCalculator.calculate(detail));
                        adapter.itemChanged(item);
                        updateFamilies();
                        loadNextDetail();
                    }

                    @Override
                    public void onError(String message) {
                        loadingDetail = false;
                        item.failed = true;
                        adapter.itemChanged(item);
                        loadNextDetail();
                    }
                });
    }

    private void showState(int iconRes, String message, String buttonText) {
        adapter.hideFooter();
        stateView.show(iconRes, null, message, buttonText, this::showMore);
    }

    private int emptyIcon() {
        return query.isEmpty() ? R.drawable.ic_cart : R.drawable.ic_search;
    }

    @Override
    public void onBuyClick(MarketItem item) {
        if (!item.isReady()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.market_confirm_title, item.character.getName()))
                .setMessage(getString(R.string.market_confirm_message, PlayerHud.format(item.price)))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.market_buy, (dialog, which) -> buy(item))
                .show();
    }

    private void buy(MarketItem item) {
        boolean bought = playerStore.buyHero(OwnedHero.fromCharacter(item.character), item.price);
        if (!bought) {
            snackbar(getString(R.string.market_buy_failed));
            return;
        }
        adapter.remove(item);
        updateFamilies();
        refreshCoins();
        snackbar(getString(R.string.market_bought, item.character.getName()));
        if (adapter.isEmpty()) {
            boolean moreAvailable = !candidates.isEmpty() || hasMorePages;
            showState(emptyIcon(), emptyMessage(),
                    moreAvailable ? getString(R.string.market_more_action) : null);
        }
    }

    @Override
    public void onItemClick(MarketItem item) {
        startActivity(CharacterDetailActivity.newIntent(this, item.character));
    }

    @Override
    public void onRetryItem(MarketItem item) {
        item.failed = false;
        adapter.itemChanged(item);
        pendingDetails.add(item);
        loadNextDetail();
    }

    @Override
    public void onFooterAction() {
        showMore();
    }

    private void refreshCoins() {
        PlayerHud.bind(playerHud, playerStore.get());
        adapter.setCoins(playerStore.get().getCoins());
        roulettes.bindButtons();
    }

    @Override
    public void onCoinsChanged() {
        refreshCoins();
    }

    @Override
    public void showMessage(String text) {
        snackbar(text);
    }

    private void snackbar(String text) {
        Snackbar.make(root, text, Snackbar.LENGTH_LONG).setAnchorView(bottomNav).show();
    }
}
