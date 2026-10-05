package com.example.marvel.ui.album;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;
import com.example.marvel.data.model.Team;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.Skeleton;
import com.example.marvel.ui.common.StateView;
import com.example.marvel.ui.common.TeamUi;
import com.example.marvel.ui.detail.CharacterDetailActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TeamPageActivity extends AppCompatActivity implements TeamPageAdapter.Listener {

    private static final String EXTRA_TEAM_ID = "extra_team_id";
    private static final int GRID_COLUMNS = 3;

    private CharacterRepository repository;
    private PlayerStore playerStore;
    private TeamPageAdapter adapter;
    private int teamId;
    private Team team;

    private View root;
    private View playerHud;
    private RecyclerView grid;
    private View progress;
    private StateView errorView;
    private RequestHandle teamRequest;
    private RequestHandle imagesRequest;

    public static Intent newIntent(Context context, int teamId) {
        return new Intent(context, TeamPageActivity.class).putExtra(EXTRA_TEAM_ID, teamId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_team_page);

        teamId = getIntent().getIntExtra(EXTRA_TEAM_ID, 0);
        repository = CharacterRepository.getInstance(this);
        playerStore = PlayerStore.getInstance(this);
        root = findViewById(R.id.team_root);
        playerHud = findViewById(R.id.player_hud);
        grid = findViewById(R.id.team_grid);
        progress = findViewById(R.id.team_progress_bar);
        errorView = new StateView(findViewById(R.id.team_error));

        Screens.padForSystemBars(root);
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        adapter = new TeamPageAdapter(this);
        GridLayoutManager layout = new GridLayoutManager(this, GRID_COLUMNS);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isFullWidth(position) ? GRID_COLUMNS : 1;
            }
        });
        grid.setLayoutManager(layout);
        grid.setAdapter(adapter);
        loadTeam();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerHud.bind(playerHud, playerStore.get());
        if (team != null) showTeam();
    }

    @Override
    protected void onDestroy() {
        if (teamRequest != null) teamRequest.cancel();
        if (imagesRequest != null) imagesRequest.cancel();
        super.onDestroy();
    }

    private void loadTeam() {
        Skeleton.show(progress);
        errorView.hide();
        teamRequest = repository.loadTeam(teamId, new RepoCallback<Team>() {
            @Override
            public void onSuccess(Team loaded) {
                Skeleton.hide(progress);
                team = loaded;
                showTeam();
                loadImages();
            }

            @Override
            public void onError(String message) {
                Skeleton.hide(progress);
                errorView.show(R.drawable.ic_error, null, message,
                        getString(R.string.action_retry), () -> loadTeam());
            }
        });
    }

    private void showTeam() {
        PlayerState state = playerStore.get();
        List<NamedRef> owned = new ArrayList<>();
        List<NamedRef> missing = new ArrayList<>();
        for (NamedRef member : team.getMembers()) {
            if (member == null) continue;
            if (state.owns(member.getId())) owned.add(member);
            else missing.add(member);
        }
        owned.sort(Comparator.comparing(m -> m.getName().toLowerCase(Locale.ROOT)));
        missing.sort(Comparator.comparingInt(NamedRef::getId));
        adapter.setData(team, state, owned, missing);
    }

    private void loadImages() {
        PlayerState state = playerStore.get();
        List<Integer> ids = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();
        for (NamedRef member : team.getMembers()) {
            if (member == null) continue;
            if (state.owns(member.getId())) ids.add(member.getId());
            else missing.add(member.getId());
        }
        missing.sort(Integer::compare);
        ids.addAll(missing);
        imagesRequest = repository.loadCharactersByIds(ids, new RepoCallback<List<Character>>() {
            @Override
            public void onSuccess(List<Character> characters) {
                adapter.setCharacters(characters);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    @Override
    public void onClaim() {
        if (team == null) return;
        int before = playerStore.get().getClaimedMilestones(teamId);
        int coins = playerStore.claimTeamRewards(teamId, team.getMemberIds());
        showTeam();
        if (coins <= 0) {
            snackbar(getString(R.string.team_claim_failed));
            return;
        }
        int after = playerStore.get().getClaimedMilestones(teamId);
        root.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        PlayerHud.bind(playerHud, playerStore.get());
        adapter.popSeals(before, after);
        snackbar(getString(R.string.team_claimed_toast, PlayerHud.format(coins), sealsText(before, after)));
    }

    private String sealsText(int from, int to) {
        List<String> names = new ArrayList<>();
        for (int i = from; i < to; i++) names.add(TeamUi.sealName(this, i));
        String joined = names.get(names.size() - 1);
        if (names.size() > 1) {
            String head = String.join(", ", names.subList(0, names.size() - 1));
            joined = getString(R.string.team_seals_and, head, joined);
        }
        return getResources().getQuantityString(R.plurals.team_seals_earned, names.size(), joined);
    }

    @Override
    public void onMemberClick(Character character) {
        startActivity(CharacterDetailActivity.newIntent(this, character));
    }

    private void snackbar(String text) {
        Snackbar.make(root, text, Snackbar.LENGTH_LONG).show();
    }
}
