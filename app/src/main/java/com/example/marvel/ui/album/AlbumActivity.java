package com.example.marvel.ui.album;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.data.model.Team;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.TeamAlbum;
import com.example.marvel.ui.common.MainNav;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.StateView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity implements AlbumAdapter.Listener {

    private CharacterRepository repository;
    private PlayerStore playerStore;
    private AlbumAdapter adapter;
    private final List<AlbumEntry> entries = new ArrayList<>();

    private View playerHud;
    private TextView sealCount;
    private TextView subtitle;
    private StateView stateView;
    private RecyclerView list;

    private final ArrayDeque<AlbumEntry> pending = new ArrayDeque<>();
    private boolean loading;
    private RequestHandle request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_album);

        repository = CharacterRepository.getInstance(this);
        playerStore = PlayerStore.getInstance(this);
        View root = findViewById(R.id.album_root);
        playerHud = findViewById(R.id.player_hud);
        sealCount = findViewById(R.id.album_seal_count);
        subtitle = findViewById(R.id.album_subtitle);
        stateView = new StateView(findViewById(R.id.album_state));

        Screens.padForSystemBars(root);

        list = findViewById(R.id.album_list);
        adapter = new AlbumAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        MainNav.setup(this, bottomNav, R.id.nav_album, () -> list.smoothScrollToPosition(0));

        for (int teamId : GameBalance.ALBUM_TEAM_IDS) {
            entries.add(new AlbumEntry(teamId));
        }
        adapter.setEntries(entries);
        pending.addAll(entries);
        loadNext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }

    @Override
    public void onEntryClick(AlbumEntry entry) {
        if (entry.isReady()) {
            startActivity(TeamPageActivity.newIntent(this, entry.teamId));
        } else if (entry.failed) {
            entry.failed = false;
            adapter.changed(entry);
            pending.add(entry);
            loadNext();
        }
    }

    private void loadNext() {
        if (loading) return;
        AlbumEntry entry = pending.poll();
        if (entry == null) {
            updateEmptyState();
            return;
        }
        loading = true;
        request = repository.loadTeam(entry.teamId, new RepoCallback<Team>() {
            @Override
            public void onSuccess(Team team) {
                loading = false;
                if (AlbumEntry.isPlayable(team)) {
                    entry.setTeam(team);
                    adapter.changed(entry);
                } else {
                    entries.remove(entry);
                    adapter.remove(entry);
                }
                updateSummary();
                loadNext();
            }

            @Override
            public void onError(String message) {
                loading = false;
                entry.failed = true;
                adapter.changed(entry);
                loadNext();
            }
        });
    }

    private void retryFailed() {
        for (AlbumEntry entry : entries) {
            if (entry.failed && !pending.contains(entry)) {
                entry.failed = false;
                adapter.changed(entry);
                pending.add(entry);
            }
        }
        stateView.hide();
        list.setVisibility(View.VISIBLE);
        loadNext();
    }

    private void updateEmptyState() {
        boolean anyReady = false;
        for (AlbumEntry entry : entries) {
            if (entry.isReady()) anyReady = true;
        }
        if (anyReady || entries.isEmpty()) {
            stateView.hide();
            list.setVisibility(View.VISIBLE);
        } else {
            list.setVisibility(View.INVISIBLE);
            stateView.show(R.drawable.ic_error, null, getString(R.string.album_empty),
                    getString(R.string.action_retry), this::retryFailed);
        }
    }

    private void refresh() {
        PlayerState state = playerStore.get();
        PlayerHud.bind(playerHud, state);
        adapter.setState(state);
        updateSummary();
    }

    private void updateSummary() {
        PlayerState state = playerStore.get();
        int totalSeals = entries.size() * TeamAlbum.milestoneCount();
        sealCount.setText(getString(R.string.album_seals, state.countTeamSeals(), totalSeals));

        int claimable = 0;
        for (AlbumEntry entry : entries) {
            if (!entry.isReady()) continue;
            int reached = TeamAlbum.milestonesReached(TeamAlbum.countOwned(state, entry.memberIds));
            if (reached > state.getClaimedMilestones(entry.teamId)) claimable++;
        }
        if (claimable > 0) {
            subtitle.setText(getResources().getQuantityString(R.plurals.album_claimable, claimable, claimable));
            subtitle.setTextColor(getColor(R.color.red));
        } else {
            subtitle.setText(R.string.album_subtitle);
            subtitle.setTextColor(getColor(R.color.text_secondary));
        }
    }
}
