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
import androidx.recyclerview.widget.GridLayoutManager;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.FragmentSearchBinding;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;


public class Search extends Fragment implements NovelAdapter.OnNovelItemClickListener{
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private Source source;

    public Search() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        source = SourceManager.getSource(1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        binding.submit.setOnClickListener(v -> searchNovels());

        DisplayUtils utils = new DisplayUtils(requireContext(), R.layout.item_novel);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), utils.noOfCols()));
        binding.novelList.addItemDecoration(new SpacingDecorator(utils.spacing()));

        viewModel.getNovels().observe(requireActivity(), (novels) -> {
            binding.progress.setVisibility(View.GONE);
            binding.novelList.setAdapter(new NovelAdapter(novels, this));
        });

        return binding.getRoot();
    }

    private void fetchNovels(String keyword) {
        Filter filter = new Filter();
        filter.addFilter(Filter.FILTER_KEYWORD, keyword);
        binding.progress.setVisibility(View.VISIBLE);
        viewModel.search(source, filter, 1);
    }

    private void searchNovels() {
        binding.searchField.clearFocus();
        if (binding.searchField.getText() != null) {
            String keyword = binding.searchField.getText().toString();
            fetchNovels(keyword);
        }
    }

    @Override
    public void onNovelItemClick(NovelItem item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putString("novel", item.url);
        controller.navigate(R.id.details_fragment, bundle);
    }
}