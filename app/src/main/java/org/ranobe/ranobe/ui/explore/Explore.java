package org.ranobe.ranobe.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.config.Ranobe;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        binding.sourceList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        setSourcesListToUi();

        return binding.getRoot();
    }

    private void setSourcesListToUi() {
        HashMap<Integer, Class<?>> sources = SourceManager.getSources();
        List<DataSource> dataSources = new ArrayList<>();
        for(Integer id: sources.keySet()) {
            Source src = SourceManager.getSource(id);
            dataSources.add(src.metadata());
        }
        binding.sourceList.setAdapter(new SourceAdapter(dataSources, this));
    }

    @Override
    public void select(DataSource source) {
        Snackbar.make(binding.getRoot(), "Select current source " + source.name, Snackbar.LENGTH_SHORT).show();
        Ranobe.saveCurrentSource(source.sourceId);
    }
}