package org.ranobe.ranobe.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentSettingsBinding;
import org.ranobe.ranobe.network.repository.GithubRepo;
import org.ranobe.ranobe.ui.settings.viewmodel.SettingsViewModel;
import org.ranobe.ranobe.ui.views.GetPro;
import org.ranobe.ranobe.worker.ChapterUpdateScheduler;

public class Settings extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    public Settings() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        enableNewChapterUpdates();
                    } else {
                        View root = getView();
                        if (root != null) {
                            Snackbar.make(root, "Notification permission denied", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        if (!Ranobe.isPro()) {
            binding.proApp.setVisibility(View.VISIBLE);
        }

        binding.themeModeOption.setOnClickListener(v -> binding.themeModeView.setVisibility(getPolarVisibility(binding.themeModeView)));
        binding.lightChip.setOnClickListener(v -> selectTheme(AppCompatDelegate.MODE_NIGHT_NO));
        binding.nightChip.setOnClickListener(v -> selectTheme(AppCompatDelegate.MODE_NIGHT_YES));
        binding.autoChip.setOnClickListener(v -> selectTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        binding.chapterUpdatesOption.setOnClickListener(v -> handleChapterUpdatesClick());
        syncChapterUpdatesToggle();

        binding.projectLink.setOnClickListener(v -> openLink(Ranobe.RANOBE_GITHUB_LINK));
        binding.musicPlayerLink.setOnClickListener(v -> openLink(Ranobe.MP_LITE_GITHUB_LINK));
        binding.discordLink.setOnClickListener(v -> openLink(Ranobe.DISCORD_INVITE_LINK));
        binding.ranobemLink.setOnClickListener(v -> openLink(Ranobe.RANOBE_M_LINK));
        binding.proApp.setOnClickListener(v -> openLink(Ranobe.RANOBE_PRO_APP_LINK));

        setCurrentThemeMode();

        viewModel.getUpdate().observe(requireActivity(), this::release);
        viewModel.checkForUpdate();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncChapterUpdatesToggle();
    }

    private void release(GithubRepo.GithubRelease release) {
        if (release.updateAvailable) {
            binding.updateCard.setVisibility(View.VISIBLE);
            binding.versionString.setText(String.format("A new version of the app is available %s", release.newReleaseVersion));
            binding.getUpdate.setOnClickListener(v -> openLink(release.newReleaseUrl));
        }
    }

    private void setCurrentThemeMode() {
        int mode = Ranobe.getThemeMode(requireActivity().getApplicationContext());
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)
            binding.themeModeOption.setIcon(R.drawable.ic_theme_mode_light);
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            binding.themeModeOption.setIcon(R.drawable.ic_theme_mode_night);
        else
            binding.themeModeOption.setIcon(R.drawable.ic_theme_mode_auto);
    }

    private int getPolarVisibility(View view) {
        int mode = view.getVisibility();
        return mode == View.VISIBLE ? View.GONE : View.VISIBLE;
    }

    private void selectTheme(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme);
        Ranobe.storeThemeMode(requireActivity().getApplicationContext(), theme);
    }

    private void syncChapterUpdatesToggle() {
        binding.chapterUpdatesOption.setChecked(Ranobe.isPro() && Ranobe.isNewChapterUpdatesEnabled());
    }

    private void handleChapterUpdatesClick() {
        if (!Ranobe.isPro()) {
            new GetPro().show(getParentFragmentManager(), GetPro.TAG);
            return;
        }
        boolean enabled = Ranobe.isNewChapterUpdatesEnabled();
        if (enabled) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Disable Notifications")
                    .setMessage("Disable new chapter notifications?")
                    .setPositiveButton("Disable", (dialog, i) -> {
                        Ranobe.setNewChapterUpdatesEnabled(false);
                        ChapterUpdateScheduler.cancel(requireContext());
                        syncChapterUpdatesToggle();
                        Snackbar.make(binding.getRoot(), "New chapter notifications disabled", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, i) -> dialog.dismiss())
                    .show();
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Enable Notifications")
                    .setMessage("Get notified when new chapters are available for novels in your library?")
                    .setPositiveButton("Enable", (dialog, i) -> requestNotificationPermissionAndEnable())
                    .setNegativeButton("Cancel", (dialog, i) -> dialog.dismiss())
                    .show();
        }
    }

    private void requestNotificationPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                enableNewChapterUpdates();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            enableNewChapterUpdates();
        }
    }

    private void enableNewChapterUpdates() {
        Ranobe.setNewChapterUpdatesEnabled(true);
        ChapterUpdateScheduler.schedule(requireContext());
        syncChapterUpdatesToggle();
        Snackbar.make(binding.getRoot(), "New chapter notifications enabled", Snackbar.LENGTH_SHORT).show();
    }

    private void openLink(String url) {
        requireActivity().startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
        ));
    }
}
