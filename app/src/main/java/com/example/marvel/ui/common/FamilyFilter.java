package com.example.marvel.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.example.marvel.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FamilyFilter {

    public interface Listener {
        void onFamilyChanged(int familyId);
    }

    private static final int MAX_PILLS = 10;

    private final View root;
    private final ChipGroup group;
    private final Listener listener;
    private List<Families.Family> families = Collections.emptyList();
    private int selectedId;
    private String selectedName = "";

    public FamilyFilter(View root, Listener listener) {
        this.root = root;
        this.group = root.findViewById(R.id.family_chips);
        this.listener = listener;
    }

    public int getSelectedId() {
        return selectedId;
    }

    public String getSelectedName() {
        return selectedName;
    }

    public void setFamilies(List<Families.Family> newFamilies) {
        if (newFamilies.equals(families) && group.getChildCount() > 0) return;
        families = new ArrayList<>(newFamilies);
        render();
    }

    private void render() {
        Context context = group.getContext();
        group.removeAllViews();
        root.setVisibility(families.isEmpty() && selectedId == 0 ? View.GONE : View.VISIBLE);

        addChip(context.getString(R.string.family_all), selectedId == 0, () -> select(0, ""));
        boolean overflow = families.size() > MAX_PILLS - 1;
        List<Families.Family> shown = new ArrayList<>(
                overflow ? families.subList(0, MAX_PILLS - 2) : families);
        if (selectedId != 0 && indexOf(shown, selectedId) < 0) {
            Families.Family selected = find(families, selectedId);
            if (selected == null) selected = new Families.Family(selectedId, selectedName, 0);
            if (overflow) shown.set(shown.size() - 1, selected);
            else shown.add(selected);
        }
        for (Families.Family family : shown) {
            addChip(label(context, family), family.id == selectedId, () -> select(family.id, family.name));
        }
        if (overflow) addChip(context.getString(R.string.family_see_all), false, this::showAll);
    }

    private void addChip(String text, boolean checked, Runnable onClick) {
        Chip chip = (Chip) LayoutInflater.from(group.getContext())
                .inflate(R.layout.item_filter_chip, group, false);
        chip.setText(text);
        chip.setChecked(checked);
        chip.setOnClickListener(v -> group.post(onClick));
        group.addView(chip);
    }

    private void select(int familyId, String name) {
        boolean same = familyId == selectedId;
        selectedId = same ? 0 : familyId;
        selectedName = same ? "" : name;
        render();
        listener.onFamilyChanged(selectedId);
    }

    private void showAll() {
        Context context = group.getContext();
        String[] labels = new String[families.size() + 1];
        labels[0] = context.getString(R.string.family_all);
        for (int i = 0; i < families.size(); i++) labels[i + 1] = label(context, families.get(i));
        int checked = selectedId == 0 ? 0 : indexOf(families, selectedId) + 1;
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.family_dialog_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        if (selectedId != 0) select(selectedId, selectedName);
                        else render();
                    } else {
                        Families.Family family = families.get(which - 1);
                        if (family.id != selectedId) select(family.id, family.name);
                        else render();
                    }
                })
                .show();
        render();
    }

    private static String label(Context context, Families.Family family) {
        return context.getString(R.string.family_chip, family.name, family.count);
    }

    private static int indexOf(List<Families.Family> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) return i;
        }
        return -1;
    }

    private static Families.Family find(List<Families.Family> list, int id) {
        int index = indexOf(list, id);
        return index < 0 ? null : list.get(index);
    }
}
