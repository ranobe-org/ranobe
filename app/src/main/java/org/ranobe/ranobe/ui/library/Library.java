package org.ranobe.ranobe.ui.library;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentLibraryBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.views.GetPro;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;
import org.ranobe.ranobe.util.ListUtils;
import org.ranobe.ranobe.util.NumberUtils;
import org.ranobe.ranobe.worker.ChapterUpdateScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Library extends Fragment implements NovelAdapter.OnNovelItemClickListener, NovelAdapter.OnNovelLongClickListener, MaterialToolbar.OnMenuItemClickListener {
    private static final int SORT_DEFAULT = 0;
    private static final int SORT_AZ = 1;
    private static final int SORT_ZA = 2;

    private FragmentLibraryBinding binding;
    private List<Novel> allNovels = new ArrayList<>();
    private int currentSort = SORT_DEFAULT;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    public Library() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentLibraryBinding.bind(view);

        binding.toolbar.setOnMenuItemClickListener(this);
        syncNewChaptersIcon();
        maybeShowChapterUpdatesBanner();

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                applyFilterAndSort(s.toString());
            }
        });

        DisplayUtils utils = new DisplayUtils(requireContext(), R.layout.item_novel);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), utils.noOfCols()));
        binding.novelList.addItemDecoration(new SpacingDecorator(utils.spacing()));

        RanobeDatabase.database().novels().list().observe(getViewLifecycleOwner(), novels -> {
            allNovels = novels;
            applyFilterAndSort(getCurrentQuery());
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.search) {
            toggleSearchView();
            return true;
        } else if (id == R.id.sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.new_chapters) {
            handleNewChaptersToggle();
            return true;
        }
        return false;
    }

    private void handleNewChaptersToggle() {
        if (!Ranobe.isPro()) {
            GetPro pro = new GetPro();
            pro.show(getParentFragmentManager(), GetPro.TAG);
            return;
        }

        boolean enabled = Ranobe.isNewChapterUpdatesEnabled();
        if (enabled) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Disable Notifications")
                    .setMessage("Disable new chapter notifications?")
                    .setPositiveButton("Disable", (dialog, i) -> disableNewChapterUpdates())
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
        Snackbar.make(binding.getRoot(), "New chapter notifications enabled", Snackbar.LENGTH_SHORT).show();
        syncNewChaptersIcon();
    }

    private void disableNewChapterUpdates() {
        Ranobe.setNewChapterUpdatesEnabled(false);
        ChapterUpdateScheduler.cancel(requireContext());
        Snackbar.make(binding.getRoot(), "New chapter notifications disabled", Snackbar.LENGTH_SHORT).show();
        syncNewChaptersIcon();
    }

    private void maybeShowChapterUpdatesBanner() {
        if (!Ranobe.isPro() || Ranobe.isChapterUpdateBannerShown() || Ranobe.isNewChapterUpdatesEnabled()) {
            return;
        }
        binding.chapterUpdatesBanner.setVisibility(View.VISIBLE);
        binding.enableUpdatesButton.setOnClickListener(v -> {
            Ranobe.setChapterUpdateBannerShown(true);
            binding.chapterUpdatesBanner.setVisibility(View.GONE);
            handleNewChaptersToggle();
        });
    }

    private void syncNewChaptersIcon() {
        MenuItem item = binding.toolbar.getMenu().findItem(R.id.new_chapters);
        if (item == null) return;
        boolean enabled = Ranobe.isPro() && Ranobe.isNewChapterUpdatesEnabled();
        item.setIcon(enabled ? R.drawable.ic_notifications_active : R.drawable.ic_disabled);
    }

    private void toggleSearchView() {
        int visibility = binding.searchView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        binding.searchView.setVisibility(visibility);
        if (visibility == View.GONE) {
            binding.searchInput.setText(null);
        }
    }

    private String getCurrentQuery() {
        Editable text = binding.searchInput.getText();
        return text != null ? text.toString() : "";
    }

    private void applyFilterAndSort(String query) {
        binding.progress.hide();
        List<Novel> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();

        for (Novel novel : allNovels) {
            if (lowerQuery.isEmpty() || (novel.name != null && novel.name.toLowerCase(Locale.getDefault()).contains(lowerQuery))) {
                filtered.add(novel);
            }
        }

        if (currentSort == SORT_AZ) {
            filtered = ListUtils.sortByName(filtered, true);
        } else if (currentSort == SORT_ZA) {
            filtered = ListUtils.sortByName(filtered, false);
        }

        if (filtered.isEmpty() && allNovels.isEmpty()) {
            showNoNovels();
        } else {
            binding.emoji.setText(null);
            binding.error.setText(null);
        }

        NovelAdapter adapter = new NovelAdapter(filtered, this, this);
        binding.novelList.setAdapter(adapter);
    }

    private void showSortDialog() {
        String[] options = {"Default", "A → Z", "Z → A"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort)
                .setSingleChoiceItems(options, currentSort, (dialog, which) -> {
                    currentSort = which;
                    applyFilterAndSort(getCurrentQuery());
                    dialog.dismiss();
                })
                .show();
    }

    private void showNoNovels() {
        binding.error.setText(R.string.no_novels_error);
        binding.emoji.setText(Ranobe.SILLY_EMOJI[NumberUtils.getRandom(Ranobe.SILLY_EMOJI.length)]);
    }

    @Override
    public void onNovelItemClick(Novel item) {
        RanobeSettings.get().setCurrentSource(item.sourceId).save();
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        controller.navigate(R.id.library_fragment_to_details, bundle);
    }

    @Override
    public void onNovelLongClick(Novel novel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to remove novel from the library?")
                .setPositiveButton("Yes", (dialog, i) -> removeFromLib(novel))
                .setNegativeButton("Cancel", (dialog, i) -> dialog.dismiss())
                .show();
    }

    private void removeFromLib(Novel novel) {
        RanobeDatabase.databaseExecutor.execute(() -> RanobeDatabase.database().novels().delete(novel.url));
        Snackbar.make(binding.getRoot(), "Removing novel from the library", Snackbar.LENGTH_LONG).show();
    }
}
