package org.ranobe.ranobe.ui.reader;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ActivityReaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.ReaderTheme;
import org.ranobe.ranobe.ui.reader.adapter.PageAdapter;
import org.ranobe.ranobe.ui.reader.sheet.CustomizeReader;
import org.ranobe.ranobe.ui.reader.viewmodel.ReaderViewModel;
import org.ranobe.ranobe.util.ListUtils;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity implements CustomizeReader.OnOptionSelection {
    private ActivityReaderBinding binding;
    private List<ChapterItem> chapterItems = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private int currentChapterIndex;
    private String currentChapterUrl;
    private PageAdapter adapter;
    private boolean isLoading = false;
    private ReaderViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(Ranobe.getThemeMode(getApplicationContext()));
        setContentView(binding.getRoot());
        binding.customize.setOnClickListener(v-> setUpCustomizeReader());

        String novelUrl = getIntent().getStringExtra("novel");
        currentChapterUrl = getIntent().getStringExtra("currentChapter");
        viewModel = new ViewModelProvider(this).get(ReaderViewModel.class);

        adapter = new PageAdapter(chapters);
        binding.pageList.setLayoutManager(new LinearLayoutManager(this));
        binding.pageList.setAdapter(adapter);
        binding.pageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    isLoading = true;
                    currentChapterIndex += 1;

                    if (currentChapterIndex < chapterItems.size()) {
                        binding.progress.setVisibility(View.VISIBLE);
                        viewModel.chapter(chapterItems.get(currentChapterIndex).url);
                    }
                }
            }
        });

        viewModel.getChapters().observe(this, this::setChapters);
        viewModel.getChapter().observe(this, this::setChapter);
        viewModel.getError().observe(this, this::setError);
        viewModel.chapters(novelUrl);
    }

    private void setChapters(List<ChapterItem> items) {
        chapterItems = ListUtils.sortById(items);
        for(int i = 0; i < chapterItems.size(); i++) {
            if(chapterItems.get(i).url.equals(currentChapterUrl)) {
                currentChapterIndex = i;
                break;
            }
        }
        viewModel.chapter(currentChapterUrl);
    }

    private void setUpCustomizeReader() {
        CustomizeReader sheet = new CustomizeReader(this);
        sheet.show(getSupportFragmentManager(), "customize-sheet");
    }

    private void setChapter(Chapter chapter) {
        isLoading = false;
        binding.progress.setVisibility(View.GONE);
        chapter.id = chapterItems.get(currentChapterIndex).id;
        chapters.add(chapter);
        adapter.notifyItemInserted(chapters.size() - 1);
    }

    private void setError(String msg) {
        if (msg.length() == 0) return;
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void setFontSize(float size) {
        Ranobe.storeReaderFont(this, size);
        adapter.setFontSize(size);
        adapter.notifyItemRangeChanged(0, chapters.size());
    }

    @Override
    public void setReaderTheme(String themeName) {
        ReaderTheme theme = Ranobe.themes.get(themeName);
        if(theme != null) {
            adapter.setTheme(theme);
            adapter.notifyItemRangeChanged(0, chapters.size());
            Ranobe.storeReaderTheme(this, themeName);
        }
    }
}