package com.example.marvel.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import com.example.marvel.R;
import com.example.marvel.game.Artifact;
import com.google.android.material.card.MaterialCardView;

public final class RewardCard {

    private RewardCard() {
    }

    public static void bind(MaterialCardView card, Artifact.Rarity rarity, Artifact artifact, int coins) {
        Context context = card.getContext();
        int color = ArtifactUi.rarityColor(context, rarity);
        card.setStrokeColor(ColorStateList.valueOf(color));

        TextView rarityView = card.findViewById(R.id.artifact_rarity);
        rarityView.setText(ArtifactUi.rarityName(context, rarity));
        rarityView.setTextColor(color);

        TextView name = card.findViewById(R.id.artifact_name);
        TextView type = card.findViewById(R.id.artifact_type);
        TextView effect = card.findViewById(R.id.artifact_effect);
        if (artifact != null) {
            name.setText(artifact.getName());
            type.setVisibility(View.VISIBLE);
            type.setText(ArtifactUi.typeName(context, artifact.getType()));
            effect.setText(artifact.describe());
        } else {
            name.setText(context.getString(R.string.chest_coins_name, PlayerHud.format(coins)));
            type.setVisibility(View.GONE);
            effect.setText(R.string.chest_coins_effect);
        }
    }
}
