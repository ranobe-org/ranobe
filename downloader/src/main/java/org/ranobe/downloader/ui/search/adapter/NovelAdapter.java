package org.ranobe.downloader.ui.search.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ranobe.core.models.Novel;
import org.ranobe.downloader.databinding.ItemNovelBinding;

import java.util.List;

public class NovelAdapter extends RecyclerView.Adapter<NovelAdapter.MyViewHolder> {
    private final List<Novel> items;
    private final OnNovelItemClickListener listener;
    private OnNovelLongClickListener longClickListener;

    public NovelAdapter(List<Novel> items, OnNovelItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public NovelAdapter(List<Novel> items, OnNovelItemClickListener listener, OnNovelLongClickListener longClickListener) {
        this.items = items;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public List<Novel> getItems() {
        return items;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNovelBinding binding = ItemNovelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Novel item = items.get(position);
        holder.binding.novelName.setText(item.name);
        Glide.with(holder.binding.novelCover.getContext())
                .load(item.cover)
                .into(holder.binding.novelCover);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnNovelItemClickListener {
        void onNovelItemClick(Novel item);
    }

    public interface OnNovelLongClickListener {
        void onNovelLongClick(Novel novel);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemNovelBinding binding;

        public MyViewHolder(@NonNull ItemNovelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.novelCoverLayout.setOnClickListener(v ->
                    listener.onNovelItemClick(items.get(getAdapterPosition())));

            binding.novelCoverLayout.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onNovelLongClick(items.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }
    }
}
