package org.ranobe.ranobe.ui.reader.sheet.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.databinding.ItemReaderThemeBinding;

public class ReaderThemeAdapter extends RecyclerView.Adapter<ReaderThemeAdapter.MyViewHolder> {
    private final OnReaderThemeSelected listener;

    public ReaderThemeAdapter(OnReaderThemeSelected listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReaderThemeBinding binding = ItemReaderThemeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 10;
    }

    public interface OnReaderThemeSelected {
        void select();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ItemReaderThemeBinding binding;
        public MyViewHolder(@NonNull ItemReaderThemeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.textLayout.setOnClickListener(v -> listener.select());
        }
    }
}
