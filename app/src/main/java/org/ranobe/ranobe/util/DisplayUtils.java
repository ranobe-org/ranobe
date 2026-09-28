package org.ranobe.ranobe.util;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.ranobe.ranobe.ui.views.GridSpacingDecorator;

public class DisplayUtils {
    private static final int MIN_COLUMN_WIDTH_DP = 112;
    private static final int MIN_COLUMNS = 3;
    private static final int GRID_SPACING_DP = 12;

    private DisplayUtils() {
    }

    // as many columns as fit at the minimum width, but never fewer than three
    public static int novelGridColumns(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        return Math.max(MIN_COLUMNS, widthDp / MIN_COLUMN_WIDTH_DP);
    }

    // sets up a cover grid for item_novel_grid tiles (NovelAdapter#asGrid)
    public static GridLayoutManager applyNovelGrid(RecyclerView list) {
        Context context = list.getContext();
        int columns = novelGridColumns(context);
        int spacing = (int) (GRID_SPACING_DP * context.getResources().getDisplayMetrics().density);

        GridLayoutManager layoutManager = new GridLayoutManager(context, columns);
        list.setLayoutManager(layoutManager);
        list.addItemDecoration(new GridSpacingDecorator(columns, spacing));
        return layoutManager;
    }
}
