package com.example.marvel.ui.album;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.UiTokens;
import com.example.marvel.ui.common.Images;
import com.example.marvel.R;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.TeamAlbum;
import com.example.marvel.ui.common.TeamUi;

import java.util.ArrayList;
import java.util.List;

class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.Holder> {

    interface Listener {
        void onEntryClick(AlbumEntry entry);
    }


    private final Listener listener;
    private final List<AlbumEntry> entries = new ArrayList<>();
    private PlayerState state;

    AlbumAdapter(Listener listener) {
        this.listener = listener;
    }

    void setEntries(List<AlbumEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    void setState(PlayerState state) {
        this.state = state;
        notifyItemRangeChanged(0, entries.size());
    }

    void changed(AlbumEntry entry) {
        int index = entries.indexOf(entry);
        if (index >= 0) notifyItemChanged(index);
    }

    void remove(AlbumEntry entry) {
        int index = entries.indexOf(entry);
        if (index < 0) return;
        entries.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_team, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(entries.get(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView name;
        private final TextView progress;
        private final ImageView[] seals;
        private final TextView next;
        private final TextView claim;

        Holder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.team_image);
            name = itemView.findViewById(R.id.team_name);
            progress = itemView.findViewById(R.id.team_progress);
            seals = new ImageView[]{
                    itemView.findViewById(R.id.team_seal_0),
                    itemView.findViewById(R.id.team_seal_1),
                    itemView.findViewById(R.id.team_seal_2)};
            next = itemView.findViewById(R.id.team_next);
            claim = itemView.findViewById(R.id.team_claim);
        }

        void bind(AlbumEntry entry) {
            Context context = itemView.getContext();
            itemView.setOnClickListener(v -> listener.onEntryClick(entry));

            if (!entry.isReady()) {
                name.setText(entry.failed ? R.string.album_team_failed : R.string.album_team_loading);
                name.setTextColor(context.getColor(entry.failed ? R.color.red : R.color.text_secondary));
                progress.setText("");
                next.setText("");
                claim.setVisibility(View.GONE);
                bindSeals(context, 0, 0);
                image.setImageResource(R.drawable.placeholder_character);
                itemView.setContentDescription(name.getText());
                return;
            }

            int owned = TeamAlbum.countOwned(state, entry.memberIds);
            int reached = TeamAlbum.milestonesReached(owned);
            int claimed = state == null ? 0 : state.getClaimedMilestones(entry.teamId);

            name.setText(entry.team.getName());
            name.setTextColor(context.getColor(R.color.text_primary));
            int total = entry.team.getMemberCount();
            progress.setText(context.getResources().getQuantityString(
                    R.plurals.album_team_progress, total, owned, total));
            int nextGoal = TeamAlbum.nextMilestone(reached);
            next.setText(nextGoal < 0
                    ? context.getString(R.string.album_all_seals)
                    : context.getString(R.string.album_next_seal, nextGoal));
            claim.setVisibility(reached > claimed ? View.VISIBLE : View.GONE);
            bindSeals(context, claimed, reached);

            itemView.setContentDescription(name.getText() + ". " + progress.getText() + ". "
                    + next.getText() + (reached > claimed ? ". " + claim.getText() : ""));

            Images.load(image, entry.team.getCardImageUrl());
        }

        private void bindSeals(Context context, int claimed, int reached) {
            for (int i = 0; i < seals.length; i++) {
                boolean earned = i < claimed;
                boolean ready = !earned && i < reached;
                seals[i].setColorFilter(earned || ready
                        ? TeamUi.sealColor(context, i) : context.getColor(R.color.icon_inactive));
                seals[i].setAlpha(ready ? UiTokens.ALPHA_LOCKED : 1f);
            }
        }
    }
}
