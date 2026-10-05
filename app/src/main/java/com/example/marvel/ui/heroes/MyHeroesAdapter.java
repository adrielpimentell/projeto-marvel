package com.example.marvel.ui.heroes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Cards;
import com.example.marvel.ui.common.Images;
import com.example.marvel.R;
import com.example.marvel.game.Economy;
import com.example.marvel.game.Artifact;
import com.example.marvel.game.GameAttributes;
import com.example.marvel.game.Loadout;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

class MyHeroesAdapter extends RecyclerView.Adapter<MyHeroesAdapter.HeroHolder> {

    interface Listener {
        void onUseHero(OwnedHero hero);

        void onUpgrade(OwnedHero hero, OwnedHero.Stat stat);
    }

    private final Listener listener;
    private final List<OwnedHero> heroes = new ArrayList<>();
    private int activeHeroId;
    private int coins;

    MyHeroesAdapter(Listener listener) {
        this.listener = listener;
    }

    void setState(PlayerState state, List<OwnedHero> visible) {
        boolean sameList = heroes.equals(visible);
        heroes.clear();
        heroes.addAll(visible);
        activeHeroId = state.getActiveHero().getCharacterId();
        coins = state.getCoins();
        if (sameList) {
            notifyItemRangeChanged(0, heroes.size());
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return heroes.size();
    }

    @NonNull
    @Override
    public HeroHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HeroHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_hero, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HeroHolder holder, int position) {
        holder.bind(heroes.get(position));
    }

    class HeroHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView image;
        private final TextView name;
        private final TextView realName;
        private final TextView overall;
        private final TextView artifacts;
        private final View activeBadge;
        private final MaterialButton useButton;
        private final View lifeRow;
        private final View strengthRow;
        private final View speedRow;
        private final View intelligenceRow;

        HeroHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.hero_card);
            image = itemView.findViewById(R.id.hero_image);
            name = itemView.findViewById(R.id.hero_name);
            realName = itemView.findViewById(R.id.hero_real_name);
            overall = itemView.findViewById(R.id.hero_overall);
            artifacts = itemView.findViewById(R.id.hero_artifacts);
            activeBadge = itemView.findViewById(R.id.hero_active_badge);
            useButton = itemView.findViewById(R.id.hero_use_button);
            lifeRow = itemView.findViewById(R.id.upgrade_life);
            strengthRow = itemView.findViewById(R.id.upgrade_strength);
            speedRow = itemView.findViewById(R.id.upgrade_speed);
            intelligenceRow = itemView.findViewById(R.id.upgrade_intelligence);
        }

        void bind(OwnedHero hero) {
            Context context = itemView.getContext();
            boolean active = hero.getCharacterId() == activeHeroId;
            GameAttributes a = hero.getAttributes();

            name.setText(hero.hasInfo() ? hero.getName() : context.getString(R.string.starter_hero_fallback));
            String real = hero.getRealName();
            realName.setText(real.isEmpty() ? context.getString(R.string.real_name_unknown) : real);
            overall.setText(context.getString(R.string.market_overall, a.getOverall()));

            List<String> names = new ArrayList<>();
            for (Artifact artifact : Loadout.artifactsOf(hero)) names.add(artifact.getName());
            artifacts.setVisibility(names.isEmpty() ? View.GONE : View.VISIBLE);
            artifacts.setText(context.getString(R.string.artifacts_hero_line, String.join(", ", names)));

            Cards.highlight(card, active);
            activeBadge.setVisibility(active ? View.VISIBLE : View.GONE);
            useButton.setVisibility(active ? View.GONE : View.VISIBLE);
            useButton.setOnClickListener(v -> listener.onUseHero(hero));

            bindUpgrade(lifeRow, hero, OwnedHero.Stat.LIFE, R.string.attr_life, a.getLife());
            bindUpgrade(strengthRow, hero, OwnedHero.Stat.STRENGTH, R.string.attr_strength, a.getStrength());
            bindUpgrade(speedRow, hero, OwnedHero.Stat.SPEED, R.string.attr_speed, a.getSpeed());
            bindUpgrade(intelligenceRow, hero, OwnedHero.Stat.INTELLIGENCE,
                    R.string.attr_intelligence, a.getIntelligence());

            Images.load(image, hero.getImageUrl());
        }

        private void bindUpgrade(View row, OwnedHero hero, OwnedHero.Stat stat, int labelRes, int value) {
            Context context = row.getContext();
            TextView label = row.findViewById(R.id.upgrade_label);
            LinearProgressIndicator bar = row.findViewById(R.id.upgrade_bar);
            TextView valueText = row.findViewById(R.id.upgrade_value);
            MaterialButton button = row.findViewById(R.id.upgrade_button);

            label.setText(labelRes);
            valueText.setText(String.valueOf(value));
            bar.setProgressCompat(value, true);

            if (hero.isMaxed(stat)) {
                button.setText(R.string.upgrade_max);
                button.setIcon(null);
                button.setEnabled(false);
                button.setContentDescription(null);
                return;
            }
            int cost = Economy.upgradeCost(hero.getUpgrades(stat));
            button.setText(String.valueOf(cost));
            button.setIconResource(R.drawable.ic_add);
            button.setEnabled(coins >= cost);
            button.setContentDescription(context.getString(R.string.upgrade_description,
                    context.getString(labelRes), cost));
            button.setOnClickListener(v -> listener.onUpgrade(hero, stat));
        }
    }
}
