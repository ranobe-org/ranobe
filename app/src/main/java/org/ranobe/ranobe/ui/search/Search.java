package org.ranobe.ranobe.ui.search;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;
import org.ranobe.ranobe.ui.views.SpacingDecorator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Search extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private final List<Map.Entry<DataSource, List<Novel>>> results = new ArrayList<>();
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private SearchResultAdapter resultAdapter;

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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.searchView.setEndIconOnClickListener(v -> searchNovels());
        binding.searchField.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            // a hardware enter sends both down and up; only search once
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (enter && event.getAction() == KeyEvent.ACTION_DOWN)) {
                searchNovels();
                return true;
            }
            return enter;
        });

        binding.resultList.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultAdapter = new SearchResultAdapter();
        binding.resultList.setAdapter(resultAdapter);

        viewModel.getResults().observe(getViewLifecycleOwner(), this::setResults);
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> updateState());
    }

    private void searchNovels() {
        CharSequence text = binding.searchField.getText();
        if (text == null || text.toString().trim().isEmpty()) return;
        binding.searchField.clearFocus();
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(binding.searchField.getWindowToken(), 0);
        viewModel.search(text.toString());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setResults(Map<DataSource, List<Novel>> result) {
        results.clear();
        results.addAll(result.entrySet());
        resultAdapter.notifyDataSetChanged();
        updateState();
    }

    private void updateState() {
        boolean loading = Boolean.TRUE.equals(viewModel.isLoading().getValue());
        boolean searched = viewModel.getKeyword() != null;
        if (loading) binding.progress.show();
        else binding.progress.hide();
        binding.empty.setVisibility(!loading && searched && results.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNovelItemClick(Novel item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        controller.navigate(R.id.details_fragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.MyViewHolder> {
        private final SpacingDecorator spacingDecorator = new SpacingDecorator(10);
        // every row is the same horizontal cover list, so let them share recycled tiles
        private final RecyclerView.RecycledViewPool novelPool = new RecyclerView.RecycledViewPool();

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSearchResultBinding resultBinding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MyViewHolder(resultBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Map.Entry<DataSource, List<Novel>> entry = results.get(position);
            holder.binding.sourceName.setText(entry.getKey().name);
            holder.binding.searchResults.setAdapter(new NovelAdapter(entry.getValue(), Search.this));
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

                LinearLayoutManager layoutManager = new LinearLayoutManager(binding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false);
                layoutManager.setRecycleChildrenOnDetach(true);
                binding.searchResults.setLayoutManager(layoutManager);
                binding.searchResults.setRecycledViewPool(novelPool);
                binding.searchResults.addItemDecoration(spacingDecorator);
            }
        }
    }
}
