package org.ranobe.ranobe.ui.history.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.ItemHistoryBinding;
import org.ranobe.ranobe.databinding.ItemHistoryHeaderBinding;
import org.ranobe.ranobe.models.ReadHistory;
import org.ranobe.ranobe.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class HistoryAdapter extends ListAdapter<HistoryAdapter.Row, RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_HISTORY = 1;

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row oldItem, @NonNull Row newItem) {
            if (oldItem.history == null || newItem.history == null) {
                return oldItem.header == newItem.header;
            }
            // one row per novel, so the novel identifies the row even when the chapter changes
            return Objects.equals(oldItem.history.novelUrl, newItem.history.novelUrl);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row oldItem, @NonNull Row newItem) {
            if (oldItem.history == null || newItem.history == null) {
                return oldItem.header == newItem.header;
            }
            ReadHistory a = oldItem.history;
            ReadHistory b = newItem.history;
            return Objects.equals(a.url, b.url)
                    && a.timestamp == b.timestamp
                    && Objects.equals(a.name, b.name)
                    && Objects.equals(a.novelName, b.novelName)
                    && Objects.equals(a.cover, b.cover);
        }
    };

    private final Listener listener;

    public HistoryAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @StringRes
    private static int sectionFor(long timestamp, long today) {
        long day = 24L * 60 * 60 * 1000;
        if (timestamp >= today) return R.string.history_today;
        if (timestamp >= today - day) return R.string.history_yesterday;
        if (timestamp >= today - 6 * day) return R.string.history_this_week;
        return R.string.history_earlier;
    }

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // list is newest first, so each section header is emitted once when the day bucket changes
    public void submitHistory(List<ReadHistory> histories) {
        List<Row> rows = new ArrayList<>();
        int lastHeader = 0;
        long today = startOfToday();
        for (ReadHistory history : histories) {
            int header = sectionFor(history.timestamp, today);
            if (header != lastHeader) {
                rows.add(new Row(header, null));
                lastHeader = header;
            }
            rows.add(new Row(0, history));
        }
        submitList(rows);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).history == null ? TYPE_HEADER : TYPE_HISTORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(ItemHistoryHeaderBinding.inflate(inflater, parent, false));
        }
        return new ViewHolder(ItemHistoryBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.header.setText(row.header);
            return;
        }

        ReadHistory item = row.history;
        ItemHistoryBinding binding = ((ViewHolder) holder).itemBinding;
        Glide.with(binding.novelCover.getContext())
                .load(item.cover)
                .centerCrop()
                .into(binding.novelCover);

        binding.novelTitle.setText(item.novelName);
        binding.lastReadChapter.setText(item.name);
        binding.lastReadTimestamp.setText(DateUtils.getRelativeTime(item.timestamp));
    }

    public interface Listener {
        void onContinueReading(ReadHistory history);

        void onOpenDetails(ReadHistory history);

        void onRemove(ReadHistory history);
    }

    public static class Row {
        @StringRes
        final int header;
        final ReadHistory history;

        Row(@StringRes int header, ReadHistory history) {
            this.header = header;
            this.history = history;
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryHeaderBinding binding;

        HeaderHolder(@NonNull ItemHistoryHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding itemBinding;

        public ViewHolder(@NonNull ItemHistoryBinding itemView) {
            super(itemView.getRoot());
            itemBinding = itemView;

            itemBinding.readChapter.setOnClickListener(v -> {
                ReadHistory history = current();
                if (history != null) listener.onContinueReading(history);
            });
            itemBinding.novelCoverLayout.setOnClickListener(v -> {
                ReadHistory history = current();
                if (history != null) listener.onOpenDetails(history);
            });
            itemBinding.removeHistory.setOnClickListener(v -> {
                ReadHistory history = current();
                if (history != null) listener.onRemove(history);
            });
            itemBinding.readChapter.setOnLongClickListener(v -> {
                ReadHistory history = current();
                if (history != null) listener.onRemove(history);
                return true;
            });
        }

        // the row can be clicked mid-animation after the list changed, when it has no position
        private ReadHistory current() {
            int position = getAdapterPosition();
            return position == RecyclerView.NO_POSITION ? null : getItem(position).history;
        }
    }
}
