package com.example.marvel.ui.detail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.marvel.ui.common.Screens;
import com.example.marvel.ui.common.Images;
import com.example.marvel.R;
import com.example.marvel.data.model.Character;
import com.example.marvel.data.model.NamedRef;
import com.example.marvel.data.repository.CharacterRepository;
import com.example.marvel.data.repository.RepoCallback;
import com.example.marvel.data.repository.RequestHandle;
import com.example.marvel.game.AttributeCalculator;
import com.example.marvel.game.DailyChallenge;
import com.example.marvel.game.DailyChallenges;
import com.example.marvel.game.DailyClock;
import com.example.marvel.game.Economy;
import com.example.marvel.game.GameAttributes;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.battle.BattleActivity;
import com.example.marvel.ui.common.OriginUi;
import com.example.marvel.ui.common.PlayerHud;
import com.example.marvel.ui.common.Skeleton;
import com.example.marvel.ui.common.StateView;
import com.example.marvel.ui.daily.DailyUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class CharacterDetailActivity extends AppCompatActivity {

    private static final String EXTRA_CHARACTER = "extra_character";
    private static final int CHIP_LIMIT = 12;
    private static final int MOVIE_LIMIT = 6;


    private CharacterRepository repository;
    private RequestHandle request;
    private Character character;
    private GameAttributes attributes;

    private View root;
    private View battleBar;
    private ImageView heroImage;
    private TextView nameText;
    private TextView realNameText;
    private TextView issuesValue;
    private TextView moviesValue;
    private TextView teamsValue;
    private LinearLayout infoRows;
    private View detailsProgress;
    private StateView detailsError;
    private View detailsContent;
    private TextView overallValue;
    private LinearProgressIndicator overallBar;
    private TextView aboutText;
    private MaterialButton openArticle;
    private TextView powersLabel;
    private ChipGroup powersGroup;
    private TextView teamsLabel;
    private ChipGroup teamsGroup;
    private TextView moviesLabel;
    private LinearLayout moviesList;
    private MaterialButton battleButton;

    public static Intent newIntent(Context context, Character character) {
        return new Intent(context, CharacterDetailActivity.class)
                .putExtra(EXTRA_CHARACTER, character);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screens.edgeToEdge(this);
        setContentView(R.layout.activity_character_detail);

        character = getIntent().getSerializableExtra(EXTRA_CHARACTER, Character.class);
        if (character == null) {
            finish();
            return;
        }
        repository = CharacterRepository.getInstance(this);

        bindViews();
        applyWindowInsets();
        showBasics(character);
        loadDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerHud.bind(findViewById(R.id.player_hud), PlayerStore.getInstance(this).get());
        updateBattleButton();
    }

    @Override
    protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }

    private void bindViews() {
        root = findViewById(R.id.detail_root);
        battleBar = findViewById(R.id.battle_bar);
        heroImage = findViewById(R.id.hero_image);
        nameText = findViewById(R.id.detail_name);
        realNameText = findViewById(R.id.detail_real_name);
        issuesValue = findViewById(R.id.stat_issues_value);
        moviesValue = findViewById(R.id.stat_movies_value);
        teamsValue = findViewById(R.id.stat_teams_value);
        infoRows = findViewById(R.id.info_rows);
        detailsProgress = findViewById(R.id.details_progress);
        detailsError = new StateView(findViewById(R.id.details_error));
        detailsContent = findViewById(R.id.details_content);
        overallValue = findViewById(R.id.overall_value);
        overallBar = findViewById(R.id.overall_bar);
        aboutText = findViewById(R.id.about_text);
        openArticle = findViewById(R.id.open_article);
        powersLabel = findViewById(R.id.powers_label);
        powersGroup = findViewById(R.id.powers_group);
        teamsLabel = findViewById(R.id.teams_label);
        teamsGroup = findViewById(R.id.teams_group);
        moviesLabel = findViewById(R.id.movies_label);
        moviesList = findViewById(R.id.movies_list);
        battleButton = findViewById(R.id.battle_button);

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        battleButton.setOnClickListener(v -> onBattleClick());
    }

    private void applyWindowInsets() {
        View backButton = findViewById(R.id.back_button);
        ViewGroup.MarginLayoutParams backParams =
                (ViewGroup.MarginLayoutParams) backButton.getLayoutParams();
        int backTopMargin = backParams.topMargin;
        int barBottomPadding = battleBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            backParams.topMargin = backTopMargin + bars.top;
            backButton.setLayoutParams(backParams);
            battleBar.setPadding(battleBar.getPaddingLeft(), battleBar.getPaddingTop(),
                    battleBar.getPaddingRight(), barBottomPadding + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void loadDetails() {
        Skeleton.show(detailsProgress);
        detailsError.hide();
        battleButton.setEnabled(false);

        request = repository.loadCharacterDetail(character.getId(), new RepoCallback<Character>() {
            @Override
            public void onSuccess(Character detail) {
                character = detail;
                showBasics(detail);
                showDetails(detail);
            }

            @Override
            public void onError(String message) {
                Skeleton.hide(detailsProgress);
                detailsError.show(R.drawable.ic_error, null, message,
                        getString(R.string.action_retry), () -> loadDetails());
            }
        });
    }

    private void showBasics(Character c) {
        nameText.setText(c.getName());
        String realName = c.getRealName();
        realNameText.setText(realName.isEmpty() ? getString(R.string.real_name_unknown) : realName);
        issuesValue.setText(PlayerHud.format(c.getIssueAppearances()));
        if (!c.hasDetails()) {
            moviesValue.setText(R.string.stat_unknown);
            teamsValue.setText(R.string.stat_unknown);
        }

        Images.loadLarge(heroImage, c.getLargeImageUrl(), c.getCardImageUrl());
    }

    private void showDetails(Character c) {
        Skeleton.hide(detailsProgress);
        detailsError.hide();
        detailsContent.setVisibility(View.VISIBLE);

        moviesValue.setText(PlayerHud.format(c.getMovies().size()));
        teamsValue.setText(PlayerHud.format(c.getTeams().size()));

        showInfoRows(c);
        showAttributes(AttributeCalculator.calculate(c));
        showAbout(c);

        powersLabel.setText(getString(R.string.section_powers, c.getPowers().size()));
        fillChips(powersGroup, c.getPowers(), false, R.string.powers_empty);
        teamsLabel.setText(getString(R.string.section_teams, c.getTeams().size()));
        fillChips(teamsGroup, c.getTeams(), false, R.string.teams_empty);
        moviesLabel.setText(getString(R.string.section_movies, c.getMovies().size()));
        fillMovies(c.getMovies(), false);

        battleButton.setEnabled(true);
        updateBattleButton();
    }

    private void updateBattleButton() {
        if (attributes == null) return;
        int heroOverall = PlayerStore.getInstance(this).get().getActiveHero().getBattleAttributes().getOverall();
        battleButton.setText(getString(R.string.battle_button_reward,
                Economy.coinsForVictory(heroOverall, attributes.getOverall())));
        updateDailyHint(heroOverall);
    }

    private void updateDailyHint(int heroOverall) {
        TextView hint = findViewById(R.id.detail_daily_hint);
        DailyChallenges daily = PlayerStore.getInstance(this).get().getDaily();
        List<String> matches = new ArrayList<>();
        if (daily != null && daily.getDay() >= DailyClock.today()) {
            for (DailyChallenge challenge : daily.getChallenges()) {
                if (challenge.countsEnemy(character, attributes.getOverall(), heroOverall)) {
                    matches.add(DailyUi.describe(this, challenge));
                }
            }
        }
        hint.setText(getString(R.string.detail_counts_for_daily, String.join(" · ", matches)));
        hint.setVisibility(matches.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showInfoRows(Character c) {
        infoRows.removeAllViews();

        NamedRef origin = c.getOrigin();
        if (origin != null && !origin.getName().isEmpty()) {
            addInfoRow(R.drawable.ic_star, R.string.info_origin,
                    OriginUi.name(this, origin.getId(), origin.getName()));
        }

        NamedRef first = c.getFirstAppearance();
        if (first != null && !first.getName().isEmpty()) {
            String issue = first.getIssueNumber();
            String text = issue == null || issue.trim().isEmpty()
                    ? first.getName()
                    : getString(R.string.first_appearance_format, issue.trim(), first.getName());
            addInfoRow(R.drawable.ic_book, R.string.info_first_appearance, text);
        }

        List<String> aliases = c.getAliases();
        if (!aliases.isEmpty()) {
            List<String> firstAliases = aliases.subList(0, Math.min(3, aliases.size()));
            addInfoRow(R.drawable.ic_person, R.string.info_aliases, String.join(", ", firstAliases));
        }

        infoRows.setVisibility(infoRows.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void addInfoRow(int iconRes, int labelRes, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_info_row, infoRows, false);
        ((ImageView) row.findViewById(R.id.info_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.info_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.info_value)).setText(value);
        infoRows.addView(row);
    }

    private void showAttributes(GameAttributes a) {
        attributes = a;
        overallValue.setText(String.valueOf(a.getOverall()));
        overallBar.setProgressCompat(a.getOverall(), true);
        bindAttributeRow(R.id.attr_life, R.string.attr_life, a.getLife());
        bindAttributeRow(R.id.attr_strength, R.string.attr_strength, a.getStrength());
        bindAttributeRow(R.id.attr_speed, R.string.attr_speed, a.getSpeed());
        bindAttributeRow(R.id.attr_intelligence, R.string.attr_intelligence, a.getIntelligence());
    }

    private void bindAttributeRow(int rowId, int labelRes, int value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.attribute_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.attribute_value)).setText(String.valueOf(value));
        ((LinearProgressIndicator) row.findViewById(R.id.attribute_bar)).setProgressCompat(value, true);
    }

    private void showAbout(Character c) {
        String deck = c.getDeck();
        aboutText.setText(deck.isEmpty() ? getString(R.string.about_empty) : deck);

        String url = c.getSiteDetailUrl();
        openArticle.setVisibility(url == null ? View.GONE : View.VISIBLE);
        openArticle.setOnClickListener(v -> openInBrowser(url));
    }

    private void fillChips(ChipGroup group, List<NamedRef> items, boolean expanded, int emptyRes) {
        group.removeAllViews();
        if (items.isEmpty()) {
            Chip empty = inflateChip(group, getString(emptyRes));
            empty.setChipBackgroundColorResource(android.R.color.transparent);
            empty.setTextColor(getColor(R.color.text_secondary));
            group.addView(empty);
            return;
        }

        int shown = expanded ? items.size() : Math.min(items.size(), CHIP_LIMIT);
        for (int i = 0; i < shown; i++) {
            group.addView(inflateChip(group, items.get(i).getName()));
        }
        if (shown < items.size()) {
            Chip more = inflateChip(group, getString(R.string.show_more_chips, items.size() - shown));
            more.setTextColor(getColor(R.color.red));
            more.setClickable(true);
            more.setOnClickListener(v -> fillChips(group, items, true, emptyRes));
            group.addView(more);
        }
    }

    private Chip inflateChip(ChipGroup group, String text) {
        Chip chip = (Chip) LayoutInflater.from(this).inflate(R.layout.item_info_chip, group, false);
        chip.setText(text);
        return chip;
    }

    private void fillMovies(List<NamedRef> movies, boolean expanded) {
        moviesList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (movies.isEmpty()) {
            TextView row = (TextView) inflater.inflate(R.layout.item_movie_row, moviesList, false);
            row.setText(R.string.movies_empty);
            row.setTextColor(getColor(R.color.text_secondary));
            moviesList.addView(row);
            return;
        }

        int shown = expanded ? movies.size() : Math.min(movies.size(), MOVIE_LIMIT);
        for (int i = 0; i < shown; i++) {
            TextView row = (TextView) inflater.inflate(R.layout.item_movie_row, moviesList, false);
            row.setText(movies.get(i).getName());
            moviesList.addView(row);
        }
        if (shown < movies.size()) {
            MaterialButton showAll = (MaterialButton) inflater.inflate(
                    R.layout.item_text_button, moviesList, false);
            showAll.setText(getString(R.string.show_all_movies, movies.size()));
            showAll.setOnClickListener(v -> fillMovies(movies, true));
            moviesList.addView(showAll);
        }
    }

    private void onBattleClick() {
        OwnedHero active = PlayerStore.getInstance(this).get().getActiveHero();
        if (active.getCharacterId() == character.getId()) {
            Snackbar.make(root, getString(R.string.battle_self, character.getName()), Snackbar.LENGTH_SHORT)
                    .setAnchorView(battleBar)
                    .show();
            return;
        }
        startActivity(BattleActivity.newIntent(this, character));
    }

    private void openInBrowser(String url) {
        if (url == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(root, R.string.no_browser, Snackbar.LENGTH_SHORT)
                    .setAnchorView(battleBar)
                    .show();
        }
    }
}
