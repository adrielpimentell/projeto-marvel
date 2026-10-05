package com.example.marvel.ui.market;

import com.example.marvel.data.model.Character;
import com.example.marvel.game.Economy;
import com.example.marvel.game.GameAttributes;

final class MarketItem {

    Character character;
    GameAttributes attributes;
    int price;
    boolean failed;

    MarketItem(Character character) {
        this.character = character;
    }

    boolean isReady() {
        return attributes != null;
    }

    void setDetails(Character detailed, GameAttributes attributes) {
        this.character = detailed;
        this.attributes = attributes;
        this.price = Economy.heroPrice(attributes.getOverall());
        this.failed = false;
    }
}
