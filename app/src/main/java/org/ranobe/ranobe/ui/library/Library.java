package org.ranobe.ranobe.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentLibraryBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

public class Library extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private FragmentLibraryBinding binding;

    public Library() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        setUpObservers();
        return binding.getRoot();
    }

    private void setUpObservers() {
        // TODO: pass novel id to other pages as well
        // TODO: use that id to check in db, ordering / timestamps for entities
        RanobeDatabase
                .database()
                .novels()
                .list()
                .observe(getViewLifecycleOwner(), this::setUpUi);
    }

    private void setUpUi(List<Novel> novels) {
        NovelAdapter adapter = new NovelAdapter(new ArrayList<>(novels), this);
        DisplayUtils utils = new DisplayUtils(requireContext(), R.layout.item_novel);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), utils.noOfCols()));
        binding.novelList.addItemDecoration(new SpacingDecorator(utils.spacing()));
        binding.novelList.setAdapter(adapter);
    }

    @Override
    public void onNovelItemClick(NovelItem item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        Ranobe.saveCurrentSource(item.sourceId);

        Bundle bundle = new Bundle();
        bundle.putString("novel", item.url);
        controller.navigate(R.id.library_fragment_to_details, bundle);
    }
}