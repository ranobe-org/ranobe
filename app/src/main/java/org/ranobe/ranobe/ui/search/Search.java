package org.ranobe.ranobe.ui.search;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentSearchBinding;
import org.ranobe.ranobe.databinding.ItemSearchResultBinding;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;
import org.ranobe.ranobe.ui.views.SpacingDecorator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Search extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private List<DataSource> dataSources;

    private SearchResultAdapter resultAdapter;
    private LinkedHashMap<DataSource, List<Novel>> results;

    public Search() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        binding.searchView.setEndIconOnClickListener(v -> searchNovels());
        binding.resultList.setLayoutManager(new LinearLayoutManager(requireActivity()));

        results = new LinkedHashMap<>();
        resultAdapter = new SearchResultAdapter(results, this);
        binding.resultList.setAdapter(resultAdapter);

        HashMap<Integer, Class<?>> sources = SourceManager.getSources();
        dataSources = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            dataSources.add(src.metadata());
        }

        runSearch(viewModel.getFilter().getKeyword());
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
        results.clear();
        resultAdapter.notifyDataSetChanged();
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
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        controller.navigate(R.id.details_fragment, bundle);
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
        public SearchResultAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSearchResultBinding resultBinding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MyViewHolder(resultBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchResultAdapter.MyViewHolder holder, int position) {
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