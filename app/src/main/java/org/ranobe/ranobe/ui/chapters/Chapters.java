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
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentChaptersBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.ui.chapters.adapter.ChapterAdapter;
import org.ranobe.ranobe.ui.chapters.viewmodel.ChaptersViewModel;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.ui.reader.ReaderActivity;
import org.ranobe.ranobe.util.ListUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Chapters extends BottomSheetDialogFragment implements ChapterAdapter.OnChapterItemClickListener, Toolbar.OnMenuItemClickListener {
    private final List<Chapter> originalItems = new ArrayList<>();
    private FragmentChaptersBinding binding;
    private ChaptersViewModel viewModel;
    private Novel novel;
    private ChapterAdapter adapter;

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
    }

    private void setUpObservers() {
        viewModel.getError().observe(getViewLifecycleOwner(), this::setUpError);
        viewModel.getChapters(novel).observe(getViewLifecycleOwner(), this::setChapter);
    }

    private void setUpUi() {
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        binding.searchField.addTextChangedListener(new SearchBarTextWatcher());

        adapter = new ChapterAdapter(originalItems, this);
        binding.chapterList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.chapterList.setAdapter(adapter);
    }

    private void setUpError(String error) {
        binding.progress.hide();
        if (originalItems.size() == 0) {
            Error.navigateToErrorFragment(requireActivity(), error);
        }
    }

    private void searchResults(String keyword) {
        if (keyword.length() > 0) {
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
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.sort) {
            sort();
        } else if (id == R.id.search) {
            setSearchView();
        }
        return true;
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