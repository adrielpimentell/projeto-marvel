package com.example.marvel.ui.list;

import android.content.res.Resources;
import android.icu.text.CompactDecimalFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.Images;
import com.example.marvel.ui.common.ListFooter;
import com.example.marvel.R;
import com.example.marvel.data.model.Character;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CharacterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onCharacterClick(Character character);

        void onFooterActionClick();
    }

    private static final int TYPE_CHARACTER = 0;
    private static final int TYPE_FOOTER = 1;

    private static final CompactDecimalFormat COMPACT_NUMBER = CompactDecimalFormat.getInstance(
            Locale.forLanguageTag("pt-BR"), CompactDecimalFormat.CompactStyle.SHORT);

    private final Listener listener;
    private final List<Character> characters = new ArrayList<>();
    private final Set<Integer> ids = new HashSet<>();

    private final ListFooter footer = new ListFooter(R.layout.view_skeleton_grid);

    public CharacterAdapter(Listener listener) {
        this.listener = listener;
    }

    public void clear() {
        characters.clear();
        ids.clear();
        footer.reset();
        notifyDataSetChanged();
    }

    public int addCharacters(List<Character> newCharacters) {
        int start = characters.size();
        for (Character character : newCharacters) {
            if (ids.add(character.getId())) {
                characters.add(character);
            }
        }
        int added = characters.size() - start;
        if (added > 0) {
            notifyItemRangeInserted(start, added);
        }
        return added;
    }

    public int getCharacterCount() {
        return characters.size();
    }

    public void showFooterSkeleton() {
        footer.showSkeleton(this, characters.size());
    }

    public void showFooterLoading() {
        footer.showLoading(this, characters.size());
    }

    public void showFooterMessage(String message, String actionText) {
        footer.showMessage(this, characters.size(), message, actionText);
    }

    public void hideFooter() {
        footer.hide(this, characters.size());
    }

    public boolean isFooter(int position) {
        return footer.isVisible() && position == characters.size();
    }

    @Override
    public int getItemCount() {
        return characters.size() + footer.count();
    }

    @Override
    public int getItemViewType(int position) {
        return isFooter(position) ? TYPE_FOOTER : TYPE_CHARACTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            return footer.createHolder(parent);
        }
        return new CharacterHolder(inflater.inflate(R.layout.item_character, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ListFooter.Holder) {
            footer.bind((ListFooter.Holder) holder, listener::onFooterActionClick);
        } else {
            ((CharacterHolder) holder).bind(characters.get(position));
        }
    }

    class CharacterHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView name;
        private final TextView realName;
        private final TextView issueBadge;

        CharacterHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.character_image);
            name = itemView.findViewById(R.id.character_name);
            realName = itemView.findViewById(R.id.character_real_name);
            issueBadge = itemView.findViewById(R.id.issue_badge);
        }

        void bind(Character character) {
            Resources res = itemView.getResources();

            name.setText(character.getName());
            String real = character.getRealName();
            realName.setText(real.isEmpty() ? res.getString(R.string.real_name_unknown) : real);

            int issues = character.getIssueAppearances();
            if (issues > 0) {
                issueBadge.setVisibility(View.VISIBLE);
                issueBadge.setText(res.getQuantityString(
                        R.plurals.issue_count, issues, COMPACT_NUMBER.format(issues)));
            } else {
                issueBadge.setVisibility(View.GONE);
            }

            Images.loadWithFade(image, character.getCardImageUrl());

            itemView.setContentDescription(character.getName());
            itemView.setOnClickListener(v -> listener.onCharacterClick(character));
        }
    }
}
