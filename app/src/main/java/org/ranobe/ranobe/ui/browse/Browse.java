package org.ranobe.ranobe.ui.browse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentBrowseBinding;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.browse.viewmodel.BrowseViewModel;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

public class Browse extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private final List<NovelItem> list = new ArrayList<>();
    private FragmentBrowseBinding binding;

    private BrowseViewModel viewModel;
    private NovelAdapter adapter;

    private boolean isLoading = false;

    public Browse() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BrowseViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBrowseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentBrowseBinding.bind(view);

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
                    fetchNovels();
                }
            }
        });

        viewModel.getError().observe(requireActivity(), this::setUpError);
        viewModel.getNovels().observe(requireActivity(), (novels) -> {
            binding.progress.hide();
            isLoading = false;
            int old = list.size();
            list.clear();
            list.addAll(novels);
            adapter.notifyItemRangeInserted(old, list.size());
        });

        fetchNovels();
    }

    private void setUpError(String error) {
        binding.progress.hide();
        // error on the first call
        if (list.size() == 0) {
            Error.navigateToErrorFragment(requireActivity(), error);
        }
    }

    private void fetchNovels() {
        viewModel.novels();
    }

    @Override
    public void onNovelItemClick(NovelItem item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putString(Ranobe.KEY_NOVEL_URL, item.url);
        controller.navigate(R.id.browse_fragment_to_details, bundle);
    }
}
