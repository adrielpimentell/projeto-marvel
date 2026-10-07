package com.example.marvel.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.auth.Session;
import com.example.marvel.data.season.RankingEntry;
import com.example.marvel.data.season.RankingPage;
import com.example.marvel.data.season.SeasonRepository;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.ArtifactCatalog;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Season;
import com.example.marvel.game.TeamAlbum;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.Avatars;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.ui.ranking.SeasonCountdown;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT).withZone(Season.ZONE);

    private PlayerStore playerStore;
    private int loadToken;

    public static Intent newIntent(Context context) {
        return new Intent(context, ProfileActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_profile);
        Screens.padForSystemBars(findViewById(R.id.profile_root));
        playerStore = PlayerStore.getInstance(this);
        findViewById(R.id.profile_back).setOnClickListener(v -> finish());
        bindAccountActions();
    }

    private void bindAccountActions() {
        ViewGroup actions = findViewById(R.id.profile_account_actions);
        actions.removeAllViews();
        addAction(actions, R.drawable.ic_person, R.string.account_change_avatar, false,
                () -> AccountDialogs.showAvatarPicker(this, this::rebindHeader));
        addAction(actions, R.drawable.ic_edit, R.string.account_change_name, false,
                () -> AccountDialogs.showRename(this, this::rebindHeader));
        addAction(actions, R.drawable.ic_lock, R.string.account_change_password, false,
                () -> AccountDialogs.showChangePassword(this));
        addAction(actions, R.drawable.ic_logout, R.string.account_sign_out, false,
                () -> AccountDialogs.showSignOut(this));
        addAction(actions, R.drawable.ic_delete, R.string.account_delete, true,
                () -> AccountDialogs.showDelete(this));
    }

    private void addAction(ViewGroup parent, int iconRes, int textRes, boolean danger, Runnable onClick) {
        View row = LayoutInflater.from(this).inflate(R.layout.view_profile_action, parent, false);
        ImageView icon = row.findViewById(R.id.action_icon);
        icon.setImageResource(iconRes);
        TextView text = row.findViewById(R.id.action_text);
        text.setText(textRes);
        if (danger) {
            int red = getColor(R.color.red);
            icon.setImageTintList(ColorStateList.valueOf(red));
            text.setTextColor(red);
            ((ImageView) row.findViewById(R.id.action_chevron)).setImageTintList(ColorStateList.valueOf(red));
        }
        row.setContentDescription(getString(textRes));
        row.setOnClickListener(v -> onClick.run());
        parent.addView(row);
    }

    private void rebindHeader() {
        if (isFinishing() || isDestroyed()) return;
        bindHeader(playerStore.get());
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerStore.ensureSeason(Season.currentId());
        PlayerState state = playerStore.get();
        bindHeader(state);
        bindSeason(state);
        bindCollection(state);
        bindRecords(state);
        AuthRepository.getInstance(this).refreshProfile(this::rebindHeader);
    }

    private void bindHeader(PlayerState state) {
        String name = AuthRepository.getInstance(this).cachedPlayerName();
        String rank = PlayerHud.rankName(this, state.getRank());
        Avatars.bindMine(findViewById(R.id.profile_avatar), findViewById(R.id.profile_avatar_image));
        ((TextView) findViewById(R.id.profile_name)).setText(name == null ? getString(R.string.profile_none) : name);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user == null || user.getEmail() == null ? "" : user.getEmail();
        TextView emailRank = findViewById(R.id.profile_email_rank);
        emailRank.setText(email.isEmpty() ? rank : getString(R.string.profile_email_rank, email, rank));

        TextView since = findViewById(R.id.profile_since);
        FirebaseUserMetadata metadata = user == null ? null : user.getMetadata();
        if (metadata == null || metadata.getCreationTimestamp() <= 0) {
            since.setVisibility(View.GONE);
        } else {
            since.setVisibility(View.VISIBLE);
            since.setText(getString(R.string.profile_since,
                    DATE.format(Instant.ofEpochMilli(metadata.getCreationTimestamp()))));
        }
    }

    private void bindSeason(PlayerState state) {
        Instant now = Season.now();
        setStat(findViewById(R.id.profile_season_trophies), PlayerHud.format(state.getTrophies()),
                getString(R.string.profile_season_trophies), 0);
        setStat(findViewById(R.id.profile_season_time),
                SeasonCountdown.time(this, Duration.between(now, Season.endOf(now))),
                getString(R.string.profile_season_time), 0);
        View position = findViewById(R.id.profile_season_position);
        setStat(position, getString(R.string.profile_position_loading),
                getString(R.string.profile_season_position), 0);
        TextView hint = findViewById(R.id.profile_season_hint);
        hint.setVisibility(View.GONE);

        String uid = Session.uid();
        if (uid == null) return;
        int token = ++loadToken;
        SeasonRepository.getInstance(this).loadMyPosition(Season.idAt(now), uid,
                new RankingPage(Collections.emptyList(), false),
                new SeasonRepository.Callback<RankingEntry>() {
                    @Override
                    public void onSuccess(RankingEntry me) {
                        if (token != loadToken || isDestroyed()) return;
                        if (me == null) {
                            setStat(position, getString(R.string.profile_position_none),
                                    getString(R.string.profile_season_position), 0);
                            hint.setText(R.string.profile_position_hint_none);
                            hint.setVisibility(View.VISIBLE);
                        } else {
                            setStat(position, getString(R.string.profile_position_value,
                                            PlayerHud.format(me.getPosition())),
                                    getString(R.string.profile_season_position), 0);
                        }
                    }

                    @Override
                    public void onError(int messageRes) {
                        if (token != loadToken || isDestroyed()) return;
                        setStat(position, getString(R.string.profile_position_none),
                                getString(R.string.profile_season_position), 0);
                        hint.setText(messageRes);
                        hint.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void bindCollection(PlayerState state) {
        ViewGroup rows = findViewById(R.id.profile_collection_rows);
        rows.removeAllViews();
        OwnedHero active = state.getActiveHero();
        String heroName = active.hasInfo() ? active.getName() : getString(R.string.starter_hero_fallback);
        addRow(rows, R.string.profile_coins, PlayerHud.format(state.getCoins()));
        addRow(rows, R.string.profile_active_hero, heroName);
        addRow(rows, R.string.profile_heroes, PlayerHud.format(state.getHeroes().size()));
        addRow(rows, R.string.profile_artifacts, getString(R.string.profile_artifacts_value,
                state.getArtifactIds().size(), ArtifactCatalog.all().size()));
        addRow(rows, R.string.profile_album_teams, getString(R.string.profile_album_value,
                completeTeams(state), GameBalance.ALBUM_TEAM_IDS.length));

        bindRarity(state, R.id.profile_rarity_common, Artifact.Rarity.COMMON);
        bindRarity(state, R.id.profile_rarity_rare, Artifact.Rarity.RARE);
        bindRarity(state, R.id.profile_rarity_epic, Artifact.Rarity.EPIC);
        bindRarity(state, R.id.profile_rarity_legendary, Artifact.Rarity.LEGENDARY);
    }

    private void bindRarity(PlayerState state, int columnId, Artifact.Rarity rarity) {
        int owned = 0;
        for (Artifact artifact : ArtifactCatalog.ofRarity(rarity)) {
            if (state.ownsArtifact(artifact.getId())) owned++;
        }
        String value = getString(R.string.profile_artifacts_value, owned, ArtifactCatalog.ofRarity(rarity).size());
        setStat(findViewById(columnId), value, ArtifactUi.rarityName(this, rarity),
                ArtifactUi.rarityColor(this, rarity));
    }

    private static int completeTeams(PlayerState state) {
        int complete = 0;
        for (int teamId : GameBalance.ALBUM_TEAM_IDS) {
            if (state.getClaimedMilestones(teamId) >= TeamAlbum.milestoneCount()) complete++;
        }
        return complete;
    }

    private void bindRecords(PlayerState state) {
        ViewGroup rows = findViewById(R.id.profile_records_rows);
        rows.removeAllViews();
        int bestFloor = state.getSurvivalBestFloor();
        addRow(rows, R.string.profile_survival_floor,
                bestFloor > 0 ? PlayerHud.format(bestFloor) : getString(R.string.profile_none));
        addRow(rows, R.string.profile_wins, PlayerHud.format(state.getWins()));
        addRow(rows, R.string.profile_losses, PlayerHud.format(state.getLosses()));
        addRow(rows, R.string.profile_best_rank, PlayerHud.rankName(this, state.getBestRank()));
        addRow(rows, R.string.profile_seasons_awarded, PlayerHud.format(state.getSeasonPrizesCount()));
    }

    private void addRow(ViewGroup parent, int labelRes, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.view_profile_row, parent, false);
        String label = getString(labelRes);
        ((TextView) row.findViewById(R.id.profile_row_label)).setText(label);
        ((TextView) row.findViewById(R.id.profile_row_value)).setText(value);
        row.setContentDescription(label + ": " + value);
        parent.addView(row);
    }

    private static void setStat(View column, String value, String label, int valueColor) {
        TextView valueView = column.findViewById(R.id.stat_value);
        valueView.setText(value);
        if (valueColor != 0) valueView.setTextColor(valueColor);
        ((TextView) column.findViewById(R.id.stat_label)).setText(label);
        column.findViewById(R.id.stat_bonus).setVisibility(View.GONE);
        column.setContentDescription(label + ": " + value);
    }
}
