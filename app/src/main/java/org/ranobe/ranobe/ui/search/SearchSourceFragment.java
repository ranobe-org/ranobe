package org.ranobe.ranobe.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.databinding.FragmentSearchSourceBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.Cloudflare;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;
import org.ranobe.ranobe.ui.views.EmptyState;
import org.ranobe.ranobe.ui.views.LoadFooterAdapter;
import org.ranobe.ranobe.ui.webview.WebViewActivity;
import org.ranobe.ranobe.util.DisplayUtils;

import java.util.ArrayList;

/**
 * The results of one source, a tab of {@link Search}. It only renders the state the shared
 * {@link SearchViewModel} keeps for its source and asks it for more pages.
 */
public class SearchSourceFragment extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private static final String ARG_SOURCE_ID = "source_id";
    private static final String ARG_SOURCE_NAME = "source_name";
    // start the next page while this many rows are still below the screen
    private static final int PREFETCH_ROWS = 2;

    private FragmentSearchSourceBinding binding;
    private SearchViewModel viewModel;
    private NovelAdapter adapter;
    private LoadFooterAdapter footer;
    private GridLayoutManager layoutManager;
    private int sourceId;
    private String sourceName;
    private final ActivityResultLauncher<String> verify =
            registerForActivityResult(new WebViewActivity.Verify(), passed -> viewModel.retry(sourceId));

    public static SearchSourceFragment newInstance(int sourceId, String name) {
        SearchSourceFragment fragment = new SearchSourceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SOURCE_ID, sourceId);
        args.putString(ARG_SOURCE_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        sourceId = args.getInt(ARG_SOURCE_ID);
        sourceName = args.getString(ARG_SOURCE_NAME);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchSourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new NovelAdapter(new ArrayList<>(), this).asGrid();
        footer = new LoadFooterAdapter(this::retry);
        layoutManager = DisplayUtils.applyNovelGrid(binding.novelList);
        // the footer row spans every column
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position >= adapter.getItemCount() ? layoutManager.getSpanCount() : 1;
            }
        });
        binding.novelList.setAdapter(new ConcatAdapter(adapter, footer));
        binding.novelList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) maybeLoadMore();
            }
        });

        viewModel.state(sourceId).observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        // only the visible tab is resumed, so this is where a tab searches for the current query
        viewModel.select(sourceId);
    }

    private void render(SearchViewModel.State state) {
        boolean fresh = adapter.getItemCount() == 0 && !state.items.isEmpty();
        adapter.submit(state.items);
        if (fresh) binding.novelList.scrollToPosition(0);
        boolean empty = state.items.isEmpty();

        binding.initialProgress.setVisibility(state.loading && empty ? View.VISIBLE : View.GONE);

        if (state.isIdle()) {
            EmptyState.show(binding.emptyState, R.drawable.ic_search, R.string.search_hint_title,
                    getString(R.string.search_hint_message, sourceName), 0, null);
        } else if (empty && state.error != null) {
            boolean blocked = Cloudflare.blockedUrl(state.error) != null;
            EmptyState.show(binding.emptyState, R.drawable.ic_search, R.string.search_failed, state.error,
                    blocked ? R.string.cloudflare_verify : R.string.retry, v -> retry());
        } else if (empty && state.endReached) {
            EmptyState.show(binding.emptyState, R.drawable.ic_search, R.string.search_nothing_found,
                    getString(R.string.search_nothing_found_hint, state.query, sourceName), 0, null);
        } else {
            EmptyState.hide(binding.emptyState);
        }

        if (empty) {
            footer.set(LoadFooterAdapter.Mode.NONE, null);
        } else if (state.loading) {
            footer.set(LoadFooterAdapter.Mode.LOADING, null);
        } else if (state.error != null) {
            footer.set(LoadFooterAdapter.Mode.FAILED, getString(R.string.load_more_failed_reason, state.error));
        } else if (state.endReached) {
            footer.set(LoadFooterAdapter.Mode.END, null);
        } else {
            footer.set(LoadFooterAdapter.Mode.NONE, null);
            // a short page can't be scrolled, keep loading until the screen is filled
            binding.novelList.post(() -> {
                if (binding != null && !binding.novelList.canScrollVertically(1)) maybeLoadMore();
            });
        }
    }

    // a Cloudflare check is passed in the browser first, anything else is just tried again
    private void retry() {
        SearchViewModel.State state = viewModel.state(sourceId).getValue();
        String blocked = state == null ? null : Cloudflare.blockedUrl(state.error);
        if (blocked != null) verify.launch(blocked);
        else viewModel.retry(sourceId);
    }

    private void maybeLoadMore() {
        if (layoutManager == null) return;
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible >= adapter.getItemCount() - layoutManager.getSpanCount() * PREFETCH_ROWS) {
            viewModel.loadMore(sourceId);
        }
    }

    @Override
    public void onNovelItemClick(Novel item) {
        // details, chapters and the reader go through the current source
        RanobeSettings.get().setCurrentSource(item.sourceId > 0 ? item.sourceId : sourceId).save();
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        controller.navigate(R.id.search_fragment_to_details, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
        footer = null;
        layoutManager = null;
    }
}
