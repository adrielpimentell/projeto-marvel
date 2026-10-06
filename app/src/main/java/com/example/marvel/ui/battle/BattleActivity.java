package com.example.marvel.ui.battle;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.marvel.game.Season;
import com.example.marvel.data.season.SeasonRepository;
import com.example.marvel.ui.common.UiTokens;
import com.example.marvel.ui.common.Screens;
import com.example.marvel.ui.common.Images;
import com.example.marvel.MainActivity;
import com.example.marvel.R;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.game.AttributeCalculator;
import com.example.marvel.game.BattleEngine;
import com.example.marvel.game.BattleFacts;
import com.example.marvel.game.BattleResult;
import com.example.marvel.game.DailyChallenge;
import com.example.marvel.game.DailyClock;
import com.example.marvel.game.GameAttributes;
import com.example.marvel.game.Loadout;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Survival;
import com.example.marvel.game.SurvivalOutcome;
import com.example.marvel.game.SurvivalRun;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.StateView;
import com.example.marvel.ui.daily.DailyUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BattleActivity extends AppCompatActivity {

    private static final String EXTRA_ENEMY = "extra_enemy";
    private static final String STATE_RESULT = "state_result";
    private static final String STATE_ENEMY = "state_enemy";
    private static final String STATE_RANK_BEFORE = "state_rank_before";
    private static final String STATE_DAILY_DONE = "state_daily_done";
    private static final String EXTRA_SURVIVAL_FLOOR = "extra_survival_floor";
    private static final String STATE_SURVIVAL = "state_survival";

    private CharacterRepository repository;
    private PlayerStore playerStore;
    private RequestHandle request;
    private BattleAnimator animator;

    private Character enemy;
    private OwnedHero hero;
    private BattleResult result;
    private int rankBefore = -1;
    private ArrayList<String> dailyDone = new ArrayList<>();
    private int survivalFloor;
    private int[] survivalOutcome;

    private View root;
    private View closeButton;
    private View playerHud;
    private View bottomBar;
    private View loadingView;
    private StateView errorView;
    private View arena;
    private View resultScroll;
    private View skipButton;
    private View backHomeButton;
    private View resultIconBg;
    private ImageView resultIcon;
    private TextView resultTitle;
    private TextView resultSubtitle;

    public static Intent newIntent(Context context, Character enemy) {
        return new Intent(context, BattleActivity.class).putExtra(EXTRA_ENEMY, enemy);
    }

    public static Intent newSurvivalIntent(Context context, Character enemy, int floor) {
        return newIntent(context, enemy).putExtra(EXTRA_SURVIVAL_FLOOR, floor);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_battle);

        enemy = getIntent().getSerializableExtra(EXTRA_ENEMY, Character.class);
        if (enemy == null) {
            finish();
            return;
        }
        repository = CharacterRepository.getInstance(this);
        playerStore = PlayerStore.getInstance(this);
        survivalFloor = getIntent().getIntExtra(EXTRA_SURVIVAL_FLOOR, 0);
        hero = playerStore.get().getActiveHero();
        SurvivalRun run = playerStore.get().getSurvival();
        if (isSurvival() && run != null && playerStore.get().find(run.getHeroId()) != null) {
            hero = playerStore.get().find(run.getHeroId());
        }

        bindViews();
        applyWindowInsets();

        if (savedInstanceState != null) {
            BattleResult saved = savedInstanceState.getSerializable(STATE_RESULT, BattleResult.class);
            Character savedEnemy = savedInstanceState.getSerializable(STATE_ENEMY, Character.class);
            if (saved != null && savedEnemy != null) {
                result = saved;
                enemy = savedEnemy;
                rankBefore = savedInstanceState.getInt(STATE_RANK_BEFORE, -1);
                ArrayList<String> savedDaily = savedInstanceState.getStringArrayList(STATE_DAILY_DONE);
                if (savedDaily != null) dailyDone = savedDaily;
                survivalOutcome = savedInstanceState.getIntArray(STATE_SURVIVAL);
                showResult(false);
                return;
            }
        }
        loadEnemyAndFight();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerHud.bind(playerHud, playerStore.get());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (result != null) {
            outState.putSerializable(STATE_RESULT, result);
            outState.putSerializable(STATE_ENEMY, enemy);
            outState.putInt(STATE_RANK_BEFORE, rankBefore);
            outState.putStringArrayList(STATE_DAILY_DONE, dailyDone);
            outState.putIntArray(STATE_SURVIVAL, survivalOutcome);
        }
    }

    @Override
    protected void onDestroy() {
        if (animator != null) animator.cancel();
        if (request != null) request.cancel();
        super.onDestroy();
    }

    private void bindViews() {
        root = findViewById(R.id.battle_root);
        closeButton = findViewById(R.id.close_button);
        playerHud = findViewById(R.id.player_hud);
        bottomBar = findViewById(R.id.bottom_bar);
        loadingView = findViewById(R.id.battle_loading);
        errorView = new StateView(findViewById(R.id.battle_error));
        arena = findViewById(R.id.arena);
        resultScroll = findViewById(R.id.result_scroll);
        skipButton = findViewById(R.id.skip_button);
        backHomeButton = findViewById(R.id.back_home_button);
        resultIconBg = findViewById(R.id.result_icon_bg);
        resultIcon = findViewById(R.id.result_icon);
        resultTitle = findViewById(R.id.result_title);
        resultSubtitle = findViewById(R.id.result_subtitle);

        animator = new BattleAnimator(this,
                new BattleAnimator.Fighter(findViewById(R.id.hero_block)),
                new BattleAnimator.Fighter(findViewById(R.id.enemy_block)),
                findViewById(R.id.arena_message),
                findViewById(R.id.arena_turn),
                findViewById(R.id.arena_log));

        closeButton.setOnClickListener(v -> finish());
        skipButton.setOnClickListener(v -> animator.skip());
        backHomeButton.setOnClickListener(v -> goHome());
    }

    private void applyWindowInsets() {
        ViewGroup.MarginLayoutParams closeParams =
                (ViewGroup.MarginLayoutParams) closeButton.getLayoutParams();
        int closeTopMargin = closeParams.topMargin;
        int barBottomPadding = bottomBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            closeParams.topMargin = closeTopMargin + bars.top;
            closeButton.setLayoutParams(closeParams);
            bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(),
                    bottomBar.getPaddingRight(), barBottomPadding + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void loadEnemyAndFight() {
        loadingView.setVisibility(View.VISIBLE);
        errorView.hide();

        if (enemy.hasDetails()) {
            fight();
            return;
        }
        request = repository.loadCharacterDetail(enemy.getId(), new RepoCallback<Character>() {
            @Override
            public void onSuccess(Character detail) {
                enemy = detail;
                fight();
            }

            @Override
            public void onError(String message) {
                loadingView.setVisibility(View.GONE);
                errorView.show(R.drawable.ic_error, null, message,
                        getString(R.string.action_retry), () -> loadEnemyAndFight());
            }
        });
    }

    private void fight() {
        if (result != null) return;
        GameAttributes enemyAttributes = AttributeCalculator.calculate(enemy);
        if (isSurvival()) {
            if (!fightSurvivalFloor(enemyAttributes)) {
                finish();
                return;
            }
        } else {
            result = BattleEngine.fight(hero.getBattleAttributes(), Loadout.artifactsOf(hero),
                    enemyAttributes, new Random());

            playerStore.ensureSeason(Season.currentId());
            rankBefore = playerStore.get().getRank();
            addDailyMessages(playerStore.applyBattle(result,
                    BattleFacts.of(result, hero, enemy), DailyClock.today()));
            SeasonRepository.getInstance(this).sync();
        }

        loadingView.setVisibility(View.GONE);
        arena.setVisibility(View.VISIBLE);
        skipButton.setVisibility(View.VISIBLE);
        animator.start(result, heroName(), enemy.getName(), () -> showResult(true));
    }

    private boolean fightSurvivalFloor(GameAttributes enemyAttributes) {
        SurvivalRun run = playerStore.get().getActiveSurvival();
        if (run == null || run.getFloor() != survivalFloor || run.getEnemyId() != enemy.getId()) {
            return false;
        }
        BattleResult fought = BattleEngine.fight(hero.getBattleAttributes(), Loadout.artifactsOf(hero),
                enemyAttributes, new Random(), Survival.modifiers(run, hero));
        SurvivalOutcome outcome = playerStore.applySurvivalBattle(fought,
                BattleFacts.of(fought, hero, enemy), DailyClock.today(), survivalFloor, enemy.getId());
        if (outcome == null) return false;
        result = fought;
        survivalOutcome = new int[]{outcome.isWon() ? 1 : 0, outcome.getFloor(), outcome.getCoins(),
                outcome.isNewRecord() ? 1 : 0, outcome.isChestUnlocked() ? 1 : 0};
        addDailyMessages(outcome.getDailyCompleted());
        return true;
    }

    private void addDailyMessages(List<DailyChallenge> completed) {
        for (DailyChallenge challenge : completed) {
            dailyDone.add(getString(R.string.battle_daily_done, DailyUi.describe(this, challenge),
                    PlayerHud.format(challenge.getCoins())));
        }
    }

    private boolean isSurvival() {
        return survivalFloor > 0;
    }

    private void showResult(boolean animate) {
        loadingView.setVisibility(View.GONE);
        errorView.hide();
        arena.setVisibility(View.GONE);
        skipButton.setVisibility(View.GONE);
        resultScroll.setVisibility(View.VISIBLE);
        backHomeButton.setVisibility(View.VISIBLE);
        PlayerHud.bind(playerHud, playerStore.get());

        boolean won = result.isHeroWinner();
        int turns = result.getTurns().size();
        resultIcon.setImageResource(won ? R.drawable.ic_trophy : R.drawable.ic_close);
        resultIconBg.setBackgroundTintList(ColorStateList.valueOf(getColor(won ? R.color.green : R.color.red)));
        resultTitle.setText(won ? R.string.battle_win_title : R.string.battle_loss_title);
        resultSubtitle.setText(won
                ? getResources().getQuantityString(R.plurals.battle_subtitle, turns,
                        heroName(), enemy.getName(), turns)
                : getResources().getQuantityString(R.plurals.battle_subtitle, turns,
                        enemy.getName(), heroName(), turns));

        ((TextView) findViewById(R.id.challenger_name)).setText(heroName());
        loadImage(hero.getImageUrl(), R.id.challenger_image);
        ((TextView) findViewById(R.id.opponent_name)).setText(enemy.getName());
        loadImage(enemy.getCardImageUrl(), R.id.opponent_image);

        GameAttributes a = result.getHero();
        GameAttributes b = result.getEnemy();
        bindRow(R.id.row_life, R.string.attr_life, a.getLife(), b.getLife());
        bindRow(R.id.row_strength, R.string.attr_strength, a.getStrength(), b.getStrength());
        bindRow(R.id.row_speed, R.string.attr_speed, a.getSpeed(), b.getSpeed());
        bindRow(R.id.row_intelligence, R.string.attr_intelligence, a.getIntelligence(), b.getIntelligence());
        bindRow(R.id.row_overall, R.string.attr_overall, a.getOverall(), b.getOverall());

        if (survivalOutcome != null) {
            bindSurvivalResult();
        } else {
            bindSigned(findViewById(R.id.coins_value), result.getCoins());
            bindSigned(findViewById(R.id.trophies_value), result.getTrophies());
            PlayerState player = playerStore.get();
            ((TextView) findViewById(R.id.total_value)).setText(getString(R.string.trophies_with_rank,
                    PlayerHud.format(player.getTrophies()), PlayerHud.rankName(this, player.getRank())));
            ((TextView) findViewById(R.id.record_text)).setText(getString(R.string.battle_record,
                    player.getWins(), player.getLosses()));
            showRankChange(player.getRank());
        }
        TextView dailyText = findViewById(R.id.daily_done);
        dailyText.setText(String.join("\n", dailyDone));
        dailyText.setVisibility(dailyDone.isEmpty() ? View.GONE : View.VISIBLE);

        if (animate) {
            resultIconBg.setScaleX(0f);
            resultIconBg.setScaleY(0f);
            resultIconBg.animate().scaleX(1f).scaleY(1f).setDuration(UiTokens.DURATION_EMPHASIS_MS)
                    .setInterpolator(new OvershootInterpolator()).start();
        }
    }

    private void bindSurvivalResult() {
        boolean won = survivalOutcome[0] == 1;
        int floor = survivalOutcome[1];
        PlayerState player = playerStore.get();
        resultTitle.setText(won ? getString(R.string.survival_win_title, floor)
                : getString(R.string.survival_loss_title));
        bindSigned(findViewById(R.id.coins_value), survivalOutcome[2]);

        ((TextView) findViewById(R.id.trophies_label)).setText(R.string.survival_floor_label);
        TextView floorValue = findViewById(R.id.trophies_value);
        floorValue.setText(String.valueOf(floor));
        floorValue.setTextColor(getColor(R.color.text_primary));
        ((TextView) findViewById(R.id.total_label)).setText(R.string.survival_record_label);
        int best = player.getSurvivalBestFloor();
        ((TextView) findViewById(R.id.total_value)).setText(survivalOutcome[3] == 1
                ? getString(R.string.survival_result_new_record, best)
                : getString(R.string.survival_result_record, best));

        TextView summary = findViewById(R.id.record_text);
        SurvivalRun run = player.getSurvival();
        if (won && run != null && !run.isOver()) {
            summary.setText(getString(R.string.survival_result_hp,
                    Survival.currentHp(run, hero), Survival.maxHp(run, hero)));
        } else if (run != null) {
            summary.setText(getResources().getQuantityString(R.plurals.survival_result_over,
                    run.getFloorsWon(), run.getFloorsWon(), PlayerHud.format(run.getCoinsEarned())));
        }

        TextView badge = findViewById(R.id.rank_change);
        badge.setVisibility(survivalOutcome[4] == 1 ? View.VISIBLE : View.GONE);
        badge.setText(R.string.survival_chest_unlocked);
        ((TextView) backHomeButton).setText(R.string.action_continue);
    }

    private void showRankChange(int rankNow) {
        TextView badge = findViewById(R.id.rank_change);
        if (rankBefore < 0 || rankNow == rankBefore) {
            badge.setVisibility(View.GONE);
            return;
        }
        boolean up = rankNow > rankBefore;
        String rankName = PlayerHud.rankName(this, rankNow);
        badge.setText(getString(up ? R.string.battle_rank_up : R.string.battle_rank_down, rankName));
        badge.setBackgroundResource(up ? R.drawable.bg_pill_red : R.drawable.bg_pill_surface);
        badge.setTextColor(getColor(up ? R.color.red : R.color.text_muted));
        badge.setVisibility(View.VISIBLE);
    }

    private void bindSigned(TextView view, int value) {
        view.setText(value == 0 ? "0" : String.format(Locale.ROOT, "%+d", value));
        view.setTextColor(getColor(value > 0 ? R.color.green : value < 0 ? R.color.red : R.color.text_muted));
    }

    private void loadImage(String url, int imageId) {
        Images.load((ImageView) findViewById(imageId), url);
    }

    private void bindRow(int rowId, int labelRes, int mine, int theirs) {
        View row = findViewById(rowId);
        TextView left = row.findViewById(R.id.row_left);
        TextView right = row.findViewById(R.id.row_right);
        ((TextView) row.findViewById(R.id.row_label)).setText(labelRes);
        left.setText(String.valueOf(mine));
        right.setText(String.valueOf(theirs));

        int winner = getColor(R.color.red);
        int loser = getColor(R.color.text_secondary);
        int tie = getColor(R.color.text_label);
        left.setTextColor(mine > theirs ? winner : mine < theirs ? loser : tie);
        right.setTextColor(theirs > mine ? winner : theirs < mine ? loser : tie);
    }

    private String heroName() {
        return hero.hasInfo() ? hero.getName() : getString(R.string.starter_hero_fallback);
    }

    private void goHome() {
        if (isSurvival()) {
            finish();
            return;
        }
        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }
}
