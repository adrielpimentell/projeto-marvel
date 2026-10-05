package com.example.marvel.ui.album;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.ui.common.UiTokens;
import com.example.marvel.ui.common.Images;
import com.example.marvel.R;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;
import com.example.marvel.data.model.Team;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.TeamAlbum;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.TeamUi;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TeamPageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Listener {
        void onClaim();

        void onMemberClick(Character character);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SECTION = 1;
    private static final int TYPE_MEMBER = 2;

    private static final ColorMatrixColorFilter GRAYSCALE = grayscale();

    private static final class Row {
        final int type;
        final NamedRef member;
        final boolean owned;
        final boolean ownedSection;

        Row(int type, NamedRef member, boolean owned, boolean ownedSection) {
            this.type = type;
            this.member = member;
            this.owned = owned;
            this.ownedSection = ownedSection;
        }
    }

    private final Listener listener;
    private final List<Row> rows = new ArrayList<>();
    private final Map<Integer, Character> characters = new HashMap<>();
    private Team team;
    private PlayerState state;
    private int ownedCount;
    private int missingCount;
    private int popFrom = -1;
    private int popTo = -1;

    TeamPageAdapter(Listener listener) {
        this.listener = listener;
    }

    void setData(Team team, PlayerState state, List<NamedRef> owned, List<NamedRef> missing) {
        this.team = team;
        this.state = state;
        this.ownedCount = owned.size();
        this.missingCount = missing.size();
        rows.clear();
        rows.add(new Row(TYPE_HEADER, null, false, false));
        rows.add(new Row(TYPE_SECTION, null, false, true));
        for (NamedRef member : owned) rows.add(new Row(TYPE_MEMBER, member, true, true));
        if (!missing.isEmpty()) {
            rows.add(new Row(TYPE_SECTION, null, false, false));
            for (NamedRef member : missing) rows.add(new Row(TYPE_MEMBER, member, false, false));
        }
        notifyDataSetChanged();
    }

    void setCharacters(List<Character> loaded) {
        for (Character character : loaded) {
            if (character != null) characters.put(character.getId(), character);
        }
        notifyItemRangeChanged(0, rows.size());
    }

    void popSeals(int from, int to) {
        popFrom = from;
        popTo = to;
        if (!rows.isEmpty()) notifyItemChanged(0);
    }

    boolean isFullWidth(int position) {
        return position < rows.size() && rows.get(position).type != TYPE_MEMBER;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_team_header, parent, false));
        }
        if (viewType == TYPE_SECTION) {
            return new SectionHolder(inflater.inflate(R.layout.item_team_section, parent, false));
        }
        return new MemberHolder(inflater.inflate(R.layout.item_team_member, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) ((HeaderHolder) holder).bind();
        else if (holder instanceof SectionHolder) ((SectionHolder) holder).bind(row);
        else ((MemberHolder) holder).bind(row);
    }

    class HeaderHolder extends RecyclerView.ViewHolder {
        private final ImageView banner;
        private final TextView title;
        private final TextView deck;
        private final TextView ownedText;
        private final View[] milestones;
        private final MaterialButton claim;

        HeaderHolder(View itemView) {
            super(itemView);
            banner = itemView.findViewById(R.id.team_banner);
            title = itemView.findViewById(R.id.team_title);
            deck = itemView.findViewById(R.id.team_deck);
            ownedText = itemView.findViewById(R.id.team_owned_count);
            milestones = new View[]{
                    itemView.findViewById(R.id.team_milestone_0),
                    itemView.findViewById(R.id.team_milestone_1),
                    itemView.findViewById(R.id.team_milestone_2)};
            claim = itemView.findViewById(R.id.team_claim);
            claim.setOnClickListener(v -> listener.onClaim());
        }

        void bind() {
            Context context = itemView.getContext();
            title.setText(team.getName());
            deck.setText(team.getDeck());
            deck.setVisibility(team.getDeck().isEmpty() ? View.GONE : View.VISIBLE);
            int total = team.getMemberCount();
            ownedText.setText(context.getResources().getQuantityString(
                    R.plurals.album_team_progress, total, ownedCount, total));

            int reached = TeamAlbum.milestonesReached(ownedCount);
            int claimed = state.getClaimedMilestones(team.getId());
            for (int i = 0; i < milestones.length; i++) {
                bindMilestone(context, milestones[i], i, claimed, reached);
            }
            bindClaimButton(context, claimed, reached);

            String url = team.getLargeImageUrl() != null ? team.getLargeImageUrl() : team.getCardImageUrl();
            Images.load(banner, url);
        }

        private void bindMilestone(Context context, View view, int index, int claimed, int reached) {
            ImageView seal = view.findViewById(R.id.milestone_seal);
            TextView name = view.findViewById(R.id.milestone_name);
            TextView goal = view.findViewById(R.id.milestone_goal);
            TextView coins = view.findViewById(R.id.milestone_coins);

            boolean earned = index < claimed;
            boolean ready = !earned && index < reached;
            int color = TeamUi.sealColor(context, index);
            seal.setColorFilter(earned || ready ? color : context.getColor(R.color.icon_inactive));
            seal.setAlpha(ready ? UiTokens.ALPHA_LOCKED : 1f);
            name.setText(TeamUi.sealName(context, index));
            name.setTextColor(earned ? color : context.getColor(R.color.text_primary));
            int needed = TeamAlbum.milestone(index);
            goal.setText(context.getResources().getQuantityString(R.plurals.team_members, needed, needed));
            coins.setText(context.getString(R.string.team_seal_coins,
                    PlayerHud.format(TeamAlbum.milestoneCoins(index))));
            coins.setTextColor(context.getColor(earned ? R.color.text_secondary : R.color.text_muted));

            int stateRes = earned ? R.string.seal_state_earned
                    : ready ? R.string.seal_state_ready : R.string.seal_state_locked;
            view.setContentDescription(context.getString(R.string.seal_description,
                    TeamUi.sealName(context, index), context.getString(stateRes))
                    + ". " + goal.getText() + ". " + coins.getText());

            if (index >= popFrom && index < popTo) pop(seal);
            if (index == milestones.length - 1) popFrom = popTo = -1;
        }

        private void pop(View seal) {
            seal.setScaleX(0.2f);
            seal.setScaleY(0.2f);
            seal.setRotation(-90f);
            seal.animate()
                    .scaleX(1f).scaleY(1f).rotation(0f)
                    .setDuration(UiTokens.DURATION_STAMP_MS)
                    .setStartDelay(0)
                    .setInterpolator(new OvershootInterpolator(UiTokens.STAMP_OVERSHOOT))
                    .start();
        }

        private void bindClaimButton(Context context, int claimed, int reached) {
            int pending = TeamAlbum.pendingCoins(claimed, reached);
            claim.setEnabled(pending > 0);
            if (pending > 0) {
                claim.setText(context.getString(R.string.team_claim_button, PlayerHud.format(pending)));
                claim.setIconResource(R.drawable.ic_coin);
            } else if (reached < TeamAlbum.milestoneCount()) {
                int missing = TeamAlbum.milestone(reached) - ownedCount;
                claim.setText(context.getResources().getQuantityString(R.plurals.team_claim_locked,
                        missing, missing, TeamUi.sealName(context, reached)));
                claim.setIcon(null);
            } else {
                claim.setText(R.string.team_claim_done);
                claim.setIcon(null);
            }
        }
    }

    class SectionHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView hint;

        SectionHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
            hint = itemView.findViewById(R.id.section_hint);
        }

        void bind(Row row) {
            Context context = itemView.getContext();
            if (row.ownedSection) {
                title.setText(context.getString(R.string.team_owned_section, ownedCount));
                hint.setText(R.string.team_owned_empty);
                hint.setVisibility(ownedCount == 0 ? View.VISIBLE : View.GONE);
            } else {
                title.setText(context.getString(R.string.team_missing_section, missingCount));
                hint.setText(R.string.team_missing_hint);
                hint.setVisibility(View.VISIBLE);
            }
        }
    }

    class MemberHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final ImageView lock;
        private final TextView name;

        MemberHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.member_image);
            lock = itemView.findViewById(R.id.member_lock);
            name = itemView.findViewById(R.id.member_name);
        }

        void bind(Row row) {
            Context context = itemView.getContext();
            Character character = characters.get(row.member.getId());
            String memberName = character != null ? character.getName() : row.member.getName();
            name.setText(memberName);
            name.setTextColor(context.getColor(row.owned ? R.color.text_primary : R.color.text_secondary));
            lock.setVisibility(row.owned ? View.GONE : View.VISIBLE);
            image.setAlpha(row.owned ? 1f : UiTokens.ALPHA_MISSING);
            if (row.owned) image.clearColorFilter();
            else image.setColorFilter(GRAYSCALE);
            itemView.setContentDescription(context.getString(
                    row.owned ? R.string.team_member_owned : R.string.team_member_missing, memberName));

            itemView.setClickable(character != null);
            itemView.setOnClickListener(character == null ? null : v -> listener.onMemberClick(character));

            Images.load(image, character == null ? null : character.getCardImageUrl());
        }
    }

    private static ColorMatrixColorFilter grayscale() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        return new ColorMatrixColorFilter(matrix);
    }
}
