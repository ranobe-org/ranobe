package org.ranobe.ranobe.ui.explore.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ranobe.ranobe.databinding.ItemSourceBinding;
import org.ranobe.ranobe.models.DataSource;

import java.util.List;
import java.util.Locale;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.MyViewHolder> {
    private final List<DataSource> sources;
    private final OnSourceSelected listener;

    public SourceAdapter(List<DataSource> sources, OnSourceSelected listener) {
        this.sources = sources;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSourceBinding binding = ItemSourceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.binding.sourceName.setText(sources.get(position).name);
        holder.binding.sourceContent.setText(String.format(
                Locale.getDefault(),
                "%s • %s", sources.get(position).lang, sources.get(position).dev
        ));
        Glide.with(holder.binding.sourceLogo.getContext())
                .load(sources.get(position).logo)
                .into(holder.binding.sourceLogo);
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    public interface OnSourceSelected {
        void select(DataSource source);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemSourceBinding binding;
        public MyViewHolder(@NonNull ItemSourceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.sourceLayout.setOnClickListener(v -> listener.select(sources.get(getAdapterPosition())));
        }
    }
}
