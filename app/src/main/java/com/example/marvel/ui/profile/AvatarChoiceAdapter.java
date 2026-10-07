package com.example.marvel.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.ui.common.Avatars;

import java.util.List;

final class AvatarChoiceAdapter extends RecyclerView.Adapter<AvatarChoiceAdapter.Holder> {

    interface Listener {

        void onChosen(int heroId);
    }

    private final String playerName;
    private final List<OwnedHero> heroes;
    private final int selectedHeroId;
    private final Listener listener;

    AvatarChoiceAdapter(String playerName, List<OwnedHero> heroes, int selectedHeroId, Listener listener) {
        this.playerName = playerName;
        this.heroes = heroes;
        this.selectedHeroId = selectedHeroId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar_choice, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TextView initial = holder.itemView.findViewById(R.id.choice_initial);
        ImageView image = holder.itemView.findViewById(R.id.choice_image);
        TextView name = holder.itemView.findViewById(R.id.choice_name);
        int heroId;
        String label;
        if (position == 0) {
            heroId = 0;
            label = holder.itemView.getContext().getString(R.string.avatar_initial);
            Avatars.bind(initial, image, playerName, null);
        } else {
            OwnedHero hero = heroes.get(position - 1);
            heroId = hero.getCharacterId();
            label = hero.getName();
            Avatars.bind(initial, image, playerName, hero.getImageUrl());
        }
        name.setText(label);
        holder.itemView.findViewById(R.id.choice_ring)
                .setVisibility(heroId == selectedHeroId ? View.VISIBLE : View.GONE);
        holder.itemView.setContentDescription(
                holder.itemView.getContext().getString(R.string.avatar_choice_description, label));
        holder.itemView.setSelected(heroId == selectedHeroId);
        holder.itemView.setOnClickListener(v -> listener.onChosen(heroId));
    }

    @Override
    public int getItemCount() {
        return heroes.size() + 1;
    }

    static final class Holder extends RecyclerView.ViewHolder {

        Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
