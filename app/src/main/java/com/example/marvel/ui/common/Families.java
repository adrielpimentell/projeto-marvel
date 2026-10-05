package com.example.marvel.ui.common;

import com.example.marvel.data.model.NamedRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Families {

    public static final class Family {
        public final int id;
        public final String name;
        public final int count;

        public Family(int id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Family)) return false;
            Family other = (Family) o;
            return id == other.id && count == other.count && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, count);
        }
    }

    private Families() {
    }

    public static List<Family> count(List<List<NamedRef>> teamsPerHero) {
        Map<Integer, String> names = new LinkedHashMap<>();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (List<NamedRef> teams : teamsPerHero) {
            if (teams == null) continue;
            Set<Integer> seen = new HashSet<>();
            for (NamedRef team : teams) {
                if (team == null || team.getName().isEmpty() || !seen.add(team.getId())) continue;
                names.putIfAbsent(team.getId(), team.getName());
                counts.merge(team.getId(), 1, Integer::sum);
            }
        }
        List<Family> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            result.add(new Family(entry.getKey(), names.get(entry.getKey()), entry.getValue()));
        }
        result.sort((a, b) -> a.count != b.count ? Integer.compare(b.count, a.count)
                : a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT)));
        return result;
    }

    public static boolean contains(List<NamedRef> teams, int familyId) {
        if (familyId == 0) return true;
        if (teams == null) return false;
        for (NamedRef team : teams) {
            if (team != null && team.getId() == familyId) return true;
        }
        return false;
    }
}
