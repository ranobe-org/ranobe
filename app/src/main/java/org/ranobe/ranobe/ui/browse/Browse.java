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

import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentBrowseBinding;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Lang;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.browse.viewmodel.BrowseViewModel;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

public class Browse extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    // start the next page while this many rows are still below the screen
    private static final int PREFETCH_ROWS = 2;

    private final List<Novel> list = new ArrayList<>();
    private FragmentBrowseBinding binding;

    private BrowseViewModel viewModel;
    private NovelAdapter adapter;
    private GridLayoutManager layoutManager;

    private int sourceId = -1;

    public Browse() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sourceId = getArguments().getInt(Ranobe.KEY_SOURCE_ID);
        }
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
        setUpToolbar();

        adapter = new NovelAdapter(list, this).asGrid();
        layoutManager = DisplayUtils.applyNovelGrid(binding.novelList);
        binding.novelList.setAdapter(adapter);
        binding.novelList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) maybeLoadMore();
            }
        });

        viewModel.open(sourceId);
        viewModel.getNovels().observe(getViewLifecycleOwner(), this::setNovels);
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> updateLoading());
        viewModel.getError().observe(getViewLifecycleOwner(), this::setUpError);
    }

    private void setUpToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main).navigateUp());
        Source source = SourceManager.getSource(sourceId);
        if (source == null) return;
        DataSource metadata = source.metadata();
        binding.toolbar.setTitle(metadata.name);
        binding.toolbar.setSubtitle(Lang.eng.equals(metadata.lang) ? "English" : Lang.ru.equals(metadata.lang) ? "Русский" : metadata.lang);
    }

    private void setNovels(List<Novel> novels) {
        int old = list.size();
        list.clear();
        list.addAll(novels);
        if (novels.size() >= old) {
            adapter.notifyItemRangeInserted(old, novels.size() - old);
        } else {
            adapter.notifyDataSetChanged();
        }
        updateLoading();
        // a short first page can't be scrolled, so keep loading until the screen is filled
        binding.novelList.post(() -> {
            if (binding != null && !binding.novelList.canScrollVertically(1)) maybeLoadMore();
        });
    }

    private void maybeLoadMore() {
        if (layoutManager == null || viewModel.isEndReached()) return;
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible >= list.size() - layoutManager.getSpanCount() * PREFETCH_ROWS) {
            viewModel.loadMore();
        }
    }

    private void updateLoading() {
        boolean loading = Boolean.TRUE.equals(viewModel.isLoading().getValue());
        boolean empty = list.isEmpty();
        binding.initialProgress.setVisibility(loading && empty ? View.VISIBLE : View.GONE);
        binding.progress.setVisibility(loading && !empty ? View.VISIBLE : View.GONE);
        binding.empty.setVisibility(!loading && empty && viewModel.isEndReached() ? View.VISIBLE : View.GONE);
    }

    private void setUpError(BrowseViewModel.LoadError error) {
        if (error == null) return;
        viewModel.clearError();
        if (error.firstPage && list.isEmpty()) {
            Error.navigateToErrorFragment(requireActivity(), error.message);
            return;
        }
        Snackbar.make(binding.getRoot(), R.string.browse_load_failed, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> viewModel.loadMore())
                .show();
    }

    @Override
    public void onNovelItemClick(Novel item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        controller.navigate(R.id.browse_fragment_to_details, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
