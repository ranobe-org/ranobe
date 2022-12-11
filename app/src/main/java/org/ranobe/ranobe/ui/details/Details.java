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

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.FragmentDetailsBinding;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.details.viewmodel.DetailsViewModel;

import java.util.List;
import java.util.Locale;

public class Details extends Fragment {
    private String novelUrl;
    private FragmentDetailsBinding binding;
    private DetailsViewModel viewModel;

    public Details() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novelUrl = getArguments().getString("novel");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        binding.progress.setVisibility(View.VISIBLE);
        binding.chapterInfo.setOnClickListener(v -> navigateToChapterList());

        Source source = SourceManager.getSource(1);
        viewModel.getDetails().observe(requireActivity(), this::setupUi);
        viewModel.getChapters().observe(requireActivity(), this::setupChapters);
        viewModel.details(source, novelUrl);
        viewModel.chapters(source, novelUrl);

        return binding.getRoot();
    }

    private void setupChapters(List<ChapterItem> items) {
        binding.chapterCount.setText(String.format(Locale.getDefault(), "%d total chapters", items.size()));
    }

    private void navigateToChapterList() {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putString("novel", novelUrl);
        controller.navigate(R.id.chapters_fragment, bundle);
    }

    private void setupUi(Novel novel) {
        Glide.with(binding.novelCover.getContext()).load(novel.cover).into(binding.novelCover);
        binding.novelName.setText(novel.name);
        binding.rating.setRating(novel.rating);
        binding.summary.setText(novel.summary);

        if (novel.authors != null) {
            binding.authors.setText(String.join(", ", novel.authors));
            binding.authorLayout.setVisibility(View.VISIBLE);
        }
        if (novel.alternateNames != null) {
            binding.alternativeNames.setText(String.join(", ", novel.alternateNames));
            binding.alternativeNamesLayout.setVisibility(View.VISIBLE);
        }
        if (novel.genres != null) {
            binding.genres.setText(String.join(", ", novel.genres));
            binding.genresLayout.setVisibility(View.VISIBLE);
        }
        if (novel.year > 0) {
            binding.year.setText(novel.year);
            binding.yearLayout.setVisibility(View.VISIBLE);
        }
        if (novel.status != null) {
            binding.status.setText(novel.status);
            binding.statusLayout.setVisibility(View.VISIBLE);
        }

        binding.summaryInfo.setVisibility(View.VISIBLE);
        binding.detailsInfo.setVisibility(View.VISIBLE);
        binding.progress.setVisibility(View.GONE);
    }
}