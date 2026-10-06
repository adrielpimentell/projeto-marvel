package com.example.marvel.game;

public final class GameBalance {

    private GameBalance() {
    }

    public static final int WIN_TROPHIES = 10;
    public static final int LOSS_TROPHIES = -5;
    public static final int MIN_TROPHIES = 0;

    public static final int[] RANK_MIN_TROPHIES = {0, 50, 150, 300, 500, 750, 1000, 1500, 2500,
            5000, 7500, 10_000, 15_000, 20_000, 30_000, 40_000, 50_000};

    public static final int STARTER_HERO_ID = 1425;
    public static final int STARTER_LIFE = 30;
    public static final int STARTER_STRENGTH = 18;
    public static final int STARTER_SPEED = 22;
    public static final int STARTER_INTELLIGENCE = 26;

    public static final int[][] CHEST_RARITY_WEIGHTS = {
            {0, 0, 0, 0},
            {70, 25, 5, 0},
            {60, 30, 9, 1},
            {50, 33, 14, 3},
            {40, 35, 20, 5},
            {30, 37, 25, 8},
            {20, 37, 31, 12},
            {10, 35, 38, 17},
            {0, 30, 45, 25},
            {0, 15, 50, 35},
            {0, 10, 50, 40},
            {0, 5, 50, 45},
            {0, 0, 50, 50},
            {0, 0, 40, 60},
            {0, 0, 30, 70},
            {0, 0, 20, 80},
            {0, 0, 0, 100},
    };
    public static final int[] DUPLICATE_COINS = {100, 250, 500, 1000};

    public static final int[] ROULETTE_PRICES = {5_000, 7_500, 15_000, 25_000};
    public static final int[][] ROULETTE_ODDS_PER_MILLE = {
            {700, 250, 45, 5},
            {400, 450, 130, 20},
            {150, 400, 370, 80},
            {0, 200, 500, 300},
    };

    public static final long ROULETTE_SPIN_MS = 3_200;
    public static final long ROULETTE_REVEAL_MS = 600;
    public static final int ROULETTE_FULL_TURNS = 6;

    public static final long CHEST_ANIM_ENTRY_MS = 350;
    public static final long CHEST_ANIM_SUSPENSE_MS = 800;
    public static final long CHEST_ANIM_OPEN_MS = 450;
    public static final long CHEST_ANIM_REVEAL_MS = 550;
    public static final long CHEST_ANIM_DETAILS_MS = 350;
    public static final int CHEST_PARTICLES = 14;
    public static final long CHEST_ANIM_SUSPENSE_EPIC_MS = 1_000;
    public static final int CHEST_PARTICLES_EPIC = 20;
    public static final long CHEST_ANIM_SUSPENSE_LEGENDARY_MS = 1_300;
    public static final int CHEST_PARTICLES_LEGENDARY = 20;
    public static final int CHEST_SCREEN_SHAKE_DP = 10;
    public static final int CHEST_SLOW_MOTION_FACTOR = 5;

    public static final int BONUS_COMMON_PERCENT = 5;
    public static final int BONUS_RARE_PERCENT = 10;
    public static final int BONUS_EPIC_PERCENT = 15;
    public static final int BONUS_LEGENDARY_ALL_PERCENT = 10;
    public static final int MAX_EQUIPPED_ARTIFACTS = 2;

    public static final int DODGE_CHANCE_PERCENT = 10;
    public static final int POISON_DAMAGE = 10;
    public static final int POISON_TURNS = 3;
    public static final int LIFESTEAL_PERCENT = 20;
    public static final int THORNS_PERCENT = 15;
    public static final int REGENERATION_PERCENT = 3;
    public static final int EXECUTE_THRESHOLD_PERCENT = 30;
    public static final int EXECUTE_BONUS_PERCENT = 100;
    public static final int STUN_CHANCE_PERCENT = 20;
    public static final int CRIT_BOOST_PERCENT = 15;

    public static final int STARTING_COINS = 0;
    public static final int VICTORY_COINS_MIN = 100;
    public static final int VICTORY_COINS_MAX = 500;
    public static final double RATIO_FOR_MIN_COINS = 0.5;
    public static final double RATIO_FOR_MAX_COINS = 1.5;
    public static final int DEFEAT_COINS = 0;

    public static final int HP_BASE = 200;
    public static final int HP_PER_LIFE = 1;
    public static final double DAMAGE_BASE = 40;
    public static final double DAMAGE_PER_STRENGTH = 0.2;
    public static final double DAMAGE_RANDOM_MIN = 0.5;
    public static final double DAMAGE_RANDOM_MAX = 1.5;
    public static final double CRIT_CHANCE_BASE = 0.12;
    public static final double CRIT_CHANCE_PER_INTELLIGENCE = 0.002;
    public static final double CRIT_CHANCE_MAX = 0.5;
    public static final double CRIT_MULTIPLIER = 3.5;
    public static final int MAX_TURNS = 40;

    public static final long SPLASH_MAX_MS = 6_500;

    public static final long BATTLE_MIN_DURATION_MS = 5_000;
    public static final long BATTLE_MAX_DURATION_MS = 15_000;
    public static final long BATTLE_TURN_MS = 1_100;
    public static final long BATTLE_INTRO_MS = 1_300;
    public static final long BATTLE_OUTRO_MS = 900;

    public static final int PRICE_BASE = 300;
    public static final double PRICE_PER_OVERALL_SQUARED = 1.2;
    public static final int PRICE_ROUNDING = 50;
    public static final int MARKET_BATCH_SIZE = 12;

    public static final int MAX_ATTRIBUTE = 100;
    public static final int UPGRADE_STEP = 5;
    public static final int UPGRADE_BASE_COST = 75;

    public static final int[] ALBUM_TEAM_IDS = {
            3806,
            3173,
            3804,
            25956,
            26333,
            42520,
            40421,
            15595,
            23977,
            3775,
            17652,
            40429,
    };
    public static final int[] TEAM_MILESTONES = {1, 3, 5};
    public static final int[] TEAM_MILESTONE_COINS = {200, 600, 1_500};

    public static final int[] DAILY_COINS = {150, 250, 400};
    public static final int[] DAILY_WIN_TARGETS = {2, 3};
    public static final int[] DAILY_PLAY_TARGETS = {3, 4};
    public static final int[] DAILY_CRIT_TARGETS = {3, 5};
    public static final int[] DAILY_ORIGIN_IDS = {1, 3, 4, 6, 7};
    public static final int DAILY_OVERALL_STEP = 5;
    public static final int DAILY_CHEST_MIN_RANK = 1;

    public static final int SURVIVAL_START_POWER_PERCENT = 50;
    public static final double SURVIVAL_POWER_GROWTH = 1.10;
    public static final int SURVIVAL_HEAL_PERCENT = 30;
    public static final int SURVIVAL_COINS_PER_FLOOR = 20;
    public static final int[] SURVIVAL_CHEST_FLOORS = {10, 20};
    public static final int SURVIVAL_BUFF_CHOICES = 3;
    public static final int SURVIVAL_FURY_DAMAGE_PERCENT = 20;
    public static final int SURVIVAL_ARMOR_HP_PERCENT = 20;
    public static final int SURVIVAL_MEDKIT_HEAL_PERCENT = 50;
    public static final int SURVIVAL_ENEMY_POOL_PAGES = 5;

    public static final int RANKING_SIZE = 50;
    public static final Artifact.Rarity[] SEASON_PRIZE_RARITIES = {
            Artifact.Rarity.LEGENDARY, Artifact.Rarity.EPIC, Artifact.Rarity.RARE};
    public static final int[] SEASON_PRIZE_COINS = {500, 250, 100};
    public static final Artifact.Rarity SEASON_PRIZE_OTHERS_RARITY = Artifact.Rarity.COMMON;
}
