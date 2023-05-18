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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentSearchBinding;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;


public class Search extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private final List<Novel> list = new ArrayList<>();
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private NovelAdapter adapter;
    private int page = 1;
    private boolean isLoading = false;

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
        binding.submit.setOnClickListener(v -> searchNovels(1));

        adapter = new NovelAdapter(list, this);
        DisplayUtils utils = new DisplayUtils(requireContext(), R.layout.item_novel);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), utils.noOfCols()));
        binding.novelList.addItemDecoration(new SpacingDecorator(utils.spacing()));
        binding.novelList.setAdapter(adapter);
        binding.novelList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    binding.progress.show();
                    isLoading = true;
                    page += 1;
                    searchNovels(page);
                }
            }
        });

        viewModel.getError().observe(requireActivity(), this::setUpError);
        viewModel.getNovels().observe(requireActivity(), this::setUpAdapter);

        return binding.getRoot();
    }

    private void setUpError(String error) {
        if (list.size() == 0) {
            Error.navigateToErrorFragment(requireActivity(), error);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setUpAdapter(List<Novel> novels) {
        binding.progress.hide();
        isLoading = false;
        // expand the current list without changing list reference
        list.clear();
        list.addAll(novels);
        adapter.notifyDataSetChanged();
    }

    private void fetchNovels(String keyword) {
        Filter filter = new Filter();
        filter.addFilter(Filter.FILTER_KEYWORD, keyword);
        binding.progress.show();
        viewModel.search(filter, page);
    }

    private void searchNovels(int page) {
        binding.searchField.clearFocus();
        if (binding.searchField.getText() != null) {
            String keyword = binding.searchField.getText().toString();
            this.page = page;
            fetchNovels(keyword);
        }
    }

    @Override
    public void onNovelItemClick(Novel item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putString(Ranobe.KEY_NOVEL, item.url);
        controller.navigate(R.id.details_fragment, bundle);
    }
}