package org.ranobe.ranobe.ui.explore.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
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
        DataSource source = sources.get(position);
        holder.binding.sourceId.setText(String.format(Locale.getDefault(), "%02d", source.sourceId));
        holder.binding.sourceName.setText(source.name);
        holder.binding.sourceContent.setText(String.format(
                Locale.getDefault(),
                "%s • %s", source.lang, source.dev
        ));

        if (!source.isActive) {
            holder.binding.sourceLayout.setAlpha(0.5f);
            Glide.with(holder.binding.sourceLogo.getContext())
                    .load(R.drawable.ic_disabled)
                    .into(holder.binding.sourceLogo);
        } else {
            Glide.with(holder.binding.sourceLogo.getContext())
                    .load(source.logo)
                    .into(holder.binding.sourceLogo);
        }
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

            binding.sourceLayout.setOnClickListener(v -> {
                DataSource source = sources.get(getAdapterPosition());
                if (!source.isActive) {
                    Snackbar.make(v, "This source is no longer active!", Snackbar.LENGTH_SHORT).show();
                } else {
                    listener.select(sources.get(getAdapterPosition()));
                }
            });
        }
    }
}
