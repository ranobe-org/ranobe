package org.ranobe.ranobe.ui.details;

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

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentDetailsBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.ui.details.viewmodel.DetailsViewModel;
import org.ranobe.ranobe.ui.error.Error;

import java.util.List;

public class Details extends Fragment {
    private FragmentDetailsBinding binding;
    private DetailsViewModel viewModel;

    private Novel novel;

    public Details() {
        // Required
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novel = getArguments().getParcelable(Ranobe.KEY_NOVEL);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        setUpListeners();
        setUpObservers();
        return binding.getRoot();
    }

    private void setUpListeners() {
        binding.readChapter.setOnClickListener(v -> navigateToChapterList());
        binding.progress.show();
    }

    private void setUpObservers() {
        viewModel.getError().observe(requireActivity(), this::setUpError);
        viewModel.details(novel).observe(getViewLifecycleOwner(), this::setUpUi);
    }

    private void setUpError(String error) {
        binding.progress.hide();
        Error.navigateToErrorFragment(requireActivity(), error);
    }

    private void navigateToChapterList() {
        if(novel == null) return;
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, novel);
        controller.navigate(R.id.details_fragment_to_chapters, bundle);
    }

    private void setUpUi(Novel novel) {
        this.novel = novel;
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
}
