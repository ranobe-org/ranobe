package org.ranobe.ranobe.ui.search;

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
import java.util.List;

public class Search extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private List<DataSource> dataSources;

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
        binding.submit.setOnClickListener(v -> searchNovels());
        binding.resultList.setLayoutManager(new LinearLayoutManager(requireActivity()));

        HashMap<Integer, Class<?>> sources = SourceManager.getSources();
        dataSources = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            dataSources.add(src.metadata());
        }

        return binding.getRoot();
    }

    private void searchNovels() {
        binding.searchField.clearFocus();
        if (binding.searchField.getText() != null) {
            String keyword = binding.searchField.getText().toString();

            binding.resultList.setAdapter(new SearchResultAdapter(
               dataSources,
               this,
               keyword
            ));
        }
    }

    @Override
    public void onNovelItemClick(Novel item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putString(Ranobe.KEY_NOVEL, item.url);
        controller.navigate(R.id.details_fragment, bundle);
    }

    public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.MyViewHolder> {
        private final List<DataSource> sources;
        private final NovelAdapter.OnNovelItemClickListener listener;
        private final Filter filter;

        public SearchResultAdapter(List<DataSource> sources, NovelAdapter.OnNovelItemClickListener listener, String keyword) {
            this.sources = sources;
            this.listener = listener;

            filter = new Filter();
            filter.addFilter(Filter.FILTER_KEYWORD, keyword);
        }

        @NonNull
        @Override
        public SearchResultAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSearchResultBinding resultBinding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MyViewHolder(resultBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchResultAdapter.MyViewHolder holder, int position) {
            DataSource source = sources.get(position);
            holder.binding.sourceName.setText(source.name);
            holder.binding.searchResults.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            holder.binding.searchResults.addItemDecoration(new SpacingDecorator(20));
            viewModel.search(source.sourceId, filter, 1).observe(requireActivity(), novels -> {
                if (novels.size() == 0) {
                    holder.binding.rootLayout.setVisibility(View.GONE);
                }

                NovelAdapter adapter = new NovelAdapter(novels, listener);
                holder.binding.searchResults.setAdapter(adapter);
            });
        }

        @Override
        public int getItemCount() {
            return sources.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            private final ItemSearchResultBinding binding;

            public MyViewHolder(@NonNull ItemSearchResultBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}