package org.ranobe.ranobe.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentLibraryBinding;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.views.SpacingDecorator;
import org.ranobe.ranobe.util.DisplayUtils;
import org.ranobe.ranobe.util.NumberUtils;

import java.util.List;

public class Library extends Fragment implements NovelAdapter.OnNovelItemClickListener, NovelAdapter.OnNovelLongClickListener {
    private FragmentLibraryBinding binding;

    public Library() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentLibraryBinding.bind(view);

        DisplayUtils utils = new DisplayUtils(requireContext(), R.layout.item_novel);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), utils.noOfCols()));
        binding.novelList.addItemDecoration(new SpacingDecorator(utils.spacing()));

        RanobeDatabase.database().novels().list().observe(getViewLifecycleOwner(), this::setNovels);
    }

    private void setNovels(List<Novel> novels) {
        binding.progress.hide();
        if (novels.size() == 0) {
            showNoNovels();
            return;
        }
        NovelAdapter adapter = new NovelAdapter(novels, this, this);
        binding.novelList.setAdapter(adapter);
    }

    private void showNoNovels() {
        binding.error.setText(R.string.no_novels_error);
        binding.emoji.setText(Ranobe.SILLY_EMOJI[NumberUtils.getRandom(Ranobe.SILLY_EMOJI.length)]);
    }

    @Override
    public void onNovelItemClick(Novel item) {
        RanobeSettings.get().setCurrentSource(item.sourceId).save();
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        controller.navigate(R.id.library_fragment_to_details, bundle);
    }

    @Override
    public void onNovelLongClick(Novel novel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage("Are you sure you want to remove novel from the library?")
                .setPositiveButton("Yes", (dialog, i) -> removeFromLib(novel))
                .setNegativeButton("Cancel", (dialog, i) -> dialog.dismiss())
                .show();
    }

    private void removeFromLib(Novel novel) {
        RanobeDatabase.databaseExecutor.execute(() -> RanobeDatabase.database().novels().delete(novel.url));
        Snackbar.make(binding.getRoot(), "Removing novel from the library", Snackbar.LENGTH_LONG).show();
    }
}