package org.ranobe.ranobe.ui.reader;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.databinding.ActivityReaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.models.ReaderTheme;
import org.ranobe.ranobe.ui.chapters.viewmodel.ChaptersViewModel;
import org.ranobe.ranobe.ui.history.viewmodel.HistoryViewModel;
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
    private ReaderViewModel readerViewModel;
    private HistoryViewModel historyViewModel;
    private List<Chapter> chapterItems = new ArrayList<>();
    private Chapter currentChapter;
    private ReadHistory readHistory;
    private boolean isLoading = false;
    private int currentChapterIndex;
    private LinearLayoutManager layoutManager;

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
        readHistory = getIntent().getParcelableExtra(Ranobe.KEY_READ_HISTORY);
        readerViewModel = new ViewModelProvider(this).get(ReaderViewModel.class);
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        ChaptersViewModel chaptersViewModel = new ViewModelProvider(this).get(ChaptersViewModel.class);
        if(readHistory!=null) RanobeSettings.get().setCurrentSource(readHistory.sourceId).save();

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
                        readerViewModel.getChapter(chapterItems.get(currentChapterIndex)).observe(ReaderActivity.this, chapter -> setChapter(chapter));
                    }
                }
            }
        });
        layoutManager = (LinearLayoutManager) binding.pageList.getLayoutManager();

        chaptersViewModel.getChapters(currentNovel).observe(this, this::setChapters);
        chaptersViewModel.getError().observe(this, this::setError);
    }

    private void setChapters(List<Chapter> items) {
        chapterItems = ListUtils.sortById(items);
        for (Chapter chapter : items) {
            if (chapter.url.equals(currentChapter.url)) {
                currentChapterIndex = chapterItems.indexOf(chapter);
            }
        }
        readerViewModel.getChapter(currentChapter).observe(ReaderActivity.this, this::setChapter);
    }

    private void setUpCustomizeReader() {
        CustomizeReader sheet = new CustomizeReader(this);
        sheet.show(getSupportFragmentManager(), "customize-sheet");
    }

    private void setChapter(Chapter chapter) {
        isLoading = false;
        binding.progress.hide();
        chapters.add(chapter);
        currentChapter = chapter;

        if (layoutManager == null) return;
        int scrollPosition = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleView = layoutManager.findViewByPosition(scrollPosition);
        int scrollOffset = (firstVisibleView != null) ? firstVisibleView.getTop() : 0;

        adapter.notifyItemInserted(chapters.size() - 1);

        if (readHistory != null && binding.pageList.getAdapter() != null) {
            binding.pageList.post(() -> {
                if (binding.pageList.getAdapter().getItemCount() == 1)
                    layoutManager.scrollToPositionWithOffset(0, readHistory.readerOffset);
                else layoutManager.scrollToPositionWithOffset(scrollPosition, scrollOffset);
            });
        }

        historyViewModel.markAsRead(currentChapter);
    }

    private void setError(String msg) {
        if (msg.isEmpty()) return;
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
    public void setBionicReading(boolean isBionicReading) {
        Ranobe.setBionicReader(this,isBionicReading);
        adapter.setBionicReading(isBionicReading);
        if (layoutManager == null) return;

        int scrollPosition = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleView = layoutManager.findViewByPosition(scrollPosition);
        int scrollOffset = (firstVisibleView != null) ? firstVisibleView.getTop() : 0;
        adapter.notifyItemChanged(scrollPosition);
        binding.pageList.post(() -> layoutManager.scrollToPositionWithOffset(scrollPosition, scrollOffset));
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

    @Override
    protected void onDestroy() {
        if (layoutManager != null && currentChapter != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            View view = layoutManager.findViewByPosition(position);
            int offset = (view != null) ? view.getTop() : 0;
            historyViewModel.updateReadHistoryPosition(position, offset, currentChapter.url);
        }
        super.onDestroy();

    }
}