package org.ranobe.ranobe.ui.views;

import static android.graphics.PorterDuff.Mode.CLEAR;
import static androidx.recyclerview.widget.ItemTouchHelper.LEFT;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.RecyclerView;

public abstract class RecyclerSwipeHelper extends SimpleCallback {
    public static final String TAG_NO_SWIPE = "don't swipe this item";

    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final int swipeLeftColor;
    private final int swipeRightColor;

    private final Paint clearPaint;
    private final Drawable swipeRightIcon;
    private final Drawable swipeLeftIcon;
    private final ColorDrawable background = new ColorDrawable();


    public RecyclerSwipeHelper(@ColorInt int swipeRightColor, @ColorInt int swipeLeftColor,
                               @DrawableRes int swipeRightIconResource,
                               @DrawableRes int swipeLeftIconResource, Context context) {
        super(0, LEFT | RIGHT);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(CLEAR));

        this.swipeLeftColor = swipeLeftColor;
        this.swipeRightColor = swipeRightColor;

        this.swipeRightIcon = ContextCompat.getDrawable(context, swipeRightIconResource);
        this.swipeLeftIcon = ContextCompat.getDrawable(context, swipeLeftIconResource);

        if (swipeRightIcon == null || swipeLeftIcon == null)
            throw new Resources.NotFoundException("There was an error trying to load the drawables");

        intrinsicHeight = swipeRightIcon.getIntrinsicHeight();
        intrinsicWidth = swipeRightIcon.getIntrinsicWidth();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = (dX == 0f) && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        if (dX < 0) {
            background.setColor(swipeLeftColor);
            background.setBounds((int) (itemView.getRight() + dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);

            int itemTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int itemMargin = (itemHeight - intrinsicHeight) / 2;
            int itemLeft = itemView.getRight() - itemMargin - intrinsicWidth;
            int itemRight = itemView.getRight() - itemMargin;
            int itemBottom = itemTop + intrinsicHeight;

            int alpha = ((int) ((-itemView.getTranslationX() / itemView.getWidth()) * 510));
            if (alpha > 255) alpha = 255;

            swipeLeftIcon.setAlpha(alpha);
            swipeLeftIcon.setBounds(itemLeft, itemTop, itemRight, itemBottom);
            swipeLeftIcon.draw(c);

        } else {
            background.setColor(swipeRightColor);
            background.setBounds((int) (itemView.getLeft() + dX), itemView.getTop(), itemView.getLeft(), itemView.getBottom());
            background.draw(c);

            int itemTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int itemMargin = (itemHeight - intrinsicHeight) / 2;
            int itemLeft = itemView.getLeft() + itemMargin;
            int itemRight = itemView.getLeft() + itemMargin + intrinsicWidth;
            int itemBottom = itemTop + intrinsicHeight;

            int alpha = ((int) ((itemView.getTranslationX() / itemView.getWidth()) * 510));
            if (alpha > 255) alpha = 255;

            swipeRightIcon.setAlpha(alpha);
            swipeRightIcon.setBounds(itemLeft, itemTop, itemRight, itemBottom);
            swipeRightIcon.draw(c);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (TAG_NO_SWIPE.equals(viewHolder.itemView.getTag())) return 0;
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        if (c != null) c.drawRect(left, top, right, bottom, clearPaint);
    }
}
