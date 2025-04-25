package org.ranobe.downloader.ui.search;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.core.models.DataSource;
import org.ranobe.core.models.Filter;
import org.ranobe.core.models.Novel;
import org.ranobe.core.sources.Source;
import org.ranobe.core.sources.SourceManager;
import org.ranobe.downloader.config.Config;
import org.ranobe.downloader.databinding.FragmentSearchBinding;
import org.ranobe.downloader.databinding.ItemSearchResultBinding;
import org.ranobe.downloader.ui.download.DownloadActivity;
import org.ranobe.downloader.ui.search.adapter.NovelAdapter;
import org.ranobe.downloader.ui.search.viewmodel.SearchViewModel;
import org.ranobe.downloader.ui.views.SpacingDecorator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Search extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private List<DataSource> dataSources;
    private String keyword;

    private SearchResultAdapter resultAdapter;
    private LinkedHashMap<DataSource, List<Novel>> results;

    public Search() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        if (getArguments() != null) {
            keyword = getArguments().getString(Config.KEY_KEYWORD);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        binding.searchView.setEndIconOnClickListener(v -> searchNovels());
        binding.resultList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.searchField.setText(keyword);
        results = new LinkedHashMap<>();
        resultAdapter = new SearchResultAdapter(results, this);
        binding.resultList.setAdapter(resultAdapter);

        Map<Integer, Class<?>> sources = SourceManager.getSources();
        dataSources = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            dataSources.add(src.metadata());
        }

        runSearch(viewModel.getFilter().getKeyword());
        if (keyword != null && keyword.length() > 0) {
            runSearch(keyword);
        }

        return binding.getRoot();
    }

    private void searchNovels() {
        binding.searchField.clearFocus();
        if (binding.searchField.getText() != null && binding.searchField.getText().toString().length() > 0) {
            String keyword = binding.searchField.getText().toString();
            runSearch(keyword);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void runSearch(String keyword) {
        if (keyword == null || keyword.length() == 0) return;
        binding.progress.show();

        Filter filter = new Filter();
        filter.addFilter(Filter.FILTER_KEYWORD, keyword);
        viewModel.search(dataSources, filter, 1).observe(getViewLifecycleOwner(), result -> {
            results.clear();
            results.putAll(result);
            resultAdapter.notifyDataSetChanged();
            binding.progress.hide();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> binding.progress.hide());
    }

    @Override
    public void onNovelItemClick(Novel item) {
        startActivity(new Intent(requireActivity(), DownloadActivity.class)
                .putExtra(Config.KEY_URL, item.url));
    }

    public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.MyViewHolder> {
        private final Map<DataSource, List<Novel>> results;
        private final NovelAdapter.OnNovelItemClickListener listener;
        private final SpacingDecorator spacingDecorator = new SpacingDecorator(10);

        public SearchResultAdapter(Map<DataSource, List<Novel>> results, Search listener) {
            this.results = results;
            this.listener = listener;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSearchResultBinding resultBinding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MyViewHolder(resultBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            DataSource source = (DataSource) results.keySet().toArray()[position];
            holder.binding.sourceName.setText(source.name);

            List<Novel> novels = results.get(source);
            if (novels == null) return;
            if (!novels.isEmpty()) {
                holder.binding.rootLayout.setVisibility(View.VISIBLE);
            }

            NovelAdapter adapter = new NovelAdapter(novels, listener);
            holder.binding.searchResults.setAdapter(adapter);
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            private final ItemSearchResultBinding binding;

            public MyViewHolder(@NonNull ItemSearchResultBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.searchResults.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
                binding.searchResults.addItemDecoration(spacingDecorator);
            }
        }
    }
}