package org.ranobe.ranobe.ui.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.databinding.ItemLoadFooterBinding;

import java.util.Objects;

/**
 * The single row after a paged list (use it in a ConcatAdapter): a spinner while the next page
 * loads, the error with a retry button when it failed, or a note that there is nothing more.
 * It has no row at all otherwise.
 */
public class LoadFooterAdapter extends RecyclerView.Adapter<LoadFooterAdapter.Holder> {
    public enum Mode {NONE, LOADING, FAILED, END}

    private final Runnable onRetry;
    private Mode mode = Mode.NONE;
    private String message;

    public LoadFooterAdapter(Runnable onRetry) {
        this.onRetry = onRetry;
    }

    public void set(Mode next, String nextMessage) {
        if (next == mode && Objects.equals(nextMessage, message)) return;
        boolean had = mode != Mode.NONE;
        boolean has = next != Mode.NONE;
        mode = next;
        message = nextMessage;
        if (had && has) notifyItemChanged(0);
        else if (had) notifyItemRemoved(0);
        else if (has) notifyItemInserted(0);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLoadFooterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.binding.progress.setVisibility(mode == Mode.LOADING ? View.VISIBLE : View.GONE);
        holder.binding.failed.setVisibility(mode == Mode.FAILED ? View.VISIBLE : View.GONE);
        holder.binding.end.setVisibility(mode == Mode.END ? View.VISIBLE : View.GONE);
        if (mode == Mode.FAILED && message != null) holder.binding.message.setText(message);
        holder.binding.retry.setOnClickListener(v -> onRetry.run());
    }

    @Override
    public int getItemCount() {
        return mode == Mode.NONE ? 0 : 1;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemLoadFooterBinding binding;

        Holder(ItemLoadFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
