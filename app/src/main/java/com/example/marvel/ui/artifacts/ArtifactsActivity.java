package com.example.marvel.ui.artifacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.ArtifactCatalog;
import com.example.marvel.game.GameAttributes;
import com.example.marvel.game.Loadout;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.common.ArtifactUi;
import com.example.marvel.ui.common.PlayerHud;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ArtifactsActivity extends AppCompatActivity implements ArtifactsAdapter.Listener {

    private PlayerStore playerStore;
    private ArtifactsAdapter adapter;
    private View root;
    private View playerHud;

    public static Intent newIntent(Context context) {
        return new Intent(context, ArtifactsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_artifacts);

        playerStore = PlayerStore.getInstance(this);
        root = findViewById(R.id.artifacts_root);
        playerHud = findViewById(R.id.player_hud);

        Screens.padForSystemBars(root);
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.artifacts_list);
        adapter = new ArtifactsAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onEquip(Artifact artifact) {
        PlayerState.EquipResult result = playerStore.equipArtifact(artifact.getId());
        if (result == PlayerState.EquipResult.SLOTS_FULL) {
            snackbar(getString(R.string.artifacts_slots_full));
        } else if (result == PlayerState.EquipResult.OK) {
            snackbar(getString(R.string.artifacts_equipped_toast, artifact.getName(), activeHeroName()));
        }
        refresh();
    }

    @Override
    public void onRemove(Artifact artifact) {
        if (playerStore.unequipArtifact(artifact.getId())) {
            snackbar(getString(R.string.artifacts_removed_toast, artifact.getName()));
        }
        refresh();
    }

    private void refresh() {
        PlayerState state = playerStore.get();
        PlayerHud.bind(playerHud, state);
        adapter.setState(state);

        ((TextView) findViewById(R.id.artifacts_hero_name))
                .setText(getString(R.string.artifacts_active_hero, activeHeroName()));
        ((TextView) findViewById(R.id.artifacts_collection_label)).setText(getString(
                R.string.artifacts_collection, state.getArtifactIds().size(), ArtifactCatalog.all().size()));

        List<Artifact> equipped = Loadout.artifactsOf(state.getActiveHero());
        bindSlot(findViewById(R.id.slot_1), equipped.size() > 0 ? equipped.get(0) : null);
        bindSlot(findViewById(R.id.slot_2), equipped.size() > 1 ? equipped.get(1) : null);
        bindPreview(state.getActiveHero());
    }

    private void bindSlot(View slot, Artifact artifact) {
        ImageView icon = slot.findViewById(R.id.slot_icon);
        TextView name = slot.findViewById(R.id.slot_name);
        TextView hint = slot.findViewById(R.id.slot_hint);
        if (artifact == null) {
            icon.setColorFilter(getColor(R.color.icon_inactive));
            name.setText(R.string.artifacts_slot_empty);
            name.setTextColor(getColor(R.color.text_secondary));
            hint.setVisibility(View.GONE);
            slot.setBackgroundResource(R.drawable.bg_slot_empty);
            slot.setOnClickListener(null);
            slot.setClickable(false);
            return;
        }
        int color = ArtifactUi.rarityColor(this, artifact.getRarity());
        icon.setColorFilter(color);
        name.setText(artifact.getName());
        name.setTextColor(getColor(R.color.text_primary));
        hint.setVisibility(View.VISIBLE);
        hint.setText(R.string.artifacts_slot_hint);
        slot.setBackgroundResource(R.drawable.bg_card);
        slot.setOnClickListener(v -> onRemove(artifact));
    }

    private void bindPreview(OwnedHero hero) {
        GameAttributes base = hero.getAttributes();
        GameAttributes battle = hero.getBattleAttributes();
        bindStat(R.id.preview_life, R.string.stat_short_life, battle.getLife(), base.getLife());
        bindStat(R.id.preview_strength, R.string.stat_short_strength, battle.getStrength(), base.getStrength());
        bindStat(R.id.preview_speed, R.string.stat_short_speed, battle.getSpeed(), base.getSpeed());
        bindStat(R.id.preview_intelligence, R.string.stat_short_intelligence,
                battle.getIntelligence(), base.getIntelligence());
        bindStat(R.id.preview_overall, R.string.stat_short_overall, battle.getOverall(), base.getOverall());
    }

    private void bindStat(int columnId, int labelRes, int value, int baseValue) {
        View column = findViewById(columnId);
        ((TextView) column.findViewById(R.id.stat_value)).setText(String.valueOf(value));
        ((TextView) column.findViewById(R.id.stat_label)).setText(labelRes);
        TextView bonus = column.findViewById(R.id.stat_bonus);
        int diff = value - baseValue;
        bonus.setVisibility(diff > 0 ? View.VISIBLE : View.INVISIBLE);
        bonus.setText(getString(R.string.stat_bonus, diff));
    }

    private String activeHeroName() {
        OwnedHero hero = playerStore.get().getActiveHero();
        return hero.hasInfo() ? hero.getName() : getString(R.string.starter_hero_fallback);
    }

    private void snackbar(String text) {
        Snackbar.make(root, text, Snackbar.LENGTH_SHORT).show();
    }
}
