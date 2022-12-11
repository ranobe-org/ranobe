package org.ranobe.ranobe.ui.chapters;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.ranobe.ranobe.databinding.FragmentChaptersBinding;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.ui.chapters.adapter.ChapterAdapter;
import org.ranobe.ranobe.ui.details.viewmodel.DetailsViewModel;
import org.ranobe.ranobe.ui.reader.ReaderActivity;

import java.util.List;

public class Chapters extends Fragment implements ChapterAdapter.OnChapterItemClickListener {
    private FragmentChaptersBinding binding;
    private DetailsViewModel viewModel;

    public Chapters() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChaptersBinding.inflate(inflater, container, false);
        binding.chapterList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        viewModel.getChapters().observe(requireActivity(), this::setChapter);

        return binding.getRoot();
    }

    private void setChapter(List<ChapterItem> chapters) {
        binding.chapterList.setAdapter(new ChapterAdapter(chapters, this));
        binding.progress.setVisibility(View.GONE);
    }

    @Override
    public void onChapterItemClick(ChapterItem item) {
        requireActivity().startActivity(
                new Intent(requireActivity(), ReaderActivity.class)
                        .putExtra("chapter", item.url)
        );
    }
}