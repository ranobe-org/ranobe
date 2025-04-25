package org.ranobe.ranobe.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.databinding.FragmentExploreBinding;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.explore.adapter.SourceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Explore extends Fragment implements SourceAdapter.OnSourceSelected {
    private FragmentExploreBinding binding;

    public Explore() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        binding.sourceList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.sourceListInactive.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.btnToggleHiddenSources.setOnClickListener(v -> binding.sourceListInactive.setVisibility(binding.sourceListInactive.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        setSourcesListToUi();
        return binding.getRoot();
    }

    private void setSourcesListToUi() {
        HashMap<Integer, Class<?>> sources = (HashMap<Integer, Class<?>>) SourceManager.getSources();
        List<DataSource> dataSources = new ArrayList<>();
        List<DataSource> dataSourcesInActive = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            DataSource dataSource = src.metadata();
            if (dataSource.isActive) {
                dataSources.add(src.metadata());
            } else {
                dataSourcesInActive.add(src.metadata());
            }
        }
        binding.sourceList.setAdapter(new SourceAdapter(dataSources, this));
        binding.sourceListInactive.setAdapter(new SourceAdapter(dataSourcesInActive, this));
    }

    @Override
    public void select(DataSource source) {
        RanobeSettings.get().setCurrentSource(source.sourceId).save();
        navigateToBrowse(source.sourceId);
    }

    private void navigateToBrowse(int sourceId) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putInt(Ranobe.KEY_SOURCE_ID, sourceId);
        controller.navigate(R.id.explore_fragment_to_browse, bundle);
    }
}