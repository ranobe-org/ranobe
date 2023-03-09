package org.ranobe.ranobe.ui.details;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentDetailsBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.services.download.DownloadService;
import org.ranobe.ranobe.ui.details.viewmodel.DetailsViewModel;
import org.ranobe.ranobe.ui.error.Error;

import java.util.List;

public class Details extends Fragment {
    private FragmentDetailsBinding binding;
    private DetailsViewModel viewModel;

    private String novelUrl;
    private Long novelId;

    public Details() {
        // Required
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novelUrl = getArguments().getString(Ranobe.KEY_NOVEL_URL);
            novelId = getArguments().getLong(Ranobe.KEY_NOVEL_ID, -1L);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        setUpListeners();
        checkDatabase();
        return binding.getRoot();
    }

    private void setUpListeners() {
        binding.readChapter.setOnClickListener(v -> navigateToChapterList());
        binding.addToLibrary.setOnClickListener(v -> saveNovelToLibrary());
        binding.progress.show();
    }

    private void checkDatabase() {
        RanobeDatabase.database().novels().get(novelId).observe(getViewLifecycleOwner(), novel -> {
            if (novel == null) {
                setUpObservers();
            } else {
                setUpUi(novel);
            }
        });
    }

    private void setUpObservers() {
        viewModel.getError().observe(requireActivity(), this::setUpError);
        viewModel.details(novelUrl).observe(getViewLifecycleOwner(), this::setUpUi);
    }

    private void setUpError(String error) {
        binding.progress.hide();
        Error.navigateToErrorFragment(requireActivity(), error);
    }

    private void navigateToChapterList() {
        if (novelUrl == null) return;
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putString(Ranobe.KEY_NOVEL_URL, novelUrl);
        bundle.putString(Ranobe.KEY_FROM_PAGE, Ranobe.VAL_PAGE_LIB);
        controller.navigate(R.id.details_fragment_to_chapters, bundle);
    }

    private void setUpUi(Novel novel) {
        Glide.with(binding.novelCover.getContext()).load(novel.cover).into(binding.novelCover);
        binding.novelName.setText(novel.name);
        binding.rating.setRating(novel.rating);
        binding.summary.setText(novel.summary);
        binding.status.setText(novel.status);
        addChips(novel.genres);

        if (novel.authors != null) {
            binding.authors.setText(String.join(", ", novel.authors));
        }
        if (novel.alternateNames != null) {
            binding.alternativeNames.setText(String.join(", ", novel.alternateNames));
        }
        if (novel.year > 0) {
            binding.year.setText(String.valueOf(novel.year));
        }

        binding.progress.hide();
    }

    private void addChips(List<String> genres) {
        if (genres == null) return;

        binding.genresLayout.removeAllViews();
        for (String genre : genres) {
            Chip chip = new Chip(binding.genresLayout.getContext());
            chip.setText(genre);
            binding.genresLayout.addView(chip);
        }
    }

    private void saveNovelToLibrary() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("Downloading novel and all chapters will take some time depending on the number of chapters!")
                .setPositiveButton("Continue", (dialog, which) -> {
                    Snackbar.make(binding.getRoot(), "Downloading started...", Snackbar.LENGTH_SHORT).show();
                    addNovelToDatabase();
                    downloadChapters();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addNovelToDatabase() {
        viewModel.details(novelUrl).observe(getViewLifecycleOwner(), novel ->
                RanobeDatabase.databaseExecutor.execute(() ->
                        RanobeDatabase.database().novels().save(novel)));
    }

    private void downloadChapters() {
        requireActivity().startService(new Intent(requireActivity(), DownloadService.class)
                .putExtra(Ranobe.KEY_SOURCE_ID, RanobeSettings.get().getCurrentSource())
                .putExtra(Ranobe.KEY_NOVEL_URL, novelUrl)
        );
    }
}
