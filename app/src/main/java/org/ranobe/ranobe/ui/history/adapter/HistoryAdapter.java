package org.ranobe.ranobe.ui.history.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ranobe.ranobe.databinding.ItemHistoryBinding;
import org.ranobe.ranobe.interfaces.OnItemClickListener;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.util.DateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<ReadHistory> list;
    private final OnItemClickListener<Map<String, Object>> listener;
    public HistoryAdapter(List<ReadHistory> list, OnItemClickListener<Map<String, Object>> listener) {
        this.list = list;
        this.listener = listener;
    }



    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {
        ReadHistory item = list.get(position);

        Glide.with(holder.itemBinding.novelCover.getContext())
                .load(item.cover)
                .into(holder.itemBinding.novelCover);

        holder.itemBinding.novelTitle.setText(item.novelName);
        holder.itemBinding.lastReadChapter.setText(item.name);
        holder.itemBinding.lastReadTimestamp.setText(DateUtils.getRelativeTime(item.timestamp));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding itemBinding;
        public ViewHolder(@NonNull ItemHistoryBinding itemView) {
            super(itemView.getRoot());
            itemBinding = itemView;
            Map<String,Object> tuple = new HashMap<>();

            View.OnClickListener coverClickListener = v -> {
               ReadHistory history = list.get(getAdapterPosition());
                if (history != null && listener != null) {
                    tuple.clear();
                    tuple.put("item",history);
                    tuple.put("isDetail",true);
                    listener.OnItemClick(tuple);
                }
            };

            itemBinding.novelCoverLayout.setOnClickListener(coverClickListener);
            itemBinding.novelCover.setOnClickListener(coverClickListener);

            View.OnClickListener contentClickListener = v -> {
                ReadHistory history = list.get(getAdapterPosition());
                if (history != null && listener != null) {
                    tuple.clear();
                    tuple.put("item",history);
                    tuple.put("isDetail",false);
                    listener.OnItemClick(tuple);
                }
            };

            View.OnLongClickListener contentLongClickListener = v -> {
                ReadHistory history = list.get(getAdapterPosition());

                if (history != null && listener != null) {
                    tuple.clear();
                    tuple.put("item", history);
                    tuple.put("isDetail", false);
                    tuple.put("isDelete", true);

                    listener.OnItemClick(tuple);
                }
                return true;
            };

            itemBinding.readChapter.setOnClickListener(contentClickListener);
            itemBinding.readChapter.setOnLongClickListener(contentLongClickListener);




        }

    }
}
