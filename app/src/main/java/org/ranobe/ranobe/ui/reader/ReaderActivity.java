package org.ranobe.ranobe.ui.reader;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.ranobe.ranobe.databinding.ActivityReaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.reader.viewmodel.ReaderViewModel;

public class ReaderActivity extends AppCompatActivity {
    private ActivityReaderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String chapterUrl = getIntent().getStringExtra("chapter");
        ReaderViewModel viewModel = new ViewModelProvider(this).get(ReaderViewModel.class);

        Source source = SourceManager.getSource(1);
        viewModel.getChapter().observe(this, this::setChapter);
        viewModel.chapter(source, chapterUrl);

    }

    private void setChapter(Chapter chapter) {
        binding.content.setText(chapter.content);
        binding.progress.setVisibility(View.GONE);
    }
}