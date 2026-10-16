package org.ranobe.ranobe.ui.downloads;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.DialogEpubExportBinding;
import org.ranobe.ranobe.export.ExportQueue;
import org.ranobe.ranobe.models.Novel;

import java.util.ArrayList;
import java.util.List;

/**
 * Starts an EPUB export: the user picks the chapters and grants what the export needs.
 * Create it in {@code onCreate}, it registers for permission results.
 */
public class EpubExportPrompt {
    private final Fragment fragment;
    private final ActivityResultLauncher<String[]> permissions;
    private Novel pendingNovel;
    private float pendingFrom;
    private float pendingTo;

    public EpubExportPrompt(Fragment fragment) {
        this.fragment = fragment;
        // the export runs either way, without these it just can't show progress or save to Downloads
        this.permissions = fragment.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result -> startPending());
    }

    private static float parse(CharSequence text) {
        if (text == null || text.toString().trim().isEmpty()) return Float.NaN;
        try {
            return Float.parseFloat(text.toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    public void show(Novel novel) {
        if (novel == null || !fragment.isAdded()) return;
        DialogEpubExportBinding binding = DialogEpubExportBinding.inflate(fragment.getLayoutInflater());
        binding.scope.setOnCheckedChangeListener((group, checked) ->
                binding.range.setVisibility(checked == R.id.scope_range ? View.VISIBLE : View.GONE));

        AlertDialog dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.export_dialog_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.export_start, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        // validate before closing, so a bad range can be fixed in place
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            float from = Float.NaN;
            float to = Float.NaN;
            if (binding.scope.getCheckedRadioButtonId() == R.id.scope_range) {
                from = parse(binding.from.getText());
                to = parse(binding.to.getText());
                if (!Float.isNaN(from) && !Float.isNaN(to) && from > to) {
                    binding.toLayout.setError(fragment.getString(R.string.export_invalid_range));
                    return;
                }
            }
            dialog.dismiss();
            start(novel, from, to);
        }));
        dialog.show();
    }

    private void start(Novel novel, float from, float to) {
        pendingNovel = novel;
        pendingFrom = from;
        pendingTo = to;
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        for (int i = missing.size() - 1; i >= 0; i--) {
            if (ContextCompat.checkSelfPermission(fragment.requireContext(), missing.get(i)) == PackageManager.PERMISSION_GRANTED) {
                missing.remove(i);
            }
        }
        if (missing.isEmpty()) {
            startPending();
        } else {
            permissions.launch(missing.toArray(new String[0]));
        }
    }

    private void startPending() {
        if (pendingNovel == null || !fragment.isAdded()) return;
        ExportQueue.get(fragment.requireContext()).enqueue(pendingNovel, pendingFrom, pendingTo);
        pendingNovel = null;
        View anchor = fragment.getView();
        if (anchor == null) return;
        if (!NotificationManagerCompat.from(fragment.requireContext()).areNotificationsEnabled()) {
            // it still runs, but its progress and result only show in the app
            Snackbar.make(anchor, R.string.export_started_no_notifications, Snackbar.LENGTH_LONG)
                    .setAction(R.string.export_open_settings, v -> openNotificationSettings())
                    .show();
            return;
        }
        Snackbar.make(anchor, R.string.export_started, Snackbar.LENGTH_LONG)
                .setAction(R.string.export_view, v -> fragment.startActivity(new Intent(fragment.requireContext(), DownloadsActivity.class)))
                .show();
    }

    private void openNotificationSettings() {
        String packageName = fragment.requireContext().getPackageName();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null));
        }
        fragment.startActivity(intent);
    }
}
