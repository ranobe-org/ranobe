package org.ranobe.ranobe.ui.history;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.database.mapper.ChapterMapper;
import org.ranobe.ranobe.databinding.FragmentHistoryBinding;
import org.ranobe.ranobe.interfaces.OnItemClickListener;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.ui.history.adapter.HistoryAdapter;
import org.ranobe.ranobe.ui.history.viewmodel.HistoryViewModel;
import org.ranobe.ranobe.ui.reader.ReaderActivity;

import java.util.List;
import java.util.Map;


public class History extends Fragment implements OnItemClickListener<Map<String, Object>> {
    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHistoryBinding.bind(view);

        viewModel.getReadHistories().observe(getViewLifecycleOwner(), this::getReadHistory);
    }

    private void getReadHistory(List<ReadHistory> list) {
        HistoryAdapter adapter = new HistoryAdapter(list, this);
        binding.historyRecyclerView.setAdapter(adapter);
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    }


    @Override
    public void OnItemClick(Map<String, Object> item) {
        ReadHistory history = (ReadHistory) item.get("item");
        if (history == null) return;
        boolean isDetail = Boolean.TRUE.equals(item.get("isDetail"));
        boolean isDelete = Boolean.TRUE.equals(item.get("isDelete"));

        Bundle bundle = new Bundle();
        if (isDetail) {
            NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            bundle.clear();
            bundle.putParcelable(Ranobe.KEY_NOVEL, new Novel(history.novelUrl,history.sourceId));
            controller.navigate(R.id.history_fragment_to_details, bundle);
        }else if (isDelete) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Are you sure you want to remove read history for this novel?")
                    .setPositiveButton("Yes", (dialog, i) -> viewModel.deleteNovelReadHistory(history.novelUrl))
                    .setNegativeButton("Cancel", (dialog, i) -> dialog.dismiss())
                    .show();
        } else {
            bundle.clear();
            bundle.putParcelable(Ranobe.KEY_NOVEL, new Novel(history.novelUrl,history.sourceId));
            bundle.putParcelable(Ranobe.KEY_CHAPTER, ChapterMapper.ToChapter(history));
            bundle.putParcelable(Ranobe.KEY_READ_HISTORY, history);
            requireActivity().startActivity(new Intent(requireActivity(), ReaderActivity.class).putExtras(bundle).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }
}
