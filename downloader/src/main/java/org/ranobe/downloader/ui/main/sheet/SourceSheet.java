package org.ranobe.downloader.ui.main.sheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.ranobe.core.models.DataSource;
import org.ranobe.core.sources.Source;
import org.ranobe.core.sources.SourceManager;
import org.ranobe.downloader.databinding.SheetSourcesBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SourceSheet extends BottomSheetDialogFragment {
    public static final String TAG = "sources-sheet";
    private SheetSourcesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetSourcesBinding.inflate(inflater, container, false);
        setSourcesListToUi();
        return binding.getRoot();
    }

    private void setSourcesListToUi() {
        HashMap<Integer, Class<?>> sources = (HashMap<Integer, Class<?>>) SourceManager.getSources();
        List<DataSource> dataSources = new ArrayList<>();
        for (Integer id : sources.keySet()) {
            Source src = SourceManager.getSource(id);
            if (src.metadata().isActive) {
                dataSources.add(src.metadata());
            }
        }
        binding.rvSources.setAdapter(new SourceAdapter(dataSources));
    }
}
