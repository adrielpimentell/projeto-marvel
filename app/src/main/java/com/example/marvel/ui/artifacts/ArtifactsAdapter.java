package com.example.marvel.ui.artifacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Cards;
import com.example.marvel.ui.common.UiTokens;
import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.ArtifactCatalog;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.ui.common.ArtifactUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

class ArtifactsAdapter extends RecyclerView.Adapter<ArtifactsAdapter.Holder> {

    interface Listener {
        void onEquip(Artifact artifact);

        void onRemove(Artifact artifact);
    }

    private final Listener listener;
    private final List<Artifact> items = new ArrayList<>();
    private PlayerState state;

    ArtifactsAdapter(Listener listener) {
        this.listener = listener;
    }

    void setState(PlayerState state) {
        this.state = state;
        items.clear();
        List<Artifact> missing = new ArrayList<>();
        for (int r = Artifact.Rarity.values().length - 1; r >= 0; r--) {
            for (Artifact artifact : ArtifactCatalog.ofRarity(Artifact.Rarity.values()[r])) {
                if (state.ownsArtifact(artifact.getId())) items.add(artifact);
                else missing.add(artifact);
            }
        }
        items.addAll(missing);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artifact, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView icon;
        private final TextView name;
        private final TextView rarity;
        private final TextView effect;
        private final TextView owner;
        private final MaterialButton button;
        private final View locked;

        Holder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.artifact_item_card);
            icon = itemView.findViewById(R.id.artifact_item_icon);
            name = itemView.findViewById(R.id.artifact_item_name);
            rarity = itemView.findViewById(R.id.artifact_item_rarity);
            effect = itemView.findViewById(R.id.artifact_item_effect);
            owner = itemView.findViewById(R.id.artifact_item_owner);
            button = itemView.findViewById(R.id.artifact_item_button);
            locked = itemView.findViewById(R.id.artifact_item_locked);
        }

        void bind(Artifact artifact) {
            Context context = itemView.getContext();
            int color = ArtifactUi.rarityColor(context, artifact.getRarity());
            boolean owned = state.ownsArtifact(artifact.getId());
            OwnedHero active = state.getActiveHero();
            boolean onActive = active.hasEquipped(artifact.getId());
            OwnedHero holderHero = state.whoHasEquipped(artifact.getId());

            icon.setColorFilter(color);
            name.setText(artifact.getName());
            rarity.setText(context.getString(R.string.artifacts_rarity_type,
                    ArtifactUi.rarityName(context, artifact.getRarity()),
                    ArtifactUi.typeName(context, artifact.getType())));
            rarity.setTextColor(color);
            effect.setText(artifact.describe());

            Cards.highlight(card, onActive);
            itemView.setAlpha(owned ? 1f : UiTokens.ALPHA_LOCKED);

            if (holderHero != null && !onActive) {
                owner.setVisibility(View.VISIBLE);
                owner.setText(context.getString(R.string.artifacts_with_hero,
                        holderHero.hasInfo() ? holderHero.getName()
                                : context.getString(R.string.starter_hero_fallback)));
            } else {
                owner.setVisibility(View.GONE);
            }

            if (!owned) {
                button.setVisibility(View.GONE);
                locked.setVisibility(View.VISIBLE);
                return;
            }
            locked.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
            if (onActive) {
                button.setText(R.string.artifacts_remove);
                button.setOnClickListener(v -> listener.onRemove(artifact));
            } else {
                button.setText(R.string.artifacts_equip);
                button.setOnClickListener(v -> listener.onEquip(artifact));
            }
        }
    }
}
