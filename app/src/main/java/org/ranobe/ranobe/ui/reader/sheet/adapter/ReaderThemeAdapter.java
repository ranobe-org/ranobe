package org.ranobe.ranobe.ui.reader.sheet.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ItemReaderThemeBinding;
import org.ranobe.ranobe.models.ReaderTheme;

import java.util.ArrayList;
import java.util.List;

public class ReaderThemeAdapter extends RecyclerView.Adapter<ReaderThemeAdapter.MyViewHolder> {
    private final OnReaderThemeSelected listener;
    private final List<ReaderTheme> themes;
    private final List<String> names;

    public ReaderThemeAdapter(OnReaderThemeSelected listener) {
        this.listener = listener;
        themes = new ArrayList<>(Ranobe.themes.values());
        names = new ArrayList<>(Ranobe.themes.keySet());
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReaderThemeBinding binding = ItemReaderThemeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.binding.textLayout.setCardBackgroundColor(themes.get(position).getBackground());
        holder.binding.textAa.setTextColor(themes.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    public interface OnReaderThemeSelected {
        void select(String s);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemReaderThemeBinding binding;
        public MyViewHolder(@NonNull ItemReaderThemeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.textLayout.setOnClickListener(v -> listener.select(names.get(getAdapterPosition())));
        }
    }
}
