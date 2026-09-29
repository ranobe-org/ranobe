package org.ranobe.ranobe.ui.explore.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ItemSourceBinding;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Lang;

import java.util.List;
import java.util.Locale;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.MyViewHolder> {
    private static final float DISABLED_ALPHA = 0.45f;

    private final List<DataSource> sources;
    private final OnSourceSelected listener;
    private final OnSourceToggled toggleListener;
    // browsing shows a chevron per row; managing swaps it for the on/off switch
    private boolean managing = false;

    public SourceAdapter(List<DataSource> sources, OnSourceSelected listener) {
        this(sources, listener, null);
    }

    public SourceAdapter(List<DataSource> sources, OnSourceSelected listener, OnSourceToggled toggleListener) {
        this.sources = sources;
        this.listener = listener;
        this.toggleListener = toggleListener;
    }

    private static String languageName(String lang) {
        if (Lang.eng.equals(lang)) return "English";
        if (Lang.ru.equals(lang)) return "Русский";
        return lang;
    }

    public boolean isManaging() {
        return managing;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setManaging(boolean managing) {
        this.managing = managing && toggleListener != null;
        notifyDataSetChanged();
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
        holder.binding.sourceName.setText(source.name);
        holder.binding.sourceInitial.setText(source.name == null || source.name.isEmpty()
                ? "" : source.name.substring(0, 1).toUpperCase(Locale.getDefault()));

        if (!source.isActive) {
            holder.binding.sourceContent.setText(R.string.source_inactive);
            holder.binding.sourceChevron.setVisibility(View.GONE);
            holder.binding.sourceToggle.setVisibility(View.GONE);
            setDimmed(holder, true);
            holder.binding.sourceInitial.setVisibility(View.INVISIBLE);
            Glide.with(holder.binding.sourceLogo.getContext())
                    .load(R.drawable.ic_disabled)
                    .into(holder.binding.sourceLogo);
            return;
        }

        loadLogo(holder, source.logo);
        boolean enabled = toggleListener == null || Ranobe.isSourceEnabled(source.sourceId);
        bindState(holder, source, enabled);

        holder.binding.sourceToggle.setVisibility(managing ? View.VISIBLE : View.GONE);
        // Set state without triggering listener
        holder.binding.sourceToggle.setOnCheckedChangeListener(null);
        holder.binding.sourceToggle.setChecked(enabled);
        if (managing) {
            holder.binding.sourceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                toggleListener.toggle(source, isChecked);
                bindState(holder, source, isChecked);
            });
        }
    }

    private void bindState(MyViewHolder holder, DataSource source, boolean enabled) {
        String details = String.format(Locale.getDefault(), "%s • %s", languageName(source.lang), source.dev);
        boolean showOff = !enabled && !managing;
        holder.binding.sourceContent.setText(showOff
                ? holder.itemView.getContext().getString(R.string.source_off) + " • " + details
                : details);
        holder.binding.sourceChevron.setVisibility(!managing && enabled ? View.VISIBLE : View.GONE);
        setDimmed(holder, !enabled);
    }

    // only the logo and text fade, so the switch stays clearly usable
    private void setDimmed(MyViewHolder holder, boolean dimmed) {
        float alpha = dimmed ? DISABLED_ALPHA : 1f;
        holder.binding.sourceLogo.setAlpha(alpha);
        holder.binding.sourceInitial.setAlpha(alpha);
        holder.binding.sourceText.setAlpha(alpha);
    }

    // the initial stays visible behind the logo until it loads, and remains if the logo fails
    private void loadLogo(MyViewHolder holder, String logo) {
        holder.binding.sourceInitial.setVisibility(View.VISIBLE);
        Glide.with(holder.binding.sourceLogo.getContext())
                .load(logo)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        holder.binding.sourceInitial.setVisibility(View.INVISIBLE);
                        return false;
                    }
                })
                .into(holder.binding.sourceLogo);
    }

    @Override
    public int getItemCount() {
        return sources.size();
    }

    public interface OnSourceSelected {
        void select(DataSource source);
    }

    public interface OnSourceToggled {
        void toggle(DataSource source, boolean enabled);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemSourceBinding binding;

        public MyViewHolder(@NonNull ItemSourceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.sourceLayout.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                DataSource source = sources.get(position);
                if (managing && source.isActive) {
                    binding.sourceToggle.toggle();
                } else if (!source.isActive) {
                    Snackbar.make(v, R.string.source_inactive, Snackbar.LENGTH_SHORT).show();
                } else if (!Ranobe.isSourceEnabled(source.sourceId)) {
                    Snackbar.make(v, v.getContext().getString(R.string.source_disabled, source.name), Snackbar.LENGTH_SHORT)
                            .setAction(R.string.enable, a -> {
                                // the switch is hidden outside manage mode, so save directly and rebind
                                if (toggleListener != null) toggleListener.toggle(source, true);
                                int current = getAdapterPosition();
                                if (current != RecyclerView.NO_POSITION) notifyItemChanged(current);
                            })
                            .show();
                } else {
                    listener.select(source);
                }
            });
        }
    }
}
