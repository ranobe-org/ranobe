package org.ranobe.ranobe.ui.library;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.databinding.FragmentLibraryBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.library.adapter.DatabaseNovelAdapter;
import org.ranobe.ranobe.ui.library.viewmodel.LibraryViewModel;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

public class Library extends Fragment implements NovelAdapter.OnNovelItemClickListener, DatabaseNovelAdapter.OnNovelItemLongClickListener {
    private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;

    public Library() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        setUpObservers();
        return binding.getRoot();
    }

    private void setUpObservers() {
        viewModel.list().observe(getViewLifecycleOwner(), this::setUpUi);
    }

    private void setUpUi(List<Novel> novels) {
        DatabaseNovelAdapter adapter = new DatabaseNovelAdapter(new ArrayList<>(novels), this, this);
        DisplayUtils utils = new DisplayUtils(requireContext(), R.layout.item_novel);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), utils.noOfCols()));
        binding.novelList.addItemDecoration(new SpacingDecorator(utils.spacing()));
        binding.novelList.setAdapter(adapter);
    }

    @Override
    public void onNovelItemClick(NovelItem item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        RanobeSettings.get().setCurrentSource(item.sourceId).save();

        Bundle bundle = new Bundle();
        bundle.putString(Ranobe.KEY_NOVEL_URL, item.url);
        bundle.putLong(Ranobe.KEY_NOVEL_ID, item.id);
        controller.navigate(R.id.library_fragment_to_details, bundle);
    }

    @Override
    public void onNovelItemLongClick(NovelItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to remove this novel from the library?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    viewModel.deleteNovel(item.id).observe(getViewLifecycleOwner(), this::setMessage);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
