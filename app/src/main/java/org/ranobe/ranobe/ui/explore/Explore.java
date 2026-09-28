package org.ranobe.ranobe.ui.explore;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.database.mapper.ChapterMapper;
import org.ranobe.ranobe.databinding.FragmentExploreBinding;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.explore.adapter.SourceAdapter;
import org.ranobe.ranobe.ui.reader.ReaderActivity;
import org.ranobe.ranobe.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Explore extends Fragment implements SourceAdapter.OnSourceSelected, SourceAdapter.OnSourceToggled {
    private FragmentExploreBinding binding;

    private ReadHistory readHistory;

    public Explore() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.sourceList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.novelCoverLayout.setOnClickListener(v -> openNovelDetails());
        binding.continueBtn.setOnClickListener(v -> continueReading());
        binding.continueReading.setOnClickListener(v -> continueReading());

        setSourcesListToUi();
        setContinueReadingItem();
    }

    private void continueReading() {
        if (readHistory == null) return;
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, new Novel(readHistory.novelUrl, readHistory.sourceId));
        bundle.putParcelable(Ranobe.KEY_CHAPTER, ChapterMapper.ToChapter(readHistory));
        bundle.putParcelable(Ranobe.KEY_READ_HISTORY, readHistory);
        requireActivity().startActivity(new Intent(requireActivity(), ReaderActivity.class).putExtras(bundle).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private void openNovelDetails() {
        if (readHistory == null) return;
        Bundle bundle = new Bundle();
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        bundle.clear();
        bundle.putParcelable(Ranobe.KEY_NOVEL, new Novel(readHistory.novelUrl, readHistory.sourceId));
        controller.navigate(R.id.explore_fragment_to_details, bundle);
    }

    private void setContinueReadingItem() {
        RanobeDatabase.database().readHistory().getLastReadHistory().observe(getViewLifecycleOwner(), history -> {
            this.readHistory = history;
            // hide again once history is cleared, instead of leaving a stale card behind
            int visibility = history == null ? View.GONE : View.VISIBLE;
            binding.continueReadingInfo.setVisibility(visibility);
            binding.continueReading.setVisibility(visibility);
            if (history == null) return;

            Glide.with(binding.novelCover.getContext())
                    .load(history.cover)
                    .centerCrop()
                    .into(binding.novelCover);

            binding.novelTitle.setText(history.novelName);
            binding.lastReadChapter.setText(history.name);
            binding.lastReadTimestamp.setText(DateUtils.getRelativeTime(history.timestamp));
        });
    }

    private void setSourcesListToUi() {
        Map<Integer, Class<?>> sources = SourceManager.getSources();
        List<DataSource> dataSources = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            DataSource dataSource = src.metadata();
            if (dataSource.isActive) {
                dataSources.add(dataSource);
            }
        }
        // group by language, then alphabetically
        Collections.sort(dataSources, (a, b) -> {
            int byLang = a.lang.compareTo(b.lang);
            return byLang != 0 ? byLang : a.name.compareToIgnoreCase(b.name);
        });
        SourceAdapter adapter = new SourceAdapter(dataSources, this, this);
        binding.sourceList.setAdapter(adapter);
        binding.manageSources.setOnClickListener(v -> {
            boolean managing = !adapter.isManaging();
            adapter.setManaging(managing);
            binding.manageSources.setText(managing ? R.string.done : R.string.manage);
            binding.sourcesHint.setText(managing ? R.string.sources_hint : R.string.sources_browse_hint);
        });
    }

    @Override
    public void select(DataSource source) {
        RanobeSettings.get().setCurrentSource(source.sourceId).save();
        navigateToBrowse(source.sourceId);
    }

    @Override
    public void toggle(DataSource source, boolean enabled) {
        Ranobe.setSourceEnabled(source.sourceId, enabled);
    }

    private void navigateToBrowse(int sourceId) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putInt(Ranobe.KEY_SOURCE_ID, sourceId);
        controller.navigate(R.id.explore_fragment_to_browse, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
