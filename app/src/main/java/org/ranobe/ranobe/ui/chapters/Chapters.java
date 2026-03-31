package org.ranobe.ranobe.ui.chapters;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentChaptersBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.service.DownloadService;
import org.ranobe.ranobe.ui.chapters.adapter.ChapterAdapter;
import org.ranobe.ranobe.ui.chapters.viewmodel.ChaptersViewModel;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.ui.history.viewmodel.HistoryViewModel;
import org.ranobe.ranobe.ui.reader.ReaderActivity;
import org.ranobe.ranobe.ui.views.RecyclerSwipeHelper;
import org.ranobe.ranobe.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Chapters extends BottomSheetDialogFragment implements ChapterAdapter.OnChapterItemClickListener, ChapterAdapter.OnChapterDownloadClickListener, Toolbar.OnMenuItemClickListener {
    private final List<Chapter> originalItems = new ArrayList<>();
    private final List<ReadHistory> readHistoryList = new ArrayList<>();
    private final Set<String> downloadedUrls = new HashSet<>();
    private FragmentChaptersBinding binding;
    private ChaptersViewModel viewModel;
    private HistoryViewModel historyViewModel;
    private Novel novel;
    private ChapterAdapter adapter;

    // Pending chapter/action to start after permission is granted
    private Chapter pendingDownloadChapter = null;
    private boolean pendingDownloadAll = false;

    private final ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    if (pendingDownloadAll) {
                        doDownloadAll();
                    } else if (pendingDownloadChapter != null) {
                        doEnqueue(pendingDownloadChapter);
                    }
                }
                pendingDownloadChapter = null;
                pendingDownloadAll = false;
            });

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String url = intent.getStringExtra(DownloadService.EXTRA_CHAPTER_URL);
            if (url != null && DownloadService.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                downloadedUrls.add(url);
                adapter.setDownloadedUrls(new HashSet<>(downloadedUrls));
            } else if (url != null) {
                // failed — just refresh to remove the pending spinner
                adapter.notifyDataSetChanged();
            }
        }
    };

    public Chapters() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novel = getArguments().getParcelable(Ranobe.KEY_NOVEL);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(ChaptersViewModel.class);
        historyViewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChaptersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpUi();
        setUpObservers();
        loadDownloadedUrls();
        registerDownloadReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireContext().unregisterReceiver(downloadReceiver);
    }

    private void registerDownloadReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadService.ACTION_DOWNLOAD_FAILED);
        ContextCompat.registerReceiver(requireContext(), downloadReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void loadDownloadedUrls() {
        RanobeDatabase.databaseExecutor.execute(() -> {
            List<String> urls = RanobeDatabase.database().chapters().getDownloadedUrls(novel.url);
            downloadedUrls.addAll(urls);
            requireActivity().runOnUiThread(() -> adapter.setDownloadedUrls(new HashSet<>(downloadedUrls)));
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setUpObservers() {
        viewModel.getError().observe(getViewLifecycleOwner(), this::setUpError);
        viewModel.getChapters(novel).observe(getViewLifecycleOwner(), this::setChapter);
        historyViewModel.getReadHistoriesByNovel(novel.url).observe(getViewLifecycleOwner(), readHistories -> {
            readHistoryList.clear();
            readHistoryList.addAll(readHistories);
            adapter.notifyDataSetChanged();
        });
    }

    private void setUpUi() {
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        binding.searchField.addTextChangedListener(new SearchBarTextWatcher());

        adapter = new ChapterAdapter(originalItems, readHistoryList, this);
        adapter.setDownloadListener(this);
        binding.chapterList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.chapterList.setAdapter(adapter);

        RecyclerSwipeHelper swipeHelper = new RecyclerSwipeHelper(
                getResources().getColor(R.color.success),
                getResources().getColor(R.color.danger),
                R.drawable.ic_mark_read,
                R.drawable.ic_mark_unread,
                requireContext()
        ) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                if (direction == ItemTouchHelper.LEFT) {
                    historyViewModel.markAsUnread(originalItems.get(viewHolder.getAdapterPosition()));
                } else {
                    historyViewModel.markAsRead(originalItems.get(viewHolder.getAdapterPosition()));
                }
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(swipeHelper);
        touchHelper.attachToRecyclerView(binding.chapterList);
    }

    private void setUpError(String error) {
        binding.progress.hide();
        if (originalItems.size() == 0) {
            Error.navigateToErrorFragment(requireActivity(), error);
        }
    }

    private void searchResults(String keyword) {
        if (!keyword.isEmpty()) {
            List<Chapter> filtered = ListUtils.searchByName(keyword.toLowerCase(), originalItems);
            ChapterAdapter searchAdapter = new ChapterAdapter(filtered, this);
            binding.chapterList.setAdapter(searchAdapter);
        } else {
            binding.chapterList.setAdapter(adapter);
        }
    }

    private void setSearchView() {
        int mode = binding.searchView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        binding.searchView.setVisibility(mode);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setChapter(List<Chapter> chapters) {
        originalItems.clear();
        originalItems.addAll(chapters);
        adapter.notifyDataSetChanged();
        binding.toolbar.setTitle(String.format(Locale.getDefault(), "%d Chapters", chapters.size()));
        binding.progress.hide();
    }

    private void sort() {
        Collections.reverse(originalItems);
        adapter.notifyItemRangeChanged(0, originalItems.size());
    }

    @Override
    public void onChapterItemClick(Chapter item) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, novel);
        bundle.putParcelable(Ranobe.KEY_CHAPTER, item);

        requireActivity().startActivity(new Intent(requireActivity(), ReaderActivity.class).putExtras(bundle));
    }

    @Override
    public void onDownloadClick(Chapter chapter) {
        if (hasNotifPermission()) {
            doEnqueue(chapter);
        } else {
            pendingDownloadChapter = chapter;
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void doEnqueue(Chapter chapter) {
        DownloadService.enqueue(requireContext(), chapter, novel.sourceId);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.sort) {
            sort();
        } else if (id == R.id.search) {
            setSearchView();
        } else if (id == R.id.download_all) {
            downloadAll();
        }
        return true;
    }

    private void downloadAll() {
        if (hasNotifPermission()) {
            doDownloadAll();
        } else {
            pendingDownloadAll = true;
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void doDownloadAll() {
        for (Chapter chapter : originalItems) {
            if (!downloadedUrls.contains(chapter.url)) {
                DownloadService.enqueue(requireContext(), chapter, novel.sourceId);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private boolean hasNotifPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public class SearchBarTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            searchResults(charSequence.toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}
