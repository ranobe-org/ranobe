package org.ranobe.ranobe.ui.reader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.ranobe.ranobe.databinding.FragmentReaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.reader.viewmodel.ReaderViewModel;

public class Reader extends Fragment {
    private String chapterUrl;
    private ReaderViewModel viewModel;
    private FragmentReaderBinding binding;

    public Reader() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterUrl = getArguments().getString("chapter");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(ReaderViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReaderBinding.inflate(inflater, container, false);

        Source source = SourceManager.getSource(1);
        viewModel.getChapter().observe(requireActivity(), this::setChapter);
        viewModel.chapter(source, chapterUrl);

        return binding.getRoot();
    }

    private void setChapter(Chapter chapter) {
        binding.content.setText(chapter.content);
        binding.progress.setVisibility(View.GONE);
    }
}