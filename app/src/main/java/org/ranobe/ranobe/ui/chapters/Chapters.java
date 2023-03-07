package org.ranobe.ranobe.ui.chapters;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentChaptersBinding;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.services.download.DownloadService;
import org.ranobe.ranobe.ui.chapters.adapter.ChapterAdapter;
import org.ranobe.ranobe.ui.chapters.viewmodel.ChaptersViewModel;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.ui.reader.ReaderActivity;
import org.ranobe.ranobe.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Chapters extends Fragment implements ChapterAdapter.OnChapterItemClickListener, Toolbar.OnMenuItemClickListener {
    private final List<ChapterItem> originalItems = new ArrayList<>();
    private FragmentChaptersBinding binding;
    private ChaptersViewModel viewModel;
    private String novelUrl;
    private ChapterAdapter adapter;

    public Chapters() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novelUrl = getArguments().getString(Ranobe.KEY_NOVEL_URL);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(ChaptersViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChaptersBinding.inflate(inflater, container, false);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        binding.searchField.addTextChangedListener(new TextWatcher() {
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
        });

        adapter = new ChapterAdapter(originalItems, this);
        binding.chapterList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.chapterList.setAdapter(adapter);

        viewModel.getError().observe(requireActivity(), this::setUpError);
        viewModel.getChapters(novelUrl).observe(requireActivity(), this::setChapter);
        viewModel.chapters(novelUrl);

        return binding.getRoot();
    }

    private void setUpError(String error) {
        binding.progress.hide();
        if (originalItems.size() == 0) {
            Error.navigateToErrorFragment(requireActivity(), error);
        }
    }

    private void searchResults(String keyword) {
        if (keyword.length() > 0) {
            List<ChapterItem> filtered = ListUtils.searchByName(keyword.toLowerCase(), originalItems);
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
    private void setChapter(List<ChapterItem> chapters) {
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

    private void downloadChapters() {
        requireActivity().startService(new Intent(requireActivity(), DownloadService.class)
                .putExtra(Ranobe.KEY_SOURCE_ID, RanobeSettings.get().getCurrentSource())
                .putExtra(Ranobe.KEY_NOVEL_URL, novelUrl)
        );
    }

    private void downloadChapter(ChapterItem item) {
        Toast.makeText(requireContext(), String.format("Downloading chapter %s", item.id), Toast.LENGTH_SHORT).show();
        viewModel.chapter(novelUrl, item.url).observe(getViewLifecycleOwner(), chapter ->
                RanobeDatabase.databaseExecutor.execute(() -> {
                    chapter.name = item.name;
                    chapter.updated = item.updated;
                    chapter.id = item.id;
                    RanobeDatabase.database().chapters().save(chapter);
                }));
    }

    private void showDownloadAlert() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("Downloading all chapters will take some time depending on the no of chapters!")
                .setPositiveButton("Continue", (dialog, which) -> {
                    downloadChapters();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onChapterItemClick(ChapterItem item) {
        requireActivity().startActivity(
                new Intent(requireActivity(), ReaderActivity.class)
                        .putExtra("chapter", item.url)
                        .putExtra("novel", novelUrl)
                        .putExtra("currentChapter", item.url)
        );
    }

    @Override
    public void onDownloadChapterClick(ChapterItem item) {
        downloadChapter(item);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.sort) {
            sort();
        } else if (id == R.id.search) {
            setSearchView();
        } else if (id == R.id.download) {
            showDownloadAlert();
        }
        return true;
    }
}