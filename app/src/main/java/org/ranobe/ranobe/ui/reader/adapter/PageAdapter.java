package org.ranobe.ranobe.ui.reader.adapter;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;

import org.ranobe.ranobe.App;
import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ItemPageBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ReaderTheme;
import org.ranobe.ranobe.ui.views.BionicReadingTextView;
import org.ranobe.ranobe.util.SourceUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public class PageAdapter extends RecyclerView.Adapter<PageAdapter.MyViewHolder> {
    private final List<Chapter> chapters;
    private final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
    );
    private ReaderTheme theme;
    private float fontSize;
    private boolean isBionicReading;
    private boolean showImages;

    public PageAdapter(List<Chapter> chapters) {
        this.chapters = chapters;
        this.theme = Ranobe.themes.get(Ranobe.getReaderTheme(App.getContext()));
        this.fontSize = Ranobe.getReaderFont(App.getContext());
        this.isBionicReading = Ranobe.getBionicReader();
        this.showImages = Ranobe.getShowImages();
    }

    public void setTheme(ReaderTheme theme) {
        this.theme = theme;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public void setBionicReading(boolean isBionicReading) {
        this.isBionicReading = isBionicReading;
    }

    public void setShowImages(boolean showImages) {
        this.showImages = showImages;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPageBinding binding = ItemPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Chapter chapter = chapters.get(position);

        if (theme != null) {
            holder.binding.pageLayout.setBackgroundColor(theme.getBackground());
        } else {
            holder.binding.pageLayout.setBackgroundColor(
                    getThemeColor(holder.binding.pageLayout, com.google.android.material.R.attr.colorSurface)
            );
        }

        holder.binding.chapterTitle.setText(String.format(Locale.getDefault(), "Chapter %.1f", chapter.id));
        holder.binding.pageLayout.setLayoutParams(params);
        bindContent(holder.binding.content, chapter.content == null ? "" : chapter.content);
    }

    private void bindContent(LinearLayout container, String content) {
        clearContent(container);
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        Matcher matcher = SourceUtils.IMAGE_TAG.matcher(content);
        int last = 0;
        while (matcher.find()) {
            addText(inflater, container, content.substring(last, matcher.start()));
            if (showImages) addImage(inflater, container, matcher.group(1));
            last = matcher.end();
        }
        addText(inflater, container, content.substring(last));
    }

    private void addText(LayoutInflater inflater, LinearLayout container, String text) {
        text = text.trim();
        if (text.isEmpty()) return;

        BionicReadingTextView view = (BionicReadingTextView) inflater.inflate(R.layout.item_page_text, container, false);
        if (theme != null) {
            view.setTextColor(theme.getText());
        } else {
            view.setTextColor(getThemeColor(view, com.google.android.material.R.attr.colorOnSurface));
        }
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        view.setText(text);
        view.setBionicReading(isBionicReading);
        container.addView(view);
    }

    private void addImage(LayoutInflater inflater, LinearLayout container, String url) {
        ImageView view = (ImageView) inflater.inflate(R.layout.item_page_image, container, false);
        container.addView(view);
        if (url.startsWith("/")) {
            Glide.with(view.getContext()).load(new File(url)).into(view);
        } else {
            Glide.with(view.getContext()).load(url).into(view);
        }
    }

    private void clearContent(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof ImageView) {
                Glide.with(child.getContext()).clear(child);
            }
        }
        container.removeAllViews();
    }

    private int getThemeColor(View view, int attr) {
        return MaterialColors.getColor(view, attr);
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final ItemPageBinding binding;

        public MyViewHolder(@NonNull ItemPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
