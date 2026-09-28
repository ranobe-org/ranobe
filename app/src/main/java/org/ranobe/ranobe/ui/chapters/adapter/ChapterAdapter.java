package org.ranobe.ranobe.ui.chapters.adapter;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.ItemChapterBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.service.DownloadService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.MyViewHolder> {
    private static final float READ_ALPHA = 0.5f;

    private final List<Chapter> items;
    private final OnChapterItemClickListener listener;
    // built once per history change instead of on every bind; long novels have thousands of rows
    private final Map<String, ReadHistory> historyMap = new HashMap<>();
    private Set<String> downloadedUrls = new HashSet<>();
    private OnChapterDownloadClickListener downloadListener;
    private String lastReadUrl;

    public ChapterAdapter(List<Chapter> items, OnChapterItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public ChapterAdapter(List<Chapter> items, List<ReadHistory> historyList, OnChapterItemClickListener listener) {
        this(items, listener);
        setHistory(historyList);
    }

    public Chapter getItem(int position) {
        return items.get(position);
    }

    // history is newest first, so the first entry is where the reader stopped
    @SuppressLint("NotifyDataSetChanged")
    public void setHistory(List<ReadHistory> historyList) {
        historyMap.clear();
        lastReadUrl = null;
        if (historyList != null) {
            for (ReadHistory history : historyList) {
                if (history == null) continue;
                if (lastReadUrl == null) lastReadUrl = history.url;
                historyMap.put(history.url, history);
            }
        }
        notifyDataSetChanged();
    }

    public String getLastReadUrl() {
        return lastReadUrl;
    }

    public int readCount() {
        int count = 0;
        for (Chapter chapter : items) {
            if (historyMap.containsKey(chapter.url)) count++;
        }
        return count;
    }

    public void setDownloadedUrls(Set<String> urls) {
        this.downloadedUrls = urls;
        notifyItemRangeChanged(0, items.size());
    }

    public void setDownloadListener(OnChapterDownloadClickListener listener) {
        this.downloadListener = listener;
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
        boolean isRead = historyMap.containsKey(item.url);
        boolean isLastRead = Objects.equals(item.url, lastReadUrl);

        // the last read chapter stays fully visible and highlighted even though it counts as read
        holder.binding.chapterText.setAlpha(isRead && !isLastRead ? READ_ALPHA : 1f);
        holder.binding.lastReadMarker.setVisibility(isLastRead ? View.VISIBLE : View.GONE);
        holder.binding.chapterName.setText(item.name);
        holder.binding.chapterName.setTypeface(null, isLastRead ? Typeface.BOLD : Typeface.NORMAL);
        holder.binding.chapterName.setTextColor(MaterialColors.getColor(holder.binding.chapterName,
                isLastRead ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurface));

        String updated = isLastRead
                ? holder.itemView.getContext().getString(R.string.last_read)
                : item.updated;
        boolean hasUpdated = updated != null && !updated.isEmpty();
        holder.binding.updated.setText(hasUpdated ? updated : null);
        holder.binding.updated.setVisibility(hasUpdated ? View.VISIBLE : View.GONE);

        boolean isPending = DownloadService.isPending(item.url);
        boolean isDownloaded = downloadedUrls.contains(item.url);

        if (isPending) {
            holder.binding.downloadProgress.show();
            holder.binding.downloadBtn.setVisibility(View.GONE);
        } else {
            holder.binding.downloadProgress.hide();
            holder.binding.downloadBtn.setVisibility(View.VISIBLE);
            holder.binding.downloadBtn.setImageResource(isDownloaded ? R.drawable.ic_downloaded : R.drawable.ic_download);
            holder.binding.downloadBtn.setColorFilter(MaterialColors.getColor(holder.binding.downloadBtn,
                    isDownloaded ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurfaceVariant));
            holder.binding.downloadBtn.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnChapterItemClickListener {
        void onChapterItemClick(Chapter item);
    }

    public interface OnChapterDownloadClickListener {
        void onDownloadClick(Chapter chapter);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemChapterBinding binding;

        public MyViewHolder(@NonNull ItemChapterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.chapterItemLayout.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) listener.onChapterItemClick(items.get(position));
            });

            binding.downloadBtn.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (downloadListener != null && position != RecyclerView.NO_POSITION) {
                    downloadListener.onDownloadClick(items.get(position));
                }
            });
        }
    }
}
