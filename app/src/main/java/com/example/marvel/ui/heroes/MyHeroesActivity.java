package com.example.marvel.ui.heroes;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.model.NamedRef;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.artifacts.ArtifactsActivity;
import com.example.marvel.ui.auth.LoginActivity;
import com.example.marvel.ui.auth.SignUpActivity;
import com.example.marvel.ui.common.Families;
import com.example.marvel.ui.common.FamilyFilter;
import com.example.marvel.ui.common.MainNav;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.SearchBar;
import com.example.marvel.ui.common.SearchText;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MyHeroesActivity extends AppCompatActivity implements MyHeroesAdapter.Listener {

    private PlayerStore playerStore;
    private MyHeroesAdapter adapter;
    private View root;
    private View playerHud;
    private BottomNavigationView bottomNav;
    private MaterialButton openArtifacts;
    private SearchBar searchBar;
    private FamilyFilter familyFilter;
    private TextView emptyText;
    private TextView accountText;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_my_heroes);

        playerStore = PlayerStore.getInstance(this);
        root = findViewById(R.id.my_heroes_root);
        playerHud = findViewById(R.id.player_hud);
        bottomNav = findViewById(R.id.bottom_nav);
        openArtifacts = findViewById(R.id.open_artifacts);
        openArtifacts.setOnClickListener(v -> startActivity(ArtifactsActivity.newIntent(this)));

        Screens.padForSystemBars(root);

        RecyclerView list = findViewById(R.id.my_heroes_list);
        adapter = new MyHeroesAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        emptyText = findViewById(R.id.my_heroes_empty);
        searchBar = new SearchBar(findViewById(R.id.my_heroes_search), R.string.search_hint_hero,
                SearchBar.DEFAULT_DELAY_MS, text -> {
                    query = text;
                    refresh();
                    list.scrollToPosition(0);
                });
        familyFilter = new FamilyFilter(findViewById(R.id.my_heroes_family), familyId -> {
            refresh();
            list.scrollToPosition(0);
        });
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) searchBar.hideKeyboard();
            }
        });

        MainNav.setup(this, bottomNav, R.id.nav_my_heroes, () -> list.smoothScrollToPosition(0));

        accountText = findViewById(R.id.my_heroes_account);
        findViewById(R.id.my_heroes_sign_out).setOnClickListener(v -> confirmSignOut());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        bindAccount();
    }

    private void bindAccount() {
        AuthRepository.getInstance(this).loadPlayerName(new AuthRepository.Callback<String>() {
            @Override
            public void onSuccess(String name) {
                if (isFinishing() || isDestroyed()) return;
                if (name == null) {
                    accountText.setText(R.string.account_choose_name);
                    accountText.setOnClickListener(v ->
                            startActivity(SignUpActivity.newChooseNameIntent(MyHeroesActivity.this)));
                } else {
                    accountText.setText(getString(R.string.account_playing_as, name));
                    accountText.setOnClickListener(null);
                    accountText.setClickable(false);
                }
            }

            @Override
            public void onError(int messageRes) {
                if (isFinishing() || isDestroyed()) return;
                accountText.setText(null);
            }
        });
    }

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account_sign_out_title)
                .setMessage(R.string.account_sign_out_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.account_sign_out, (dialog, which) -> {
                    AuthRepository.getInstance(this).signOut();
                    startActivity(LoginActivity.newSignedOutIntent(this));
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        searchBar.release();
        super.onDestroy();
    }

    @Override
    public void onUseHero(OwnedHero hero) {
        if (playerStore.setActiveHero(hero.getCharacterId())) {
            refresh();
            String name = hero.hasInfo() ? hero.getName() : getString(R.string.starter_hero_fallback);
            snackbar(getString(R.string.hero_now_active, name));
        }
    }

    @Override
    public void onUpgrade(OwnedHero hero, OwnedHero.Stat stat) {
        if (!playerStore.upgrade(hero.getCharacterId(), stat)) {
            snackbar(getString(R.string.upgrade_failed));
        }
        refresh();
    }

    private void refresh() {
        PlayerState state = playerStore.get();
        PlayerHud.bind(playerHud, state);
        List<List<NamedRef>> teams = new ArrayList<>();
        for (OwnedHero hero : state.getHeroes()) teams.add(hero.getTeams());
        familyFilter.setFamilies(Families.count(teams));

        List<OwnedHero> visible = visibleHeroes(state);
        adapter.setState(state, visible);
        emptyText.setText(familyFilter.getSelectedId() != 0
                ? getString(R.string.family_no_results, familyFilter.getSelectedName())
                : getString(R.string.search_no_results, query));
        emptyText.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        openArtifacts.setText(getString(R.string.artifacts_open_button,
                playerStore.get().getActiveHero().getEquippedArtifactIds().size(),
                GameBalance.MAX_EQUIPPED_ARTIFACTS));
    }

    private List<OwnedHero> visibleHeroes(PlayerState state) {
        List<OwnedHero> visible = new ArrayList<>();
        for (OwnedHero hero : state.getHeroes()) {
            if (!Families.contains(hero.getTeams(), familyFilter.getSelectedId())) continue;
            String name = hero.hasInfo() ? hero.getName() : getString(R.string.starter_hero_fallback);
            if (SearchText.matches(name, query)
                    || (!query.isEmpty() && SearchText.matches(hero.getRealName(), query))) {
                visible.add(hero);
            }
        }
        return visible;
    }

    private void snackbar(String text) {
        Snackbar.make(root, text, Snackbar.LENGTH_SHORT).setAnchorView(bottomNav).show();
    }
}
