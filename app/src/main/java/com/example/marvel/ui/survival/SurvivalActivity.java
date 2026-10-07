package com.example.marvel.ui.survival;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.ui.common.Images;
import com.example.marvel.R;
import com.example.marvel.data.api.ComicVineService;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.CharacterPage;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.data.repository.SortOption;
import com.example.marvel.game.AttributeCalculator;
import com.example.marvel.game.GameBalance;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerState;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.game.Survival;
import com.example.marvel.game.SurvivalBuff;
import com.example.marvel.game.SurvivalRun;
import com.example.marvel.ui.battle.BattleActivity;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.ranks.ChestOpenActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SurvivalActivity extends AppCompatActivity {

    private static final int MAX_ENEMY_ATTEMPTS = 3;

    private PlayerStore playerStore;
    private CharacterRepository repository;
    private final Random random = new Random();

    private View playerHud;
    private TextView recordText;
    private MaterialButton chestButton;
    private View idlePanel;
    private TextView lastRunText;
    private TextView idleHeroText;
    private View runPanel;
    private TextView floorText;
    private TextView powerText;
    private TextView heroNameText;
    private LinearProgressIndicator hpBar;
    private TextView hpText;
    private TextView buffsText;
    private TextView runCoinsText;
    private View choicePanel;
    private View[] buffCards;
    private View enemyPanel;
    private View enemyLoading;
    private View enemyContent;
    private View enemyError;
    private TextView enemyErrorText;
    private ImageView enemyImage;
    private TextView enemyName;
    private TextView enemyInfo;
    private MaterialButton fightButton;

    private Character enemy;
    private RequestHandle request;

    public static Intent newIntent(Context context) {
        return new Intent(context, SurvivalActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_survival);

        playerStore = PlayerStore.getInstance(this);
        repository = CharacterRepository.getInstance(this);
        bindViews();

        View root = findViewById(R.id.survival_root);
        Screens.padForSystemBars(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        cancelRequest();
        super.onDestroy();
    }

    private void bindViews() {
        playerHud = findViewById(R.id.player_hud);
        recordText = findViewById(R.id.survival_record);
        chestButton = findViewById(R.id.survival_chest);
        idlePanel = findViewById(R.id.survival_idle);
        lastRunText = findViewById(R.id.survival_last_run);
        idleHeroText = findViewById(R.id.survival_hero);
        runPanel = findViewById(R.id.survival_run);
        floorText = findViewById(R.id.survival_floor);
        powerText = findViewById(R.id.survival_power);
        heroNameText = findViewById(R.id.survival_hero_name);
        hpBar = findViewById(R.id.survival_hp_bar);
        hpText = findViewById(R.id.survival_hp_text);
        buffsText = findViewById(R.id.survival_buffs);
        runCoinsText = findViewById(R.id.survival_run_coins);
        choicePanel = findViewById(R.id.survival_choice);
        buffCards = new View[]{
                findViewById(R.id.survival_buff_0),
                findViewById(R.id.survival_buff_1),
                findViewById(R.id.survival_buff_2)};
        enemyPanel = findViewById(R.id.survival_enemy);
        enemyLoading = findViewById(R.id.survival_enemy_loading);
        enemyContent = findViewById(R.id.survival_enemy_content);
        enemyError = findViewById(R.id.survival_enemy_error);
        enemyErrorText = findViewById(R.id.survival_enemy_error_text);
        enemyImage = findViewById(R.id.survival_enemy_image);
        enemyName = findViewById(R.id.survival_enemy_name);
        enemyInfo = findViewById(R.id.survival_enemy_info);
        fightButton = findViewById(R.id.survival_fight);

        ((TextView) findViewById(R.id.survival_rules)).setText(
                getString(R.string.survival_rules, GameBalance.SURVIVAL_HEAL_PERCENT));
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.survival_start).setOnClickListener(v -> startRun());
        findViewById(R.id.survival_give_up).setOnClickListener(v -> confirmGiveUp());
        findViewById(R.id.survival_enemy_retry).setOnClickListener(v -> prepareEnemy());
        chestButton.setOnClickListener(v -> startActivity(ChestOpenActivity.newSurvivalIntent(this)));
        fightButton.setOnClickListener(v -> fight());
        for (int i = 0; i < buffCards.length; i++) {
            final int index = i;
            buffCards[i].setOnClickListener(v -> {
                if (playerStore.chooseSurvivalBuff(index)) refresh();
            });
        }
    }

    private void refresh() {
        PlayerState state = playerStore.get();
        PlayerHud.bind(playerHud, state);
        int best = state.getSurvivalBestFloor();
        recordText.setText(best > 0 ? getString(R.string.survival_record, best)
                : getString(R.string.survival_no_record));
        chestButton.setVisibility(state.getSurvivalChestsToOpen() > 0 ? View.VISIBLE : View.GONE);

        SurvivalRun run = state.getActiveSurvival();
        if (run == null) {
            showIdle(state);
        } else {
            showRun(state, run);
        }
    }

    private void showIdle(PlayerState state) {
        cancelRequest();
        enemy = null;
        idlePanel.setVisibility(View.VISIBLE);
        runPanel.setVisibility(View.GONE);
        OwnedHero hero = state.getActiveHero();
        idleHeroText.setText(getString(R.string.survival_hero, heroName(hero),
                hero.getBattleAttributes().getOverall()));
        SurvivalRun last = state.getSurvival();
        lastRunText.setVisibility(last != null && last.isOver() ? View.VISIBLE : View.GONE);
        if (last != null && last.isOver()) {
            lastRunText.setText(getString(R.string.survival_last_run, last.getFloor(),
                    PlayerHud.format(last.getCoinsEarned())));
        }
    }

    private void showRun(PlayerState state, SurvivalRun run) {
        idlePanel.setVisibility(View.GONE);
        runPanel.setVisibility(View.VISIBLE);
        OwnedHero hero = state.find(run.getHeroId());
        int max = Survival.maxHp(run, hero);
        int hp = Survival.currentHp(run, hero);

        floorText.setText(getString(R.string.survival_floor, run.getFloor()));
        powerText.setText(getString(R.string.survival_power, Survival.powerPercent(run.getFloor())));
        heroNameText.setText(heroName(hero));
        hpBar.setMax(max);
        hpBar.setProgressCompat(hp, true);
        hpText.setText(getString(R.string.survival_hp, hp, max));
        buffsText.setText(SurvivalUi.buffList(this, run));
        runCoinsText.setText(getString(R.string.survival_run_coins, PlayerHud.format(run.getCoinsEarned())));

        if (run.hasChoicePending()) {
            choicePanel.setVisibility(View.VISIBLE);
            enemyPanel.setVisibility(View.GONE);
            bindOffer(run.getOffer());
            return;
        }
        choicePanel.setVisibility(View.GONE);
        enemyPanel.setVisibility(View.VISIBLE);
        fightButton.setText(getString(R.string.survival_fight, run.getFloor()));
        if (enemy != null && enemy.getId() == run.getEnemyId()) {
            showEnemy(run);
        } else {
            prepareEnemy();
        }
    }

    private void bindOffer(List<SurvivalBuff> offer) {
        for (int i = 0; i < buffCards.length; i++) {
            View card = buffCards[i];
            card.setVisibility(i < offer.size() ? View.VISIBLE : View.GONE);
            if (i >= offer.size()) continue;
            SurvivalBuff buff = offer.get(i);
            ImageView icon = card.findViewById(R.id.buff_icon);
            icon.setImageResource(SurvivalUi.icon(buff));
            icon.setColorFilter(SurvivalUi.color(this, buff));
            String name = SurvivalUi.name(this, buff);
            String description = SurvivalUi.description(this, buff);
            ((TextView) card.findViewById(R.id.buff_name)).setText(name);
            ((TextView) card.findViewById(R.id.buff_description)).setText(description);
            card.setContentDescription(name + ". " + description);
        }
    }

    private void prepareEnemy() {
        SurvivalRun run = playerStore.get().getActiveSurvival();
        if (run == null || run.hasChoicePending()) return;
        cancelRequest();
        showEnemyLoading();
        if (run.getEnemyId() != 0) {
            loadEnemyDetail(run.getEnemyId());
        } else {
            pickEnemy(1);
        }
    }

    private void pickEnemy(int attempt) {
        int page = random.nextInt(GameBalance.SURVIVAL_ENEMY_POOL_PAGES);
        request = repository.loadCharacters(null, SortOption.CLASSIC,
                page * ComicVineService.MAX_PAGE_SIZE, new RepoCallback<CharacterPage>() {
                    @Override
                    public void onSuccess(CharacterPage result) {
                        SurvivalRun run = playerStore.get().getActiveSurvival();
                        if (run == null) return;
                        List<Character> options = new ArrayList<>();
                        for (Character c : result.getCharacters()) {
                            if (c.getId() != run.getHeroId() && !run.getUsedEnemyIds().contains(c.getId())) {
                                options.add(c);
                            }
                        }
                        if (options.isEmpty()) {
                            if (attempt < MAX_ENEMY_ATTEMPTS) pickEnemy(attempt + 1);
                            else showEnemyError(getString(R.string.error_not_found));
                            return;
                        }
                        Character chosen = options.get(random.nextInt(options.size()));
                        if (playerStore.setSurvivalEnemy(chosen.getId())) {
                            loadEnemyDetail(chosen.getId());
                        } else {
                            refresh();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        showEnemyError(message);
                    }
                });
    }

    private void loadEnemyDetail(int id) {
        request = repository.loadCharacterDetail(id, new RepoCallback<Character>() {
            @Override
            public void onSuccess(Character detail) {
                enemy = detail;
                SurvivalRun run = playerStore.get().getActiveSurvival();
                if (run != null) showEnemy(run);
            }

            @Override
            public void onError(String message) {
                showEnemyError(message);
            }
        });
    }

    private void showEnemyLoading() {
        enemyLoading.setVisibility(View.VISIBLE);
        enemyContent.setVisibility(View.GONE);
        enemyError.setVisibility(View.GONE);
        fightButton.setEnabled(false);
    }

    private void showEnemy(SurvivalRun run) {
        enemyLoading.setVisibility(View.GONE);
        enemyError.setVisibility(View.GONE);
        enemyContent.setVisibility(View.VISIBLE);
        enemyName.setText(enemy.getName());
        enemyInfo.setText(getString(R.string.survival_enemy_info,
                AttributeCalculator.calculate(enemy).getOverall(), Survival.powerPercent(run.getFloor())));
        Images.load(enemyImage, enemy.getCardImageUrl());
        fightButton.setEnabled(true);
    }

    private void showEnemyError(String message) {
        enemyLoading.setVisibility(View.GONE);
        enemyContent.setVisibility(View.GONE);
        enemyError.setVisibility(View.VISIBLE);
        enemyErrorText.setText(getString(R.string.survival_enemy_error, message));
        fightButton.setEnabled(false);
    }

    private void startRun() {
        if (playerStore.startSurvival()) forgetEnemyAndRefresh();
    }

    private void fight() {
        SurvivalRun run = playerStore.get().getActiveSurvival();
        if (run == null || enemy == null || enemy.getId() != run.getEnemyId()) return;
        fightButton.setEnabled(false);
        startActivity(BattleActivity.newSurvivalIntent(this, enemy, run.getFloor()));
    }

    private void confirmGiveUp() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.survival_give_up_title)
                .setMessage(R.string.survival_give_up_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.survival_give_up, (dialog, which) -> {
                    if (playerStore.endSurvival()) forgetEnemyAndRefresh();
                })
                .show();
    }

    private void forgetEnemyAndRefresh() {
        cancelRequest();
        enemy = null;
        refresh();
    }

    private void cancelRequest() {
        if (request != null) request.cancel();
        request = null;
    }

    private String heroName(OwnedHero hero) {
        return hero.hasInfo() ? hero.getName() : getString(R.string.starter_hero_fallback);
    }
}
