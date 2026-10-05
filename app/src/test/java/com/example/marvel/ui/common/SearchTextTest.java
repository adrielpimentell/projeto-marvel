package com.example.marvel.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SearchTextTest {

    @Test
    public void partOfTheNameIgnoringCase() {
        assertTrue(SearchText.matches("Spider-Man", "spider"));
        assertTrue(SearchText.matches("Spider-Man", "MAN"));
        assertTrue(SearchText.matches("Captain America", "  america "));
        assertFalse(SearchText.matches("Spider-Man", "thor"));
    }

    @Test
    public void accentsAreIgnoredOnBothSides() {
        assertTrue(SearchText.matches("Homem-Aranha", "aranha"));
        assertTrue(SearchText.matches("Homem-Aranha", "arãnha"));
        assertTrue(SearchText.matches("Mágico", "magico"));
        assertEquals("Magico", SearchText.withoutAccents("Mágico"));
    }

    @Test
    public void emptySearchShowsEverythingAndNullIsSafe() {
        assertTrue(SearchText.matches("Wolverine", ""));
        assertTrue(SearchText.matches(null, "   "));
        assertFalse(SearchText.matches(null, "x"));
        assertEquals("", SearchText.normalize(null));
    }
}
