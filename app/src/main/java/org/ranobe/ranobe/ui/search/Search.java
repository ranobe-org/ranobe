package org.ranobe.ranobe.ui.search;

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
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayoutMediator;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentSearchBinding;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;
import org.ranobe.ranobe.ui.views.EmptyState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One search field over a tab per source. A query runs on the open tab only, other tabs search
 * for it when they are opened, so no source is hit unless the user looks at it.
 */
public class Search extends Fragment {
    private final List<DataSource> sources = new ArrayList<>();
    // remembers the tab the user picks, ignoring the pager restoring or tearing itself down
    private final ViewPager2.OnPageChangeCallback pageChanges = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (!isResumed() || position < 0 || position >= sources.size()) return;
            Ranobe.setLastSearchSource(sources.get(position).sourceId);
        }
    };
    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private TabLayoutMediator mediator;

    public Search() {
        // Required empty public constructor
    }

    // turned on, still working and with a search on their site
    private static List<DataSource> searchableSources() {
        List<DataSource> list = new ArrayList<>();
        for (Integer id : SourceManager.getSources().keySet()) {
            if (!Ranobe.isSourceEnabled(id)) continue;
            DataSource source = SourceManager.getSource(id).metadata();
            if (Boolean.TRUE.equals(source.isActive) && source.isSearchable) list.add(source);
        }
        // a stable, predictable order instead of whatever the source map gives
        Collections.sort(list, (a, b) -> tabTitle(a).compareToIgnoreCase(tabTitle(b)));
        return list;
    }

    // "Ranobehub — ранобэ на русском онлайн" is a site title, the tab only needs the name
    private static String tabTitle(DataSource source) {
        String name = source.name == null ? "" : source.name.trim();
        for (String separator : new String[]{" — ", " – ", " - ", " | "}) {
            int at = name.indexOf(separator);
            if (at > 0) return name.substring(0, at).trim();
        }
        return name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // sources may have been turned on or off since the last time
        sources.clear();
        sources.addAll(searchableSources());

        if (sources.isEmpty()) {
            binding.searchView.setVisibility(View.GONE);
            binding.tabs.setVisibility(View.GONE);
            binding.pager.setVisibility(View.GONE);
            EmptyState.show(binding.emptyState, R.drawable.ic_search, R.string.search_no_sources_title,
                    getString(R.string.search_no_sources), R.string.manage, v -> openExplore());
            return;
        }
        EmptyState.hide(binding.emptyState);

        String query = viewModel.currentQuery();
        // the query can change from elsewhere (a genre tapped on a details page)
        viewModel.getQuery().observe(getViewLifecycleOwner(), text -> {
            CharSequence shown = binding.searchField.getText();
            if (text != null && (shown == null || !text.contentEquals(shown)))
                binding.searchField.setText(text);
            updateGenreChip();
        });
        binding.genreChip.setOnCloseIconClickListener(v -> {
            viewModel.clearGenre(sources.get(binding.pager.getCurrentItem()).sourceId);
            updateGenreChip();
        });
        binding.searchView.setEndIconOnClickListener(v -> {
            binding.searchField.setText(null);
            binding.searchField.requestFocus();
            showKeyboard();
        });
        binding.searchField.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            // a hardware enter sends both down and up, only search once
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (enter && event.getAction() == KeyEvent.ACTION_DOWN)) {
                submit();
                return true;
            }
            return enter;
        });

        binding.pager.setAdapter(new SourcesAdapter(this));
        // neighbouring tabs stay idle until opened, don't build them ahead of time
        binding.pager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
        mediator = new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(tabTitle(sources.get(position))));
        mediator.attach();
        binding.tabs.setVisibility(sources.size() > 1 ? View.VISIBLE : View.GONE);

        int start = indexOf(viewModel.selectedSource());
        if (start < 0) start = indexOf(Ranobe.getLastSearchSource());
        if (start > 0) {
            int target = start;
            binding.pager.setCurrentItem(target, false);
            // a restored pager brings back its old page after this, the selected source wins
            binding.pager.post(() -> {
                if (binding != null && binding.pager.getCurrentItem() != target)
                    binding.pager.setCurrentItem(target, false);
            });
        }
        binding.pager.registerOnPageChangeCallback(pageChanges);

        // nothing searched yet, start typing straight away
        if (query == null) {
            binding.searchField.requestFocus();
            binding.searchField.post(this::showKeyboard);
        }
    }

    private void submit() {
        CharSequence text = binding.searchField.getText();
        if (text == null || text.toString().trim().isEmpty() || sources.isEmpty()) return;
        binding.searchField.clearFocus();
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(binding.searchField.getWindowToken(), 0);
        viewModel.submit(text.toString(), sources.get(binding.pager.getCurrentItem()).sourceId);
    }

    private void updateGenreChip() {
        if (binding == null) return;
        int source = viewModel.genreSource();
        String text = viewModel.currentQuery();
        boolean genre = source != -1 && text != null;
        binding.genreChip.setVisibility(genre ? View.VISIBLE : View.GONE);
        if (genre) binding.genreChip.setText(getString(R.string.search_genre_chip, text));
    }

    private void showKeyboard() {
        if (binding == null) return;
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) imm.showSoftInput(binding.searchField, InputMethodManager.SHOW_IMPLICIT);
    }

    private int indexOf(int sourceId) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).sourceId == sourceId) return i;
        }
        return -1;
    }

    // sources are turned on and off on the Explore page
    private void openExplore() {
        BottomNavigationView navbar = requireActivity().findViewById(R.id.navbar);
        if (navbar != null) navbar.setSelectedItemId(R.id.explore_fragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediator != null) mediator.detach();
        mediator = null;
        // the pager keeps a reference to its adapter, which holds this fragment
        if (binding != null) {
            binding.pager.unregisterOnPageChangeCallback(pageChanges);
            binding.pager.setAdapter(null);
        }
        binding = null;
    }

    private class SourcesAdapter extends FragmentStateAdapter {
        SourcesAdapter(Fragment host) {
            super(host);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            DataSource source = sources.get(position);
            return SearchSourceFragment.newInstance(source.sourceId, tabTitle(source));
        }

        @Override
        public int getItemCount() {
            return sources.size();
        }

        // stable per source, so a restored tab keeps its fragment
        @Override
        public long getItemId(int position) {
            return sources.get(position).sourceId;
        }

        @Override
        public boolean containsItem(long itemId) {
            for (DataSource source : sources) if (source.sourceId == itemId) return true;
            return false;
        }
    }
}
