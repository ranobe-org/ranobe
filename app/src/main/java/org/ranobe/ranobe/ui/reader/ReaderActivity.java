package org.ranobe.ranobe.ui.reader;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.color.MaterialColors;
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
    // start loading the next chapter while this many paragraphs are still ahead
    private static final int PREFETCH_ROWS = 12;
    private static final long CHROME_ANIMATION_MS = 180;

    private ActivityReaderBinding binding;
    private PageAdapter adapter;
    private ReaderViewModel readerViewModel;
    private HistoryViewModel historyViewModel;
    private List<Chapter> chapterItems = new ArrayList<>();
    private ReadHistory readHistory;
    private String openedChapterUrl;
    // index in chapterItems of the last chapter appended to the page; -1 until the chapter list arrives
    private int lastLoadedIndex = -1;
    private String lastLoadedUrl;
    private String visibleChapterUrl;
    private boolean isLoading = false;
    private boolean endShown = false;
    private boolean chromeVisible = true;
    private LinearLayoutManager layoutManager;
    private boolean isVolumeKeyScroll = false;
    private int volumeScrollSpeed = Ranobe.DEFAULT_VOLUME_SCROLL_SPEED;
    private long lastVolumeScrollTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reader_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        @SuppressWarnings("deprecation")
        Novel currentNovel = getIntent().getParcelableExtra(Ranobe.KEY_NOVEL);
        @SuppressWarnings("deprecation")
        Chapter chapter = getIntent().getParcelableExtra(Ranobe.KEY_CHAPTER);
        @SuppressWarnings("deprecation")
        ReadHistory history = getIntent().getParcelableExtra(Ranobe.KEY_READ_HISTORY);
        readHistory = history;
        openedChapterUrl = chapter.url;
        readerViewModel = new ViewModelProvider(this).get(ReaderViewModel.class);
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        ChaptersViewModel chaptersViewModel = new ViewModelProvider(this).get(ChaptersViewModel.class);
        // chapter requests go through the current source, so pin it to the novel being read
        int sourceId = readHistory != null ? readHistory.sourceId : currentNovel != null ? currentNovel.sourceId : 0;
        if (sourceId > 0) RanobeSettings.get().setCurrentSource(sourceId).save();

        isVolumeKeyScroll = Ranobe.isVolumeKeyScrollEnabled();
        volumeScrollSpeed = Ranobe.getVolumeScrollSpeed();

        setUpChrome(currentNovel);
        setUpPageList();
        applyReaderTheme(Ranobe.themes.get(Ranobe.getReaderTheme(this)));

        // the opened chapter loads right away; the full list is only needed to find the next one
        isLoading = true;
        binding.progress.show();
        readerViewModel.getChapter(chapter).observe(this, this::setChapter);
        readerViewModel.getError().observe(this, this::setChapterError);
        chaptersViewModel.getChapters(currentNovel).observe(this, this::setChapters);
        chaptersViewModel.getError().observe(this, this::setError);
    }

    private void setUpChrome(Novel novel) {
        binding.customize.setOnMenuItemClickListener(this);
        binding.topBar.setNavigationOnClickListener(v -> finish());
        String novelName = novel != null && novel.name != null ? novel.name
                : readHistory != null ? readHistory.novelName : null;
        binding.topBar.setTitle(novelName);
    }

    private void setUpPageList() {
        adapter = new PageAdapter();
        layoutManager = new LinearLayoutManager(this);
        binding.pageList.setLayoutManager(layoutManager);
        binding.pageList.setAdapter(adapter);
        // theme/font changes rebind the visible rows; the default cross-fade on all of them at once glitches
        if (binding.pageList.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) binding.pageList.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        binding.pageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy != 0 && chromeVisible) setChromeVisible(false);
                updateVisibleChapter();
                if (dy > 0) maybeLoadNext();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!recyclerView.canScrollVertically(1)) loadNextChapter();
            }
        });

        // a single tap on the page toggles the bars; long presses stay free for text selection
        GestureDetectorCompat tapDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                setChromeVisible(!chromeVisible);
                return true;
            }
        });
        binding.pageList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                tapDetector.onTouchEvent(e);
                return false;
            }
        });
    }

    private void setChromeVisible(boolean visible) {
        chromeVisible = visible;
        animateBar(binding.topBar, visible, -1);
        animateBar(binding.customize, visible, 1);
    }

    private void animateBar(View bar, boolean visible, int direction) {
        bar.animate().cancel();
        if (visible) {
            bar.setVisibility(View.VISIBLE);
            bar.animate().translationY(0).alpha(1f).setDuration(CHROME_ANIMATION_MS).setListener(null).start();
        } else {
            float distance = direction * (bar.getHeight() > 0 ? bar.getHeight() : 200);
            bar.animate().translationY(distance).alpha(0f).setDuration(CHROME_ANIMATION_MS)
                    .withEndAction(() -> bar.setVisibility(View.INVISIBLE)).start();
        }
    }

    private void setChapters(List<Chapter> items) {
        chapterItems = ListUtils.sortById(items);
        lastLoadedIndex = indexOf(lastLoadedUrl != null ? lastLoadedUrl : openedChapterUrl);
        prefetchAfter(lastLoadedIndex);
        maybeLoadNext();
    }

    private int indexOf(String url) {
        if (url == null) return -1;
        for (int i = 0; i < chapterItems.size(); i++) {
            if (url.equals(chapterItems.get(i).url)) return i;
        }
        return -1;
    }

    private void setUpCustomizeReader() {
        CustomizeReader sheet = new CustomizeReader(this);
        sheet.show(getSupportFragmentManager(), "customize-sheet");
    }

    private void setChapter(Chapter chapter) {
        isLoading = false;
        binding.progress.hide();
        int start = adapter.appendChapter(chapter);
        lastLoadedUrl = chapter.url;
        if (!chapterItems.isEmpty()) lastLoadedIndex = indexOf(chapter.url);

        // reopening from history: return to the saved paragraph of the first chapter
        if (start == 0 && readHistory != null && chapter.url.equals(readHistory.url)) {
            int target = start + Math.max(0, Math.min(readHistory.position, adapter.chapterRowCount(start) - 1));
            layoutManager.scrollToPositionWithOffset(target, readHistory.readerOffset);
        }

        binding.pageList.post(this::updateVisibleChapter);
        prefetchAfter(lastLoadedIndex);
    }

    private void prefetchAfter(int index) {
        if (index >= 0 && index + 1 < chapterItems.size()) {
            readerViewModel.prefetch(chapterItems.get(index + 1));
        }
    }

    // tracks the chapter on screen: it's the one that gets marked read and whose position is saved
    private void updateVisibleChapter() {
        int position = layoutManager.findFirstVisibleItemPosition();
        Chapter chapter = adapter.chapterAt(position);
        if (chapter == null || chapter.url.equals(visibleChapterUrl)) return;
        visibleChapterUrl = chapter.url;
        binding.topBar.setSubtitle(chapter.name);
        historyViewModel.markAsRead(chapter);
    }

    private void maybeLoadNext() {
        if (adapter.getItemCount() == 0) return;
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible >= adapter.getItemCount() - PREFETCH_ROWS) loadNextChapter();
    }

    // pages only cover the screen once a chapter loads, so the window and loader must match the reader theme too
    private void applyReaderTheme(ReaderTheme theme) {
        int background = theme != null ? theme.getBackground()
                : MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSurface);
        int text = theme != null ? theme.getText()
                : MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurface);
        int indicator = theme != null ? theme.getText()
                : MaterialColors.getColor(binding.getRoot(), androidx.appcompat.R.attr.colorPrimary);
        getWindow().getDecorView().setBackgroundColor(background);
        binding.readerView.setBackgroundColor(background);
        binding.progress.setIndicatorColor(indicator);
        binding.progress.setTrackColor(ColorUtils.setAlphaComponent(indicator, 0x33));

        // bars sit on the page, so they take the page colors with a slight tint to stand apart
        int barColor = ColorUtils.blendARGB(background, text, 0.06f);
        for (Toolbar bar : new Toolbar[]{binding.topBar, binding.customize}) {
            bar.setBackgroundColor(barColor);
            bar.setTitleTextColor(text);
            bar.setSubtitleTextColor(ColorUtils.setAlphaComponent(text, 0xB3));
            Drawable navigation = bar.getNavigationIcon();
            if (navigation != null) navigation.mutate().setTint(text);
            for (int i = 0; i < bar.getMenu().size(); i++) {
                Drawable icon = bar.getMenu().getItem(i).getIcon();
                if (icon != null) icon.mutate().setTint(text);
            }
        }
    }

    private void setError(String msg) {
        if (msg == null || msg.isEmpty()) return;
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    // a failed chapter load used to leave the spinner running forever
    private void setChapterError(String msg) {
        if (msg == null || msg.isEmpty()) return;
        isLoading = false;
        binding.progress.hide();
        Snackbar.make(binding.getRoot(), R.string.chapter_load_failed, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> {
                    if (adapter.getItemCount() == 0) recreate();
                    else loadNextChapter();
                })
                .show();
    }

    @Override
    public void setFontSize(float size) {
        Ranobe.storeReaderFont(this, size);
        adapter.setFontSize(size);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void setReaderTheme(String themeName) {
        ReaderTheme theme = Ranobe.themes.get(themeName);
        applyReaderTheme(theme);
        adapter.setTheme(theme);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        Ranobe.storeReaderTheme(this, themeName);
    }

    @Override
    public void setBionicReading(boolean isBionicReading) {
        Ranobe.setBionicReader(this, isBionicReading);
        adapter.setBionicReading(isBionicReading);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void setVolumeKeyScroll(boolean isVolumeKeyScroll) {
        this.isVolumeKeyScroll = isVolumeKeyScroll;
        Ranobe.setVolumeKeyScroll(this, isVolumeKeyScroll);
    }

    @Override
    public void setVolumeScrollSpeed(int speed) {
        this.volumeScrollSpeed = speed;
        Ranobe.setVolumeScrollSpeed(this, speed);
    }

    private void scrollDown() {
        int scrollDistance = getScrollDistance();
        if (!binding.pageList.canScrollVertically(1)) {
            loadNextChapter();
        } else {
            binding.pageList.smoothScrollBy(0, scrollDistance, new LinearInterpolator(), getScrollDuration());
        }
    }

    private void scrollUp() {
        if (!binding.pageList.canScrollVertically(-1)) {
            return;
        }
        int scrollDistance = getScrollDistance();
        binding.pageList.smoothScrollBy(0, -scrollDistance, new LinearInterpolator(), getScrollDuration());
    }

    private int getScrollDistance() {
        int height = binding.pageList.getHeight();
        int effectiveHeight = (height > 0) ? height : getResources().getDisplayMetrics().heightPixels;
        int VOLUME_BTN_SCROLL_PERCENT = 60;
        return effectiveHeight * VOLUME_BTN_SCROLL_PERCENT / 100;
    }

    private int getScrollDuration() {
        switch (volumeScrollSpeed) {
            case 1:
                return 700;
            case 2:
                return 550;
            case 4:
                return 300;
            case 5:
                return 200;
            case 3:
            default:
                return 400;
        }
    }

    private void loadNextChapter() {
        if (isLoading || lastLoadedIndex < 0 || lastLoadedUrl == null) return;
        if (lastLoadedIndex + 1 < chapterItems.size()) {
            isLoading = true;
            binding.progress.show();
            readerViewModel.getChapter(chapterItems.get(lastLoadedIndex + 1)).observe(this, this::setChapter);
        } else if (!endShown) {
            endShown = true;
            adapter.appendEnd();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isVolumeKeyScroll) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            long throttle = Math.max(80, getScrollDuration() / 4);
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0 || System.currentTimeMillis() - lastVolumeScrollTime >= throttle) {
                        lastVolumeScrollTime = System.currentTimeMillis();
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            scrollDown();
                        } else {
                            scrollUp();
                        }
                    }
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isVolumeKeyScroll && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isVolumeKeyScroll && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVolumeKeyScroll = Ranobe.isVolumeKeyScrollEnabled();
        volumeScrollSpeed = Ranobe.getVolumeScrollSpeed();
    }

    public void setShowImages(boolean showImages) {
        Ranobe.setShowImages(this, showImages);
        adapter.setShowImages(showImages);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
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

    // saved on pause rather than destroy, which isn't guaranteed to run; stores the paragraph
    // within the chapter that's on screen, not the last chapter that happened to be loaded
    @Override
    protected void onPause() {
        super.onPause();
        if (layoutManager == null) return;
        int position = layoutManager.findFirstVisibleItemPosition();
        Chapter chapter = adapter.chapterAt(position);
        if (chapter == null) return;
        View view = layoutManager.findViewByPosition(position);
        int offset = (view != null) ? view.getTop() - binding.pageList.getPaddingTop() : 0;
        historyViewModel.updateReadHistoryPosition(adapter.indexInChapter(position), offset, chapter.url);
    }
}
