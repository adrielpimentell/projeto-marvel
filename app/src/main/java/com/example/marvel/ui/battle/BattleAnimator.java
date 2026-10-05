package com.example.marvel.ui.battle;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.marvel.R;
import com.example.marvel.game.BattleResult;
import com.example.marvel.game.BattleTurn;
import com.example.marvel.game.GameBalance;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

final class BattleAnimator {

    interface Listener {
        void onFinished();
    }

    static final class Fighter {
        final View block;
        final TextView name;
        final TextView tag;
        final LinearProgressIndicator hpBar;
        final TextView hpText;
        final TextView damage;
        final TextView effect;
        final ImageView shield;
        final ImageView poison;
        final ImageView stun;
        final ImageView blockIcon;

        Fighter(View block) {
            this.block = block;
            name = block.findViewById(R.id.fighter_name);
            tag = block.findViewById(R.id.fighter_tag);
            hpBar = block.findViewById(R.id.fighter_hp_bar);
            hpText = block.findViewById(R.id.fighter_hp_text);
            damage = block.findViewById(R.id.fighter_damage);
            effect = block.findViewById(R.id.fighter_effect);
            shield = block.findViewById(R.id.fighter_status_shield);
            poison = block.findViewById(R.id.fighter_status_poison);
            stun = block.findViewById(R.id.fighter_status_stun);
            blockIcon = block.findViewById(R.id.fighter_block_icon);
        }
    }

    private final Context context;
    private final Fighter hero;
    private final Fighter enemy;
    private final TextView message;
    private final TextView turnLabel;
    private final TextView log;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ObjectAnimator> running = new ArrayList<>();
    private final float lungeDistance;
    private final float shakeDistance;

    private Listener listener;
    private boolean finished;

    BattleAnimator(Context context, Fighter hero, Fighter enemy,
                   TextView message, TextView turnLabel, TextView log) {
        this.context = context;
        this.hero = hero;
        this.enemy = enemy;
        this.message = message;
        this.turnLabel = turnLabel;
        this.log = log;
        float density = context.getResources().getDisplayMetrics().density;
        lungeDistance = 26 * density;
        shakeDistance = 12 * density;
    }

    static long turnDuration(int turns) {
        long fixed = GameBalance.BATTLE_INTRO_MS + GameBalance.BATTLE_OUTRO_MS;
        long minTotal = GameBalance.BATTLE_MIN_DURATION_MS - fixed;
        long maxTotal = GameBalance.BATTLE_MAX_DURATION_MS - fixed;
        long wanted = GameBalance.BATTLE_TURN_MS * turns;
        long total = Math.max(minTotal, Math.min(maxTotal, wanted));
        return total / Math.max(1, turns);
    }

    void start(BattleResult result, String heroName, String enemyName, Listener listener) {
        this.listener = listener;
        setup(hero, heroName, context.getString(R.string.fighter_hero_tag,
                result.getHero().getOverall()), result.getHeroMaxHp(), result.getHeroStartHp(), R.color.green);
        setup(enemy, enemyName, context.getString(R.string.fighter_enemy_tag,
                result.getEnemy().getOverall()), result.getEnemyMaxHp(), result.getEnemyMaxHp(), R.color.red);
        hero.shield.setVisibility(result.isHeroShieldAtStart() ? View.VISIBLE : View.GONE);

        turnLabel.setText("");
        log.setText("");
        showMessage(context.getString(R.string.arena_ready));
        handler.postDelayed(() -> showMessage(context.getString(R.string.arena_fight)),
                GameBalance.BATTLE_INTRO_MS / 2);

        List<BattleTurn> turns = result.getTurns();
        long perTurn = turnDuration(turns.size());
        long time = GameBalance.BATTLE_INTRO_MS;
        for (int i = 0; i < turns.size(); i++) {
            final int number = i + 1;
            final BattleTurn turn = turns.get(i);
            handler.postDelayed(() -> playTurn(number, turn, heroName, enemyName, perTurn), time);
            time += perTurn;
        }

        handler.postDelayed(() -> showMessage(context.getString(R.string.arena_ko)), time);
        handler.postDelayed(this::finish, time + GameBalance.BATTLE_OUTRO_MS);
    }

    void skip() {
        finish();
    }

    void cancel() {
        finished = true;
        stopAll();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        stopAll();
        if (listener != null) listener.onFinished();
    }

    private void stopAll() {
        handler.removeCallbacksAndMessages(null);
        for (Fighter f : new Fighter[]{hero, enemy}) {
            f.block.animate().cancel();
            f.damage.animate().cancel();
            f.effect.animate().cancel();
            f.blockIcon.animate().cancel();
        }
        for (ObjectAnimator animator : running) animator.cancel();
        running.clear();
        message.animate().cancel();
    }

    private void setup(Fighter f, String name, String tag, int maxHp, int startHp, int colorRes) {
        f.name.setText(name);
        f.tag.setText(tag);
        f.hpBar.setMax(maxHp);
        f.hpBar.setProgressCompat(startHp, false);
        f.hpBar.setIndicatorColor(context.getColor(colorRes));
        f.hpText.setText(context.getString(R.string.hp_format, startHp, maxHp));
        f.damage.setAlpha(0f);
        f.effect.setAlpha(0f);
        f.blockIcon.setAlpha(0f);
        f.shield.setVisibility(View.GONE);
        f.poison.setVisibility(View.GONE);
        f.stun.setVisibility(View.GONE);
        f.block.setTranslationX(0f);
        f.block.setTranslationY(0f);
        f.block.setRotation(0f);
    }

    private void playTurn(int number, BattleTurn turn, String heroName, String enemyName, long duration) {
        Fighter actor = turn.isHeroAttacking() ? hero : enemy;
        Fighter target = turn.isHeroAttacking() ? enemy : hero;
        String actorName = turn.isHeroAttacking() ? heroName : enemyName;
        String targetName = turn.isHeroAttacking() ? enemyName : heroName;
        List<String> lines = new ArrayList<>();

        turnLabel.setText(context.getString(R.string.arena_turn, number));
        showMessage(context.getString(R.string.battle_vs));

        long t = 0;
        if (turn.getRegen() > 0 || turn.getPoisonTick() > 0) {
            if (turn.getRegen() > 0) {
                popEffect(actor, context.getString(R.string.effect_heal_format, turn.getRegen()), R.color.effect_green);
                lines.add(context.getString(R.string.arena_regen, actorName, turn.getRegen()));
            }
            if (turn.getPoisonTick() > 0) {
                popEffect(actor, context.getString(R.string.damage_format, turn.getPoisonTick()), R.color.effect_green);
                lines.add(context.getString(R.string.arena_poison_tick, actorName, turn.getPoisonTick()));
            }
            setHp(actor, turn.getActorHpAfterStart());
            showLog(lines);
            t = (long) (duration * 0.3);
        }
        if (turn.getActorHpAfterStart() == 0) {
            later(t, () -> updateStatuses(turn));
            return;
        }

        if (turn.isSkipped()) {
            later(t, () -> {
                wobble(actor, (long) (duration * 0.4));
                popLabel(actor.damage, context.getString(R.string.arena_stunned_label), R.color.effect_stun, 1f);
                lines.add(context.getString(R.string.arena_stunned, actorName));
                showLog(lines);
                updateStatuses(turn);
            });
            return;
        }

        long lunge = (long) (duration * 0.18);
        float direction = turn.isHeroAttacking() ? -1f : 1f;
        later(t, () -> actor.block.animate()
                .translationY(direction * lungeDistance)
                .setDuration(lunge)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> actor.block.animate().translationY(0f).setDuration(lunge).start())
                .start());

        later(t + lunge, () -> {
            if (turn.isDodged()) {
                dodge(target, (long) (duration * 0.3));
                popLabel(target.damage, context.getString(R.string.arena_dodge_label), R.color.text_muted, 1f);
                lines.add(context.getString(R.string.arena_dodge, targetName));
            } else if (turn.isBlocked()) {
                blockPop(target);
                popLabel(target.damage, context.getString(R.string.arena_block_label), R.color.effect_shield, 1f);
                lines.add(context.getString(R.string.arena_block, targetName));
            } else {
                shake(target, (long) (duration * 0.35));
                popLabel(target.damage, context.getString(R.string.damage_format, turn.getDamage()),
                        turn.isCritical() ? R.color.red : R.color.text_primary,
                        turn.isCritical() ? 1.35f : 1f);
                setHp(target, turn.getTargetHpAfter());
                int textRes = turn.isExecute() ? R.string.arena_execute
                        : turn.isCritical() ? R.string.arena_crit : R.string.arena_hit;
                lines.add(context.getString(textRes, actorName, turn.getDamage()));
            }
            showLog(lines);
        });

        later(t + lunge + (long) (duration * 0.3), () -> {
            if (turn.getLifesteal() > 0) {
                popEffect(actor, context.getString(R.string.effect_heal_format, turn.getLifesteal()),
                        R.color.effect_green);
                lines.add(context.getString(R.string.arena_lifesteal, turn.getLifesteal()));
            }
            if (turn.getThorns() > 0) {
                popEffect(actor, context.getString(R.string.damage_format, turn.getThorns()), R.color.effect_thorns);
                shake(actor, (long) (duration * 0.2));
                lines.add(context.getString(R.string.arena_thorns, actorName, turn.getThorns()));
            }
            if (turn.isPoisonApplied()) lines.add(context.getString(R.string.arena_poisoned, targetName));
            if (turn.isStunApplied()) lines.add(context.getString(R.string.arena_stun_applied, targetName));
            setHp(hero, turn.getHeroHpEnd());
            setHp(enemy, turn.getEnemyHpEnd());
            updateStatuses(turn);
            showLog(lines);
        });
    }

    private void later(long delay, Runnable action) {
        if (delay <= 0) action.run();
        else handler.postDelayed(action, delay);
    }

    private void setHp(Fighter f, int hp) {
        f.hpBar.setProgressCompat(hp, true);
        f.hpText.setText(context.getString(R.string.hp_format, hp, f.hpBar.getMax()));
    }

    private void updateStatuses(BattleTurn turn) {
        hero.shield.setVisibility(turn.isHeroShieldReady() ? View.VISIBLE : View.GONE);
        enemy.shield.setVisibility(turn.isEnemyShieldReady() ? View.VISIBLE : View.GONE);
        hero.poison.setVisibility(turn.isHeroPoisoned() ? View.VISIBLE : View.GONE);
        enemy.poison.setVisibility(turn.isEnemyPoisoned() ? View.VISIBLE : View.GONE);
        hero.stun.setVisibility(turn.isHeroStunned() ? View.VISIBLE : View.GONE);
        enemy.stun.setVisibility(turn.isEnemyStunned() ? View.VISIBLE : View.GONE);
    }

    private void shake(Fighter target, long duration) {
        start(ObjectAnimator.ofFloat(target.block, View.TRANSLATION_X,
                0f, shakeDistance, -shakeDistance, shakeDistance * 0.6f, -shakeDistance * 0.6f,
                shakeDistance * 0.3f, 0f), duration);
    }

    private void dodge(Fighter target, long duration) {
        start(ObjectAnimator.ofFloat(target.block, View.TRANSLATION_X,
                0f, shakeDistance * 3, shakeDistance * 3, 0f), duration);
    }

    private void wobble(Fighter target, long duration) {
        start(ObjectAnimator.ofFloat(target.block, View.ROTATION, 0f, -3f, 3f, -2f, 2f, 0f), duration);
    }

    private void blockPop(Fighter target) {
        ImageView icon = target.blockIcon;
        icon.animate().cancel();
        icon.setAlpha(0f);
        icon.setScaleX(0.4f);
        icon.setScaleY(0.4f);
        icon.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setStartDelay(0).setDuration(200)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> icon.animate().alpha(0f).setStartDelay(300).setDuration(250).start())
                .start();
    }

    private void start(ObjectAnimator animator, long duration) {
        animator.setDuration(duration);
        animator.start();
        running.add(animator);
        if (running.size() > 16) running.remove(0);
    }

    private void popEffect(Fighter f, String text, int colorRes) {
        popLabel(f.effect, text, colorRes, 1f);
    }

    private void popLabel(TextView label, String text, int colorRes, float scale) {
        label.animate().cancel();
        label.setText(text);
        label.setTextColor(context.getColor(colorRes));
        label.setAlpha(0f);
        label.setTranslationY(0f);
        label.setScaleX(0.6f);
        label.setScaleY(0.6f);
        label.animate()
                .alpha(1f).scaleX(scale).scaleY(scale).translationY(-label.getHeight() * 0.4f)
                .setStartDelay(0)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> label.animate().alpha(0f).setStartDelay(350).setDuration(250).start())
                .start();
    }

    private void showLog(List<String> lines) {
        int from = Math.max(0, lines.size() - 2);
        log.setText(String.join("\n", lines.subList(from, lines.size())));
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setScaleX(0.7f);
        message.setScaleY(0.7f);
        message.animate().scaleX(1f).scaleY(1f).setDuration(250)
                .setInterpolator(new OvershootInterpolator()).start();
    }
}
