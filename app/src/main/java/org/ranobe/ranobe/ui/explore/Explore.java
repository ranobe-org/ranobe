package org.ranobe.ranobe.ui.explore;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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
import java.util.HashMap;
import java.util.List;

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
        binding.sourceList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.novelCoverLayout.setOnClickListener(v -> openNovelDetails());
        binding.continueBtn.setOnClickListener(v -> continueReading());

        setSourcesListToUi();
        setContinueReadingItem();
        return binding.getRoot();
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
        RanobeDatabase.database().readHistory().getLastReadHistory().observe(requireActivity(), history -> {
            if (history != null) {
                this.readHistory = history;
                Glide.with(binding.novelCover.getContext())
                        .load(history.cover)
                        .into(binding.novelCover);

                binding.novelTitle.setText(history.novelName);
                binding.lastReadChapter.setText(history.name);
                binding.lastReadTimestamp.setText(DateUtils.getRelativeTime(history.timestamp));

                binding.continueReadingInfo.setVisibility(View.VISIBLE);
                binding.continueReading.setVisibility(View.VISIBLE);

            }
        });
    }

    private void setSourcesListToUi() {
        HashMap<Integer, Class<?>> sources = (HashMap<Integer, Class<?>>) SourceManager.getSources();
        List<DataSource> dataSources = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            DataSource dataSource = src.metadata();
            if (dataSource.isActive) {
                dataSources.add(src.metadata());
            }
        }
        binding.sourceList.setAdapter(new SourceAdapter(dataSources, this, this));
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
}
