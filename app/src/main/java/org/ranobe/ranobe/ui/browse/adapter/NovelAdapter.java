package org.ranobe.ranobe.ui.browse.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import org.ranobe.ranobe.databinding.ItemNovelBinding;
import org.ranobe.ranobe.databinding.ItemNovelGridBinding;
import org.ranobe.ranobe.models.Novel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NovelAdapter extends RecyclerView.Adapter<NovelAdapter.MyViewHolder> {
    private final List<Novel> items;
    private final OnNovelItemClickListener listener;
    private OnNovelLongClickListener longClickListener;
    // grid tiles stretch to their column; the default tile keeps a fixed width for horizontal rows
    private boolean grid = false;

    public NovelAdapter(List<Novel> items, OnNovelItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public NovelAdapter(List<Novel> items, OnNovelItemClickListener listener, OnNovelLongClickListener longClickListener) {
        this.items = items;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public NovelAdapter asGrid() {
        this.grid = true;
        return this;
    }

    public List<Novel> getItems() {
        return items;
    }

    // replaces the items, only rebinding tiles that actually changed
    public void submit(List<Novel> next) {
        List<Novel> old = new ArrayList<>(items);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(old.get(oldPosition).url, next.get(newPosition).url);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                Novel a = old.get(oldPosition);
                Novel b = next.get(newPosition);
                return Objects.equals(a.name, b.name) && Objects.equals(a.cover, b.cover);
            }
        });
        items.clear();
        items.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (grid) {
            ItemNovelGridBinding binding = ItemNovelGridBinding.inflate(inflater, parent, false);
            return new MyViewHolder(binding.getRoot(), binding.novelCoverLayout, binding.novelCover, binding.novelName);
        }
        ItemNovelBinding binding = ItemNovelBinding.inflate(inflater, parent, false);
        return new MyViewHolder(binding.getRoot(), binding.novelCoverLayout, binding.novelCover, binding.novelName);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Novel item = items.get(position);
        holder.name.setText(item.name);
        Glide.with(holder.cover.getContext())
                .load(item.cover)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.cover);
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
        private final ImageView cover;
        private final TextView name;

        public MyViewHolder(@NonNull View root, View coverLayout, ImageView cover, TextView name) {
            super(root);
            this.cover = cover;
            this.name = name;

            coverLayout.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) listener.onNovelItemClick(items.get(position));
            });

            coverLayout.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onNovelLongClick(items.get(position));
                    return true;
                }
                return false;
            });
        }
    }
}
