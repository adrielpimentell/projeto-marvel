package com.example.marvel.ui.ranking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.example.marvel.data.season.RankingEntry;

import java.util.List;

final class RankingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private RankingLayout layout = RankingLayout.LOADING;

    void submit(List<RankingEntry> entries, String myUid) {
        replace(RankingLayout.of(entries, myUid));
    }

    void clear() {
        replace(RankingLayout.LOADING);
    }

    private void replace(RankingLayout next) {
        int oldCount = layout.itemCount();
        layout = next;
        notifyItemRangeRemoved(0, oldCount);
        notifyItemRangeInserted(0, layout.itemCount());
    }

    boolean hasContent() {
        return layout.isLoaded();
    }

    int adapterPositionOf(String uid) {
        int position = layout.positionOf(uid);
        return position == RankingLayout.NONE ? RecyclerView.NO_POSITION : position;
    }

    @Override
    public int getItemCount() {
        return layout.itemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return layout.typeAt(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int resource = viewType == RankingLayout.TYPE_PODIUM ? R.layout.item_ranking_podium
                : viewType == RankingLayout.TYPE_MESSAGE ? R.layout.item_ranking_message
                : R.layout.item_ranking_pill;
        return new Holder(inflater.inflate(resource, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = layout.typeAt(position);
        if (type == RankingLayout.TYPE_PODIUM) {
            bindSpot(holder.itemView.findViewById(R.id.podium_first), 1);
            bindSpot(holder.itemView.findViewById(R.id.podium_second), 2);
            bindSpot(holder.itemView.findViewById(R.id.podium_third), 3);
        } else if (type == RankingLayout.TYPE_ROW) {
            RankingEntry entry = layout.rowAt(position);
            RankingViews.bindPill(holder.itemView, entry, layout.isMe(entry));
        }
    }

    private void bindSpot(View spot, int place) {
        RankingEntry entry = layout.podium(place);
        RankingViews.bindSpot(spot, entry, place, layout.isMe(entry));
    }

    static final class Holder extends RecyclerView.ViewHolder {

        Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
