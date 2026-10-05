package com.example.marvel.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.marvel.data.model.NamedRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FamiliesTest {

    private static final NamedRef AVENGERS = new NamedRef(3806, "Avengers");
    private static final NamedRef X_MEN = new NamedRef(3173, "X-Men");
    private static final NamedRef FANTASTIC_FOUR = new NamedRef(3804, "Fantastic Four");

    @Test
    public void heroWithSeveralTeamsCountsInEachAndBiggestComesFirst() {
        List<List<NamedRef>> heroes = Arrays.asList(
                Arrays.asList(AVENGERS, X_MEN),
                Arrays.asList(X_MEN),
                Arrays.asList(X_MEN, X_MEN),
                Arrays.asList(FANTASTIC_FOUR, AVENGERS),
                Collections.emptyList(),
                null);
        List<Families.Family> families = Families.count(heroes);

        assertEquals(3, families.size());
        assertEquals("X-Men", families.get(0).name);
        assertEquals(3, families.get(0).count);
        assertEquals("Avengers", families.get(1).name);
        assertEquals(2, families.get(1).count);
        assertEquals("Fantastic Four", families.get(2).name);
        assertEquals(1, families.get(2).count);
    }

    @Test
    public void tiesAreAlphabetical() {
        List<Families.Family> families = Families.count(Arrays.asList(
                Collections.singletonList(X_MEN), Collections.singletonList(AVENGERS)));
        assertEquals("Avengers", families.get(0).name);
        assertEquals("X-Men", families.get(1).name);
    }

    @Test
    public void containsRespectsAllAndMissingTeams() {
        List<NamedRef> wolverine = Arrays.asList(AVENGERS, X_MEN);
        assertTrue(Families.contains(wolverine, 0));
        assertTrue(Families.contains(wolverine, X_MEN.getId()));
        assertFalse(Families.contains(wolverine, FANTASTIC_FOUR.getId()));
        assertFalse(Families.contains(null, X_MEN.getId()));
        assertTrue(Families.contains(null, 0));
    }
}
