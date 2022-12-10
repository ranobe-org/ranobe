package org.ranobe.ranobe.ui.details;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentDetailsBinding;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.details.viewmodel.DetailsViewModel;

public class Details extends Fragment {
    private String novelUrl;
    private FragmentDetailsBinding binding;
    private DetailsViewModel viewModel;

    public Details() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novelUrl = getArguments().getString("novel");
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        Toast.makeText(requireContext(), novelUrl, Toast.LENGTH_SHORT).show();

        Source source = SourceManager.getSource(1);
        viewModel.getChapters().observe(requireActivity(), chapters -> Log.d(Ranobe.DEBUG, " CHAPTS " + chapters.size()));
        viewModel.getDetails().observe(requireActivity(), novel -> Log.d(Ranobe.DEBUG, novel.toString()));

        viewModel.details(source, novelUrl);
        viewModel.chapters(source, novelUrl);


        return binding.getRoot();
    }
}