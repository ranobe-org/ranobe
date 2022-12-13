package org.ranobe.ranobe.ui.settings.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ItemAccentBinding;
import org.ranobe.ranobe.util.ThemeUtils;

import java.util.List;

public class AccentAdapter extends RecyclerView.Adapter<AccentAdapter.MyViewHolder> {

    private final Activity activity;
    public List<Integer> accentList;

    public AccentAdapter(Activity activity) {
        this.accentList = Ranobe.ACCENT_LIST;
        this.activity = activity;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccentBinding binding = ItemAccentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.binding.accent.setCardBackgroundColor(activity.getResources().getColor(accentList.get(position)));
    }

    @Override
    public int getItemCount() {
        return accentList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemAccentBinding binding;
        public MyViewHolder(@NonNull ItemAccentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.accent.setOnClickListener(v -> {
                Ranobe.storeTheme(activity.getApplicationContext(), accentList.get(getAdapterPosition()));
                ThemeUtils.applySettings(activity);
            });
        }
    }
}
