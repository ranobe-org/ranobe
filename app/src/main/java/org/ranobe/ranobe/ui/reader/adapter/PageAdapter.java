package org.ranobe.ranobe.ui.reader.adapter;

import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;

import org.ranobe.ranobe.App;
import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ItemPageHeaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ReaderTheme;
import org.ranobe.ranobe.ui.views.BionicReadingTextView;
import org.ranobe.ranobe.util.SourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chapters are flattened into small rows (a heading, then one row per paragraph or image) instead of
 * one view per chapter, so the list can recycle views and a font or theme change only rebinds what is
 * on screen rather than re-laying out whole chapters.
 */
public class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TEXT = 1;
    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_END = 3;
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n");

    private final List<Item> items = new ArrayList<>();
    private ReaderTheme theme;
    private float fontSize;
    private boolean isBionicReading;
    private boolean showImages;

    public PageAdapter() {
        this.theme = Ranobe.themes.get(Ranobe.getReaderTheme(App.getContext()));
        this.fontSize = Ranobe.getReaderFont(App.getContext());
        this.isBionicReading = Ranobe.getBionicReader();
        this.showImages = Ranobe.getShowImages();
    }

    private static String titleOf(Chapter chapter) {
        if (chapter.name != null && !chapter.name.trim().isEmpty()) return chapter.name.trim();
        return String.format(Locale.getDefault(), "Chapter %s", cleanNumber(chapter.id));
    }

    // "1.0" reads as "1", "2.5" stays "2.5"
    private static String cleanNumber(float value) {
        return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
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

    // adds the chapter's rows at the end and returns the position of its heading
    public int appendChapter(Chapter chapter) {
        int start = items.size();
        items.add(new Item(TYPE_HEADER, chapter, titleOf(chapter), 0));

        String content = chapter.content == null ? "" : chapter.content;
        Matcher matcher = SourceUtils.IMAGE_TAG.matcher(content);
        int last = 0;
        while (matcher.find()) {
            addParagraphs(chapter, content.substring(last, matcher.start()));
            items.add(new Item(TYPE_IMAGE, chapter, matcher.group(1), items.size() - start));
            last = matcher.end();
        }
        addParagraphs(chapter, content.substring(last));

        notifyItemRangeInserted(start, items.size() - start);
        return start;
    }

    private void addParagraphs(Chapter chapter, String text) {
        int start = indexOfHeader(chapter);
        for (String paragraph : PARAGRAPH_BREAK.split(text)) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) items.add(new Item(TYPE_TEXT, chapter, trimmed, items.size() - start));
        }
    }

    private int indexOfHeader(Chapter chapter) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item.type == TYPE_HEADER && item.chapter == chapter) return i;
        }
        return items.size();
    }

    public void appendEnd() {
        items.add(new Item(TYPE_END, null, null, 0));
        notifyItemInserted(items.size() - 1);
    }

    // the chapter the row at this position belongs to (null for the end row)
    public Chapter chapterAt(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position).chapter;
    }

    // position of the row inside its chapter, where the heading is 0
    public int indexInChapter(int position) {
        if (position < 0 || position >= items.size()) return 0;
        return items.get(position).indexInChapter;
    }

    public int chapterRowCount(int headerPosition) {
        int count = 0;
        Chapter chapter = chapterAt(headerPosition);
        for (int i = headerPosition; i < items.size() && items.get(i).chapter == chapter; i++) count++;
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderHolder(ItemPageHeaderBinding.inflate(inflater, parent, false));
            case TYPE_IMAGE:
                return new SimpleHolder(inflater.inflate(R.layout.item_page_image, parent, false));
            case TYPE_END:
                return new SimpleHolder(inflater.inflate(R.layout.item_page_end, parent, false));
            case TYPE_TEXT:
            default:
                return new SimpleHolder(inflater.inflate(R.layout.item_page_text, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        switch (item.type) {
            case TYPE_HEADER:
                bindHeader((HeaderHolder) holder, item, position);
                break;
            case TYPE_IMAGE:
                bindImage((ImageView) holder.itemView, item.value);
                break;
            case TYPE_END:
                ((TextView) holder.itemView).setTextColor(ColorUtils.setAlphaComponent(textColor(holder.itemView), 0xAA));
                break;
            case TYPE_TEXT:
            default:
                bindText((BionicReadingTextView) holder.itemView, item.value);
                break;
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder.itemView instanceof ImageView) {
            Glide.with(holder.itemView.getContext()).clear(holder.itemView);
        }
    }

    private void bindHeader(HeaderHolder holder, Item item, int position) {
        int color = textColor(holder.itemView);
        holder.binding.chapterTitle.setText(item.value);
        holder.binding.chapterTitle.setTextColor(color);
        holder.binding.chapterDivider.setBackgroundTintList(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color, 0x40)));
        holder.binding.chapterDivider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
    }

    private void bindText(BionicReadingTextView view, String text) {
        view.setTextColor(textColor(view));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        view.setText(text);
        view.setBionicReading(isBionicReading);
    }

    // hidden images keep their row (at zero height) so positions and saved offsets stay stable
    private void bindImage(ImageView view, String url) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = showImages ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
        view.setLayoutParams(params);
        view.setVisibility(showImages ? View.VISIBLE : View.GONE);
        if (!showImages) {
            Glide.with(view.getContext()).clear(view);
            return;
        }
        if (url.startsWith("/")) {
            Glide.with(view.getContext()).load(new File(url)).into(view);
        } else {
            Glide.with(view.getContext()).load(url).into(view);
        }
    }

    private int textColor(View view) {
        return theme != null ? theme.getText()
                : MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class Item {
        final int type;
        final Chapter chapter;
        final String value;
        final int indexInChapter;

        Item(int type, Chapter chapter, String value, int indexInChapter) {
            this.type = type;
            this.chapter = chapter;
            this.value = value;
            this.indexInChapter = indexInChapter;
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemPageHeaderBinding binding;

        HeaderHolder(ItemPageHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class SimpleHolder extends RecyclerView.ViewHolder {
        SimpleHolder(View view) {
            super(view);
        }
    }
}
