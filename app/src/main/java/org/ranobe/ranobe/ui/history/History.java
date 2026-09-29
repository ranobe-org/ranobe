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
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.ui.history.adapter.HistoryAdapter;
import org.ranobe.ranobe.ui.history.viewmodel.HistoryViewModel;
import org.ranobe.ranobe.ui.reader.ReaderActivity;
import org.ranobe.ranobe.util.NumberUtils;

import java.util.ArrayList;
import java.util.List;


public class History extends Fragment implements HistoryAdapter.Listener {
    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

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

        // one adapter for the fragment's lifetime so updates diff in place and keep the scroll position
        adapter = new HistoryAdapter(this);
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.historyRecyclerView.setAdapter(adapter);
        binding.emoji.setText(Ranobe.SILLY_EMOJI[NumberUtils.getRandom(Ranobe.SILLY_EMOJI.length)]);
        View clearAll = binding.toolbar.getMenu().findItem(R.id.clear_history).getActionView();
        if (clearAll != null) clearAll.setOnClickListener(v -> confirmClearHistory());

        viewModel.getReadHistories().observe(getViewLifecycleOwner(), this::setReadHistory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setReadHistory(List<ReadHistory> list) {
        adapter.submitHistory(list == null ? new ArrayList<>() : list);
        boolean isEmpty = list == null || list.isEmpty();
        binding.noNovelsLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.historyRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.toolbar.getMenu().findItem(R.id.clear_history).setVisible(!isEmpty);
    }

    private void confirmClearHistory() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.history_clear)
                .setMessage(R.string.history_clear_confirm)
                .setPositiveButton(R.string.clear, (dialog, i) -> viewModel.clearHistory())
                .setNegativeButton(R.string.cancel, (dialog, i) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onContinueReading(ReadHistory history) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, new Novel(history.novelUrl, history.sourceId));
        bundle.putParcelable(Ranobe.KEY_CHAPTER, ChapterMapper.ToChapter(history));
        bundle.putParcelable(Ranobe.KEY_READ_HISTORY, history);
        requireActivity().startActivity(new Intent(requireActivity(), ReaderActivity.class).putExtras(bundle).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onOpenDetails(ReadHistory history) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, new Novel(history.novelUrl, history.sourceId));
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        controller.navigate(R.id.history_fragment_to_details, bundle);
    }

    @Override
    public void onRemove(ReadHistory history) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.history_remove_confirm, history.novelName))
                .setPositiveButton(R.string.remove, (dialog, i) -> viewModel.deleteNovelReadHistory(history.novelUrl))
                .setNegativeButton(R.string.cancel, (dialog, i) -> dialog.dismiss())
                .show();
    }
}
