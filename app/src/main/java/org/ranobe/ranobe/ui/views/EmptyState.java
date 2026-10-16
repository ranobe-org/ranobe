package org.ranobe.ranobe.ui.views;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import org.ranobe.ranobe.databinding.ViewEmptyStateBinding;

/**
 * Fills in and shows/hides the shared empty state (view_empty_state.xml).
 */
public class EmptyState {
    private EmptyState() {
    }

    public static void show(ViewEmptyStateBinding view, @DrawableRes int icon, @StringRes int title, @StringRes int message) {
        show(view, icon, title, view.getRoot().getContext().getString(message), 0, null);
    }

    public static void show(ViewEmptyStateBinding view, @DrawableRes int icon, @StringRes int title, CharSequence message,
                            @StringRes int action, View.OnClickListener onAction) {
        view.emptyIcon.setImageResource(icon);
        view.emptyTitle.setText(title);
        view.emptyMessage.setText(message);
        view.emptyMessage.setVisibility(message == null || message.length() == 0 ? View.GONE : View.VISIBLE);
        view.emptyAction.setVisibility(onAction != null ? View.VISIBLE : View.GONE);
        if (onAction != null) {
            view.emptyAction.setText(action);
            view.emptyAction.setOnClickListener(onAction);
        }
        if (view.getRoot().getVisibility() != View.VISIBLE) {
            view.getRoot().setAlpha(0f);
            view.getRoot().setVisibility(View.VISIBLE);
            view.getRoot().animate().alpha(1f).setDuration(200).start();
        }
    }

    public static void hide(ViewEmptyStateBinding view) {
        view.getRoot().animate().cancel();
        view.getRoot().setVisibility(View.GONE);
    }
}
