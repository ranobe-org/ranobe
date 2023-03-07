package org.ranobe.ranobe.ui.library.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.ranobe.ranobe.databinding.ItemNovelBinding;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;

import java.util.List;

public class DatabaseNovelAdapter extends NovelAdapter {
    private final OnNovelItemLongClickListener listener;

    public DatabaseNovelAdapter(List<NovelItem> items, OnNovelItemClickListener clickListener, OnNovelItemLongClickListener listener) {
        super(items, clickListener);
        this.listener = listener;
    }

    @Override
    public List<NovelItem> getItems() {
        return super.getItems();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNovelBinding binding = ItemNovelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ExtendedViewHolder(binding);
    }

    public interface OnNovelItemLongClickListener {
        void onNovelItemLongClick(NovelItem item);
    }

    public class ExtendedViewHolder extends NovelAdapter.MyViewHolder {

        public ExtendedViewHolder(@NonNull ItemNovelBinding binding) {
            super(binding);

            binding.novelCoverLayout.setOnLongClickListener(v -> {
                listener.onNovelItemLongClick(getItems().get(getAdapterPosition()));
                return true;
            });
        }
    }
}
