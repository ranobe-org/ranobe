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
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.database.models.ReadingList;
import org.ranobe.ranobe.databinding.ActivityReaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
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
    private List<Chapter> chapterItems = new ArrayList<>();
    private Chapter currentChapter;
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

        Novel currentNovel = getIntent().getParcelableExtra(Ranobe.KEY_NOVEL);
        currentChapter = getIntent().getParcelableExtra(Ranobe.KEY_CHAPTER);
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

                    markAsReadChapter(currentChapter);
                    if (currentChapterIndex < chapterItems.size()) {
                        binding.progress.show();
                        viewModel.getChapter(chapterItems.get(currentChapterIndex)).observe(ReaderActivity.this, chapter -> setChapter(chapter));
                    }
                }
            }
        });

        viewModel.getChapters(currentNovel).observe(this, this::setChapters);
        viewModel.getError().observe(this, this::setError);
    }

    private void setChapters(List<Chapter> items) {
        chapterItems = ListUtils.sortById(items);
        for (Chapter chapter : items) {
            if (chapter.url.equals(currentChapter.url)) {
                currentChapterIndex = chapterItems.indexOf(chapter);
            }
        }
        viewModel.getChapter(currentChapter).observe(ReaderActivity.this, this::setChapter);
    }

    private void setUpCustomizeReader() {
        CustomizeReader sheet = new CustomizeReader(this);
        sheet.show(getSupportFragmentManager(), "customize-sheet");
    }

    private void setChapter(Chapter chapter) {
        isLoading = false;
        binding.progress.hide();
        chapters.add(chapter);
        adapter.notifyItemInserted(chapters.size() - 1);
    }

    private void setError(String msg) {
        if (msg.length() == 0) return;
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    private void markAsReadChapter(Chapter chapter) {
        RanobeDatabase.databaseExecutor.execute(() -> {
            ReadingList existing = RanobeDatabase.database().readingList().get(chapter.novelUrl, chapter.url);
            if (existing != null) {
                RanobeDatabase.database().readingList().updateReadCount(chapter.url);
            } else {
                ReadingList readingList = new ReadingList(chapter.url, chapter.novelUrl);
                RanobeDatabase.database().readingList().save(readingList);
            }
        });
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
    public void setBionicReading(boolean isBionicReading) {
        adapter.setBionicReading(isBionicReading);
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.pageList.getLayoutManager();
        if (layoutManager == null) return;

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        for (int i = firstVisible; i <= lastVisible; i++) {
            adapter.notifyItemChanged(i);
        }
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