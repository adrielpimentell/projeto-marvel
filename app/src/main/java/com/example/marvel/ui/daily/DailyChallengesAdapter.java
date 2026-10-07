package com.example.marvel.ui.daily;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.example.marvel.game.DailyChallenge;
import com.example.marvel.game.DailyChallenges;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.SurvivalRun;
import com.example.marvel.ui.common.PlayerHud;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class DailyChallengesAdapter extends RecyclerView.Adapter<DailyChallengesAdapter.Holder> {

    public interface Listener {
        void onOpenDailyChest();

        void onOpenSurvival();
    }

    private static final Object PAYLOAD_COUNTDOWN = new Object();

    private final Listener listener;
    private PlayerState state;
    private boolean visible = true;
    private String countdown = "";

    public DailyChallengesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void bind(PlayerState newState) {
        int before = getItemCount();
        state = newState;
        notifyCountChange(before);
    }

    public void setVisible(boolean show) {
        if (show == visible) return;
        int before = getItemCount();
        visible = show;
        notifyCountChange(before);
    }

    public void setCountdown(String text) {
        countdown = text;
        if (getItemCount() == 1) notifyItemChanged(0, PAYLOAD_COUNTDOWN);
    }

    private void notifyCountChange(int before) {
        int after = getItemCount();
        if (before == 0 && after == 1) notifyItemInserted(0);
        else if (before == 1 && after == 0) notifyItemRemoved(0);
        else if (after == 1) notifyItemChanged(0);
    }

    @Override
    public int getItemCount() {
        return visible && state != null && state.getDaily() != null ? 1 : 0;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_daily_challenges, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind();
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_COUNTDOWN)) {
            holder.countdownText.setText(countdown);
        } else {
            holder.bind();
        }
    }

    class Holder extends RecyclerView.ViewHolder {
        private final TextView countdownText;
        private final View[] rows;
        private final ImageView chestIcon;
        private final TextView chestText;
        private final View chestButton;
        private final TextView survivalStatus;
        private final TextView survivalAction;

        Holder(View itemView) {
            super(itemView);
            countdownText = itemView.findViewById(R.id.daily_countdown);
            rows = new View[]{
                    itemView.findViewById(R.id.daily_row_0),
                    itemView.findViewById(R.id.daily_row_1),
                    itemView.findViewById(R.id.daily_row_2)};
            chestIcon = itemView.findViewById(R.id.daily_chest_icon);
            chestText = itemView.findViewById(R.id.daily_chest_text);
            chestButton = itemView.findViewById(R.id.daily_chest_open);
            survivalStatus = itemView.findViewById(R.id.home_survival_status);
            survivalAction = itemView.findViewById(R.id.home_survival_action);
            itemView.findViewById(R.id.home_survival).setOnClickListener(v -> listener.onOpenSurvival());
            chestButton.setOnClickListener(v -> listener.onOpenDailyChest());
        }

        void bind() {
            Context context = itemView.getContext();
            DailyChallenges daily = state.getDaily();
            countdownText.setText(countdown);
            List<DailyChallenge> challenges = daily.getChallenges();
            for (int i = 0; i < rows.length; i++) {
                rows[i].setVisibility(i < challenges.size() ? View.VISIBLE : View.GONE);
                if (i < challenges.size()) bindRow(context, rows[i], challenges.get(i));
            }

            int chests = state.getDailyChestsToOpen();
            chestButton.setVisibility(chests > 0 ? View.VISIBLE : View.GONE);
            chestIcon.setColorFilter(context.getColor(chests > 0 ? R.color.red : R.color.icon_inactive));
            if (chests > 0) {
                chestText.setText(context.getResources().getQuantityString(
                        R.plurals.daily_chest_ready, chests, chests));
            } else if (daily.isChestEarned()) {
                chestText.setText(R.string.daily_chest_done);
            } else {
                chestText.setText(context.getString(R.string.daily_chest_locked, daily.countComplete()));
            }
            bindSurvival(context);
        }

        private void bindSurvival(Context context) {
            SurvivalRun run = state.getActiveSurvival();
            int best = state.getSurvivalBestFloor();
            if (run != null) {
                survivalStatus.setText(context.getString(R.string.home_survival_running, run.getFloor(), best));
                survivalAction.setText(R.string.home_survival_continue);
            } else {
                survivalStatus.setText(best > 0 ? context.getString(R.string.survival_record, best)
                        : context.getString(R.string.survival_no_record));
                survivalAction.setText(R.string.home_survival_play);
            }
        }

        private void bindRow(Context context, View row, DailyChallenge c) {
            ImageView icon = row.findViewById(R.id.daily_icon);
            TextView text = row.findViewById(R.id.daily_text);
            TextView reward = row.findViewById(R.id.daily_reward);
            TextView hint = row.findViewById(R.id.daily_hint);
            LinearProgressIndicator bar = row.findViewById(R.id.daily_bar);
            TextView count = row.findViewById(R.id.daily_count);

            boolean done = c.isComplete();
            int accent = context.getColor(done ? R.color.green : R.color.red);
            String description = DailyUi.describe(context, c);
            String coins = PlayerHud.format(c.getCoins());

            icon.setImageResource(DailyUi.iconRes(c.getType()));
            icon.setColorFilter(accent);
            text.setText(description);
            reward.setText(context.getString(R.string.daily_reward, coins));
            reward.setTextColor(accent);
            String hintText = DailyUi.hint(context, c, state);
            hint.setText(hintText);
            hint.setVisibility(hintText.isEmpty() ? View.GONE : View.VISIBLE);
            bar.setIndicatorColor(accent);
            bar.setProgressCompat(c.getProgress() * 100 / c.getTarget(), false);
            count.setText(done ? context.getString(R.string.daily_done)
                    : context.getString(R.string.daily_progress, c.getProgress(), c.getTarget()));
            count.setTextColor(context.getColor(done ? R.color.green : R.color.text_muted));
            row.setContentDescription(context.getString(R.string.daily_row_description,
                    description, c.getProgress(), c.getTarget(), coins));
        }
    }
}
