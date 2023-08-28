package org.ranobe.downloader.ui.main.sheet;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.ranobe.core.models.DataSource;
import org.ranobe.downloader.databinding.ItemSourceBinding;

import java.util.List;
import java.util.Locale;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.MyViewHolder> {
    private final List<DataSource> sources;

    public SourceAdapter(List<DataSource> sources) {
        this.sources = sources;
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
        holder.binding.tvIndex.setText(String.format(Locale.getDefault(), "%02d", source.sourceId));
        holder.binding.sourceName.setText(source.name);
        holder.binding.sourceContent.setText(source.url);
        Glide.with(holder.binding.sourceLogo.getContext())
                .load(source.logo)
                .into(holder.binding.sourceLogo);
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemSourceBinding binding;

        public MyViewHolder(@NonNull ItemSourceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
