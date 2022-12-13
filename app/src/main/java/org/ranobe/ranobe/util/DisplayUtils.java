package org.ranobe.ranobe.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

public class DisplayUtils {
    private final int width;
    private final DisplayMetrics metrics;
    private int empty;

    public DisplayUtils(Context context, int viewId) {
        View view = View.inflate(context, viewId, null);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        width = view.getMeasuredWidth();
        metrics = context.getResources().getDisplayMetrics();
    }

    public int noOfCols() {
        int noOfCols = metrics.widthPixels / width;
        empty = metrics.widthPixels - (noOfCols * width);

        if (empty / (2 * noOfCols) < 5) {
            noOfCols--;
            empty = metrics.widthPixels - (noOfCols * width);
        }
        return noOfCols;
    }

    public int spacing() {
        int noOfCols = noOfCols();
        return empty / (2 * noOfCols);
    }
}
