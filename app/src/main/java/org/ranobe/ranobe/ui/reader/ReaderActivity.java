package org.ranobe.ranobe.ui.reader;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
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

public class ReaderActivity extends AppCompatActivity implements CustomizeReader.OnOptionSelection, Toolbar.OnMenuItemClickListener {
    private final List<Chapter> chapters = new ArrayList<>();
    private ActivityReaderBinding binding;
    private PageAdapter adapter;
    private ReaderViewModel viewModel;
    private List<ChapterItem> chapterItems = new ArrayList<>();
    private String currentChapterUrl;
    private String currentNovelUrl;
    private boolean isLoading = false;
    private int currentChapterIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(Ranobe.getThemeMode(getApplicationContext()));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(binding.getRoot());
        binding.customize.setOnMenuItemClickListener(this);

        currentNovelUrl = getIntent().getStringExtra(Ranobe.KEY_NOVEL_URL);
        currentChapterUrl = getIntent().getStringExtra(Ranobe.KEY_CHAPTER_URL);
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
                        binding.progress.show();
                        viewModel.chapter(currentNovelUrl, chapterItems.get(currentChapterIndex).url);
                    }
                }
            }
        });

        viewModel.getChapters().observe(this, this::setChapters);
        viewModel.getChapter().observe(this, this::setChapter);
        viewModel.getError().observe(this, this::setError);
        viewModel.chapters(currentNovelUrl);
    }

    private void setChapters(List<ChapterItem> items) {
        chapterItems = ListUtils.sortById(items);
        for (int i = 0; i < chapterItems.size(); i++) {
            if (chapterItems.get(i).url.equals(currentChapterUrl)) {
                currentChapterIndex = i;
                break;
            }
        }
        viewModel.chapter(currentNovelUrl, currentChapterUrl);
    }

    private void setUpCustomizeReader() {
        CustomizeReader sheet = new CustomizeReader(this);
        sheet.show(getSupportFragmentManager(), "customize-sheet");
    }

    private void setChapter(Chapter chapter) {
        isLoading = false;
        binding.progress.hide();
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
        adapter.setTheme(theme);
        adapter.notifyItemRangeChanged(0, chapters.size());
        Ranobe.storeReaderTheme(this, themeName);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.customize_settings) {
            setUpCustomizeReader();
            return true;
        }

        return false;
    }
}