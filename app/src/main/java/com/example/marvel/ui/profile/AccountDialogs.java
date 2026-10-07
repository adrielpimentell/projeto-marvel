package com.example.marvel.ui.profile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.example.marvel.data.auth.AuthRepository;
import com.example.marvel.data.auth.PlayerName;
import com.example.marvel.game.OwnedHero;
import com.example.marvel.game.PlayerStore;
import com.example.marvel.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("InflateParams")
final class AccountDialogs {

    private static final int AVATAR_COLUMNS = 3;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private AccountDialogs() {
    }

    static void showAvatarPicker(Activity activity, Runnable onChanged) {
        AuthRepository auth = AuthRepository.getInstance(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_avatar_picker, null, false);
        List<OwnedHero> heroes = new ArrayList<>();
        for (OwnedHero hero : PlayerStore.getInstance(activity).get().getHeroes()) {
            if (hero.hasInfo() && hero.getImageUrl() != null) heroes.add(hero);
        }
        view.findViewById(R.id.avatar_empty).setVisibility(heroes.isEmpty() ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.avatar_title)
                .setView(view)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        RecyclerView grid = view.findViewById(R.id.avatar_grid);
        grid.setLayoutManager(new GridLayoutManager(activity, AVATAR_COLUMNS));
        grid.setAdapter(new AvatarChoiceAdapter(auth.cachedPlayerName(), heroes, auth.cachedAvatarHeroId(),
                heroId -> {
                    auth.setAvatar(heroId);
                    dialog.dismiss();
                    onChanged.run();
                }));
        dialog.show();
    }

    static void showRename(Activity activity, Runnable onChanged) {
        AuthRepository auth = AuthRepository.getInstance(activity);
        String current = auth.cachedPlayerName();
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_change_name, null, false);
        ((TextView) view.findViewById(R.id.rename_current)).setText(
                activity.getString(R.string.rename_current, current == null ? "" : current));
        TextInputLayout layout = view.findViewById(R.id.rename_input_layout);
        EditText input = view.findViewById(R.id.rename_input);
        TextView error = view.findViewById(R.id.dialog_error);
        View progress = view.findViewById(R.id.dialog_progress);
        clearOnEdit(error, layout);

        AlertDialog dialog = form(activity, R.string.account_change_name, view, R.string.action_save);
        dialog.setOnShowListener(shown -> positive(dialog).setOnClickListener(v -> {
            String name = PlayerName.clean(text(input));
            if (!PlayerName.isValid(name)) {
                layout.setError(activity.getString(name.isEmpty()
                        ? R.string.auth_error_required : R.string.auth_error_name_format));
                return;
            }
            if (name.equals(current)) {
                layout.setError(activity.getString(R.string.rename_same));
                return;
            }
            setBusy(dialog, progress, true);
            auth.renamePlayer(name, new AuthRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    if (activity.isFinishing()) return;
                    dialog.dismiss();
                    onChanged.run();
                    snack(activity, activity.getString(R.string.rename_done, name));
                }

                @Override
                public void onError(int messageRes) {
                    if (activity.isFinishing()) return;
                    setBusy(dialog, progress, false);
                    if (messageRes == R.string.auth_error_name_taken) {
                        layout.setError(activity.getString(messageRes));
                    } else {
                        showError(error, messageRes);
                    }
                }
            });
        }));
        dialog.show();
    }

    static void showChangePassword(Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_change_password, null, false);
        TextInputLayout currentLayout = view.findViewById(R.id.password_current_layout);
        TextInputLayout newLayout = view.findViewById(R.id.password_new_layout);
        TextInputLayout confirmLayout = view.findViewById(R.id.password_confirm_layout);
        EditText current = view.findViewById(R.id.password_current);
        EditText fresh = view.findViewById(R.id.password_new);
        EditText confirm = view.findViewById(R.id.password_confirm);
        TextView error = view.findViewById(R.id.dialog_error);
        View progress = view.findViewById(R.id.dialog_progress);
        clearOnEdit(error, currentLayout, newLayout, confirmLayout);

        AlertDialog dialog = form(activity, R.string.account_change_password, view, R.string.action_save);
        dialog.setOnShowListener(shown -> positive(dialog).setOnClickListener(v -> {
            String currentText = raw(current);
            String newText = raw(fresh);
            boolean ok = true;
            if (currentText.isEmpty()) {
                currentLayout.setError(activity.getString(R.string.auth_error_required));
                ok = false;
            }
            if (newText.length() < MIN_PASSWORD_LENGTH) {
                newLayout.setError(activity.getString(R.string.auth_error_weak_password));
                ok = false;
            }
            if (!newText.equals(raw(confirm))) {
                confirmLayout.setError(activity.getString(R.string.auth_error_password_mismatch));
                ok = false;
            }
            if (!ok) return;
            setBusy(dialog, progress, true);
            AuthRepository.getInstance(activity).changePassword(currentText, newText,
                    new AuthRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            if (activity.isFinishing()) return;
                            dialog.dismiss();
                            snack(activity, activity.getString(R.string.password_done));
                        }

                        @Override
                        public void onError(int messageRes) {
                            if (activity.isFinishing()) return;
                            setBusy(dialog, progress, false);
                            if (messageRes == R.string.account_wrong_password) {
                                currentLayout.setError(activity.getString(messageRes));
                            } else if (messageRes == R.string.auth_error_weak_password) {
                                newLayout.setError(activity.getString(messageRes));
                            } else {
                                showError(error, messageRes);
                            }
                        }
                    });
        }));
        dialog.show();
    }

    static void showSignOut(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.account_sign_out_title)
                .setMessage(R.string.account_sign_out_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.account_sign_out, (dialog, which) -> {
                    AuthRepository.getInstance(activity).signOut();
                    activity.startActivity(LoginActivity.newSignedOutIntent(activity));
                    activity.finish();
                })
                .show();
    }

    static void showDelete(Activity activity) {
        AuthRepository auth = AuthRepository.getInstance(activity);
        String playerName = auth.cachedPlayerName() == null ? "" : auth.cachedPlayerName();
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_account, null, false);
        ((TextView) view.findViewById(R.id.delete_type_name)).setText(
                activity.getString(R.string.delete_type_name, playerName));
        TextInputLayout nameLayout = view.findViewById(R.id.delete_name_layout);
        TextInputLayout passwordLayout = view.findViewById(R.id.delete_password_layout);
        EditText name = view.findViewById(R.id.delete_name);
        EditText password = view.findViewById(R.id.delete_password);
        TextView error = view.findViewById(R.id.dialog_error);
        View progress = view.findViewById(R.id.dialog_progress);
        clearOnEdit(error, nameLayout, passwordLayout);

        AlertDialog dialog = form(activity, R.string.account_delete, view, R.string.delete_confirm);
        Runnable updateButton = () -> {
            Button button = positive(dialog);
            if (button == null) return;
            button.setEnabled(!playerName.isEmpty()
                    && PlayerName.clean(text(name)).equalsIgnoreCase(playerName)
                    && !raw(password).isEmpty());
        };
        TextWatcher watcher = new SimpleWatcher(updateButton);
        name.addTextChangedListener(watcher);
        password.addTextChangedListener(watcher);

        dialog.setOnShowListener(shown -> {
            Button button = positive(dialog);
            button.setTextColor(activity.getColor(R.color.red));
            updateButton.run();
            button.setOnClickListener(v -> {
                if (!PlayerName.clean(text(name)).equalsIgnoreCase(playerName)) {
                    nameLayout.setError(activity.getString(R.string.delete_name_mismatch));
                    return;
                }
                setBusy(dialog, progress, true);
                auth.deleteAccount(raw(password), new AuthRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        dialog.dismiss();
                        Toast.makeText(activity, R.string.delete_done, Toast.LENGTH_LONG).show();
                        activity.startActivity(LoginActivity.newSignedOutIntent(activity));
                        activity.finish();
                    }

                    @Override
                    public void onError(int messageRes) {
                        if (activity.isFinishing()) return;
                        setBusy(dialog, progress, false);
                        updateButton.run();
                        if (messageRes == R.string.account_wrong_password) {
                            passwordLayout.setError(activity.getString(messageRes));
                        } else {
                            showError(error, messageRes);
                        }
                    }
                });
            });
        });
        dialog.show();
    }

    private static AlertDialog form(Activity activity, int titleRes, View view, int positiveRes) {
        return new MaterialAlertDialogBuilder(activity)
                .setTitle(titleRes)
                .setView(view)
                .setPositiveButton(positiveRes, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
    }

    private static Button positive(AlertDialog dialog) {
        return dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    }

    private static void setBusy(AlertDialog dialog, View progress, boolean busy) {
        dialog.setCancelable(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        positive(dialog).setEnabled(!busy);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(!busy);
    }

    private static void showError(TextView error, int messageRes) {
        error.setText(messageRes);
        error.setVisibility(View.VISIBLE);
    }

    private static void clearOnEdit(TextView error, TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            EditText input = layout.getEditText();
            if (input == null) continue;
            input.addTextChangedListener(new SimpleWatcher(() -> {
                layout.setError(null);
                error.setVisibility(View.GONE);
            }));
        }
    }

    private static void snack(Activity activity, String message) {
        Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private static String text(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static String raw(EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static final class SimpleWatcher implements TextWatcher {

        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }
}
