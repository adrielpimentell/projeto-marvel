package com.example.marvel.game;

import static com.example.marvel.game.Artifact.Effect;
import static com.example.marvel.game.Artifact.Rarity;
import static com.example.marvel.game.Artifact.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ArtifactCatalog {

    private static final List<Artifact> ALL = Collections.unmodifiableList(Arrays.asList(
            bonus("soro_supersoldado", "Soro do Supersoldado", Rarity.COMMON, 0, GameBalance.BONUS_COMMON_PERCENT, 0, 0),
            bonus("fluido_teia", "Fluido de Teia Concentrado", Rarity.COMMON, 0, 0, GameBalance.BONUS_COMMON_PERCENT, 0),
            bonus("oculos_taticos", "Óculos Táticos", Rarity.COMMON, 0, 0, 0, GameBalance.BONUS_COMMON_PERCENT),
            bonus("amostra_cura", "Amostra de Fator de Cura", Rarity.COMMON, GameBalance.BONUS_COMMON_PERCENT, 0, 0, 0),
            bonus("botas_jato", "Botas a Jato", Rarity.RARE, 0, 0, GameBalance.BONUS_RARE_PERCENT, 0),
            bonus("garras_adamantium", "Garras de Adamantium", Rarity.RARE, 0, GameBalance.BONUS_RARE_PERCENT, 0, 0),
            bonus("capacete_telepatico", "Capacete Telepático", Rarity.EPIC, 0, 0, 0, GameBalance.BONUS_EPIC_PERCENT),
            bonus("armadura_extremis", "Armadura Extremis", Rarity.EPIC, GameBalance.BONUS_EPIC_PERCENT, 0, 0, 0),
            bonus("fragmento_cosmico", "Fragmento Cósmico", Rarity.LEGENDARY,
                    GameBalance.BONUS_LEGENDARY_ALL_PERCENT, GameBalance.BONUS_LEGENDARY_ALL_PERCENT,
                    GameBalance.BONUS_LEGENDARY_ALL_PERCENT, GameBalance.BONUS_LEGENDARY_ALL_PERCENT),

            special("sentido_aranha", "Sentido Aranha", Rarity.COMMON, Effect.DODGE),
            special("veneno_simbionte", "Veneno Simbionte", Rarity.RARE, Effect.POISON),
            special("lamina_vampirica", "Lâmina Vampírica", Rarity.RARE, Effect.LIFESTEAL),
            special("armadura_espinhos", "Armadura de Espinhos", Rarity.RARE, Effect.THORNS),
            special("fator_cura", "Fator de Cura", Rarity.EPIC, Effect.REGENERATION),
            special("escudo_vibranium", "Escudo de Vibranium", Rarity.LEGENDARY, Effect.SHIELD),
            special("instinto_cacador", "Instinto de Caçador", Rarity.EPIC, Effect.EXECUTE),
            special("martelo_trovao", "Martelo do Trovão", Rarity.LEGENDARY, Effect.STUN),
            special("joia_poder", "Joia do Poder", Rarity.LEGENDARY, Effect.CRIT_BOOST)
    ));

    private ArtifactCatalog() {
    }

    public static List<Artifact> all() {
        return ALL;
    }

    public static Artifact byId(String id) {
        for (Artifact artifact : ALL) {
            if (artifact.getId().equals(id)) return artifact;
        }
        return null;
    }

    public static List<Artifact> ofRarity(Rarity rarity) {
        List<Artifact> result = new ArrayList<>();
        for (Artifact artifact : ALL) {
            if (artifact.getRarity() == rarity) result.add(artifact);
        }
        return result;
    }

    private static Artifact bonus(String id, String name, Rarity rarity,
                                  int life, int strength, int speed, int intelligence) {
        return new Artifact(id, name, rarity, Type.ATTRIBUTE, life, strength, speed, intelligence, Effect.NONE);
    }

    private static Artifact special(String id, String name, Rarity rarity, Effect effect) {
        return new Artifact(id, name, rarity, Type.SPECIAL, 0, 0, 0, 0, effect);
    }
}
