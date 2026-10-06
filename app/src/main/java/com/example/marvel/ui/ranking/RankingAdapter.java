package com.example.marvel.ui.ranking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.example.marvel.data.season.RankingEntry;

import java.util.ArrayList;
import java.util.List;

final class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.Holder> {

    private final List<RankingEntry> entries = new ArrayList<>();
    private String myUid;

    void submit(List<RankingEntry> newEntries, String myUid) {
        this.myUid = myUid;
        int oldSize = entries.size();
        entries.clear();
        notifyItemRangeRemoved(0, oldSize);
        entries.addAll(newEntries);
        notifyItemRangeInserted(0, entries.size());
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ranking_row, parent, false);
        return new Holder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RankingEntry entry = entries.get(position);
        RankingRows.bind(holder.itemView, entry, entry.getUid().equals(myUid));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
