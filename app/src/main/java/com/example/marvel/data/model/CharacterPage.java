package com.example.marvel.data.model;

import java.util.Collections;
import java.util.List;

public class CharacterPage {

    private final List<Character> characters;
    private final int nextOffset;
    private final boolean hasMore;

    public CharacterPage(List<Character> characters, int nextOffset, boolean hasMore) {
        this.characters = Collections.unmodifiableList(characters);
        this.nextOffset = nextOffset;
        this.hasMore = hasMore;
    }

    public List<Character> getCharacters() {
        return characters;
    }

    public int getNextOffset() {
        return nextOffset;
    }

    public boolean hasMore() {
        return hasMore;
    }
}
