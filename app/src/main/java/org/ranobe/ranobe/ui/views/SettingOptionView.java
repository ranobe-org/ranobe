package org.ranobe.ranobe.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.ViewSettingOptionBinding;

public class SettingOptionView extends LinearLayout {
    private final ViewSettingOptionBinding binding;

    public SettingOptionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = ViewSettingOptionBinding.inflate(LayoutInflater.from(context), this, true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingOptionView, 0, 0);

        try {
            int icon = a.getResourceId(R.styleable.SettingOptionView_settingIcon, R.drawable.ic_settings);
            String title = a.getString(R.styleable.SettingOptionView_settingTitle);
            String subtitle = a.getString(R.styleable.SettingOptionView_settingSubtitle);

            binding.icon.setImageResource(icon);
            binding.title.setText(title);
            binding.subtitle.setText(subtitle);
            binding.subtitle.setVisibility(subtitle == null || subtitle.isEmpty() ? View.GONE : View.VISIBLE);
            boolean external = a.getBoolean(R.styleable.SettingOptionView_settingExternal, false);
            binding.external.setVisibility(external ? View.VISIBLE : View.GONE);
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        binding.getRoot().setOnClickListener(l);
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        // may run from a super constructor, before the row is inflated
        if (binding == null) return;
        binding.getRoot().setClickable(clickable);
        binding.getRoot().setFocusable(clickable);
        // drop the ripple so a label-only row doesn't look tappable
        if (!clickable) binding.getRoot().setBackground(null);
    }

    public void setIcon(int resource) {
        binding.icon.setImageResource(resource);
    }

    public void setChecked(boolean checked) {
        binding.toggle.setVisibility(View.VISIBLE);
        binding.toggle.setChecked(checked);
    }
}
