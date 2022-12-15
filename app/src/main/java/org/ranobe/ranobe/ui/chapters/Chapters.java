package org.ranobe.ranobe.ui.chapters;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.FragmentChaptersBinding;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.chapters.adapter.ChapterAdapter;
import org.ranobe.ranobe.ui.chapters.viewmodel.ChaptersViewModel;
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
            novelUrl = getArguments().getString("novel");
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
        binding.chapterList.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL));
        binding.chapterList.setAdapter(adapter);

        viewModel.getChapters(novelUrl).observe(getViewLifecycleOwner(), this::setChapter);
        viewModel.chapters(SourceManager.getSource(1), novelUrl);

        return binding.getRoot();
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

    private void setChapter(List<ChapterItem> chapters) {
        originalItems.addAll(chapters);
        adapter.notifyItemRangeInserted(0, chapters.size());
        binding.toolbar.setTitle(String.format(Locale.getDefault(), "%d Chapters", chapters.size()));
        binding.progress.setVisibility(View.GONE);
    }

    private void sort() {
        Collections.reverse(originalItems);
        adapter.notifyItemRangeChanged(0, originalItems.size());
    }

    @Override
    public void onChapterItemClick(ChapterItem item) {
        requireActivity().startActivity(
                new Intent(requireActivity(), ReaderActivity.class)
                        .putExtra("chapter", item.url)
        );
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if(id == R.id.sort) {
            sort();
        }
        else if (id == R.id.search) {
            setSearchView();
        }
        return true;
    }
}