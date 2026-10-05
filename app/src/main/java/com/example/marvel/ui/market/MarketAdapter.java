package com.example.marvel.ui.market;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.Images;
import com.example.marvel.R;
import com.example.marvel.ui.common.Families;
import com.example.marvel.ui.common.ListFooter;
import com.example.marvel.ui.common.TeamUi;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MarketAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onBuyClick(MarketItem item);

        void onItemClick(MarketItem item);

        void onRetryItem(MarketItem item);

        void onFooterAction();
    }

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;
    private static final int TYPE_NOTE = 2;

    private final Listener listener;
    private final List<MarketItem> all = new ArrayList<>();
    private final List<MarketItem> items = new ArrayList<>();
    private int familyId;
    private String emptyNote = "";
    private int coins;
    private final ListFooter footer = new ListFooter(R.layout.view_skeleton_list);

    MarketAdapter(Listener listener) {
        this.listener = listener;
    }

    boolean isEmpty() {
        return all.isEmpty();
    }

    List<MarketItem> getAll() {
        return Collections.unmodifiableList(all);
    }

    void addItems(List<MarketItem> newItems) {
        all.addAll(newItems);
        if (familyId == 0) {
            int start = noteCount() + items.size();
            items.addAll(newItems);
            notifyItemRangeInserted(start, newItems.size());
        } else {
            refilter();
        }
    }

    void clear() {
        all.clear();
        items.clear();
        footer.reset();
        notifyDataSetChanged();
    }

    void remove(MarketItem item) {
        all.remove(item);
        int index = items.indexOf(item);
        if (index < 0) return;
        if (familyId != 0) {
            refilter();
            return;
        }
        items.remove(index);
        notifyItemRemoved(index);
    }

    void itemChanged(MarketItem item) {
        if (familyId != 0) {
            refilter();
            return;
        }
        int index = items.indexOf(item);
        if (index >= 0) notifyItemChanged(index);
    }

    void setFamilyFilter(int familyId, String emptyNote) {
        this.familyId = familyId;
        this.emptyNote = emptyNote;
        refilter();
    }

    private void refilter() {
        items.clear();
        for (MarketItem item : all) {
            if (familyId == 0 || (item.isReady()
                    && Families.contains(item.character.getTeams(), familyId))) {
                items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private int noteCount() {
        return familyId != 0 && items.isEmpty() && !all.isEmpty() ? 1 : 0;
    }

    void setCoins(int coins) {
        this.coins = coins;
        notifyItemRangeChanged(noteCount(), items.size());
    }

    void showFooterSkeleton() {
        footer.showSkeleton(this, noteCount() + items.size());
    }

    void showFooterLoading() {
        footer.showLoading(this, noteCount() + items.size());
    }

    void showFooterMessage(String message, String action) {
        footer.showMessage(this, noteCount() + items.size(), message, action);
    }

    void hideFooter() {
        footer.hide(this, noteCount() + items.size());
    }

    @Override
    public int getItemCount() {
        return noteCount() + items.size() + footer.count();
    }

    @Override
    public int getItemViewType(int position) {
        int notes = noteCount();
        if (position < notes) return TYPE_NOTE;
        return position - notes == items.size() ? TYPE_FOOTER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            return footer.createHolder(parent);
        }
        if (viewType == TYPE_NOTE) {
            return new NoteHolder(inflater.inflate(R.layout.item_list_note, parent, false));
        }
        return new ItemHolder(inflater.inflate(R.layout.item_market_hero, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ListFooter.Holder) {
            footer.bind((ListFooter.Holder) holder, listener::onFooterAction);
        } else if (holder instanceof NoteHolder) {
            ((TextView) holder.itemView).setText(emptyNote);
        } else {
            ((ItemHolder) holder).bind(items.get(position - noteCount()));
        }
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView name;
        private final TextView realName;
        private final TextView teams;
        private final TextView overall;
        private final TextView price;
        private final MaterialButton buy;

        ItemHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.market_image);
            name = itemView.findViewById(R.id.market_name);
            realName = itemView.findViewById(R.id.market_real_name);
            teams = itemView.findViewById(R.id.market_teams);
            overall = itemView.findViewById(R.id.market_overall);
            price = itemView.findViewById(R.id.market_price);
            buy = itemView.findViewById(R.id.market_buy);
        }

        void bind(MarketItem item) {
            Resources res = itemView.getResources();
            name.setText(item.character.getName());

            if (item.failed) {
                realName.setText(R.string.market_unavailable);
                realName.setTextColor(itemView.getContext().getColor(R.color.red));
            } else {
                String real = item.character.getRealName();
                realName.setText(real.isEmpty() ? res.getString(R.string.real_name_unknown) : real);
                realName.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            }

            if (item.isReady()) {
                overall.setText(res.getString(R.string.market_overall, item.attributes.getOverall()));
                price.setText(PlayerHud.format(item.price));
                teams.setText(TeamUi.marketLine(itemView.getContext(), item.character.getTeams()));
            } else {
                overall.setText(R.string.market_overall_loading);
                price.setText(R.string.market_price_loading);
                teams.setText(item.failed ? "" : res.getString(R.string.market_teams_loading));
            }

            buy.setEnabled(item.isReady() && coins >= item.price);
            buy.setOnClickListener(v -> listener.onBuyClick(item));
            itemView.setOnClickListener(v -> {
                if (item.failed) listener.onRetryItem(item);
                else listener.onItemClick(item);
            });

            Images.load(image, item.character.getCardImageUrl());
        }
    }

    static class NoteHolder extends RecyclerView.ViewHolder {
        NoteHolder(View itemView) {
            super(itemView);
        }
    }
}
