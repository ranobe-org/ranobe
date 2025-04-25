package org.ranobe.ranobe.ui.chapters.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.databinding.ItemChapterBinding;
import org.ranobe.ranobe.models.Chapter;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.MyViewHolder> {
    private final List<Chapter> items;
    private final OnChapterItemClickListener listener;
    private List<String> readingList;

    public ChapterAdapter(List<Chapter> items, OnChapterItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setReadingList(List<String> readingList) {
        this.readingList = readingList;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChapterBinding binding = ItemChapterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Chapter item = items.get(position);
        if (readingList != null && readingList.contains(item.url)) {
            holder.binding.chapterItemLayout.setAlpha(0.5F);
        } else {
            holder.binding.chapterItemLayout.setAlpha(1);
        }
        holder.binding.chapterName.setText(item.name);
        if (item.updated != null && !item.updated.isEmpty())
            holder.binding.updated.setText(item.updated);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnChapterItemClickListener {
        void onChapterItemClick(Chapter item);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemChapterBinding binding;

        public MyViewHolder(@NonNull ItemChapterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.chapterItemLayout.setOnClickListener(v ->
                    listener.onChapterItemClick(items.get(getAdapterPosition())));
        }
    }
}
