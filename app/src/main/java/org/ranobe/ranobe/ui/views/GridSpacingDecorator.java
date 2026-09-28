package org.ranobe.ranobe.ui.views;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// equal gutters between columns and at both edges, so every column gets the same width
public class GridSpacingDecorator extends RecyclerView.ItemDecoration {
    private final int spanCount;
    private final int spacing;

    public GridSpacingDecorator(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;
        int column = position % spanCount;

        outRect.left = spacing - column * spacing / spanCount;
        outRect.right = (column + 1) * spacing / spanCount;
        outRect.top = position < spanCount ? spacing : 0;
        outRect.bottom = spacing + spacing / 2;
    }
}
