package org.ranobe.ranobe.ui.browse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.FragmentBrowseBinding;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.browse.viewmodel.BrowseViewModel;

import java.util.ArrayList;
import java.util.List;

public class Browse extends Fragment implements NovelAdapter.OnNovelItemClickListener {
    private final List<NovelItem> list = new ArrayList<>();
    private FragmentBrowseBinding binding;

    private BrowseViewModel viewModel;
    private NovelAdapter adapter;
    private Source source;

    private int page = 1;

    public Browse() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BrowseViewModel.class);
        source = SourceManager.getSource(1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentBrowseBinding.inflate(inflater, container, false);

        adapter = new NovelAdapter(list, this);
        binding.novelList.setLayoutManager(new GridLayoutManager(requireActivity(), 2));
        binding.novelList.setAdapter(adapter);
        binding.novelList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1)) {
                    page += 1;
                    fetchNovels();
                }
            }
        });

        fetchNovels();
        return binding.getRoot();
    }

    private void fetchNovels() {
        viewModel.novels(source, page);
        viewModel.getNovels().observe(requireActivity(), (novels) -> {
            binding.progress.setVisibility(View.GONE);
            int old = list.size();
            list.addAll(novels);
            adapter.notifyItemRangeInserted(old, novels.size());
        });
    }

    @Override
    public void onNovelItemClick(NovelItem item) {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        Bundle bundle = new Bundle();
        bundle.putString("novel", item.url);
        controller.navigate(R.id.details_fragment, bundle);
    }
}