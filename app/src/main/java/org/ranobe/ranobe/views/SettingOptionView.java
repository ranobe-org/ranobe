package org.ranobe.ranobe.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.ViewSettingOptionBinding;

public class SettingOptionView  extends LinearLayout {
    private final ViewSettingOptionBinding binding;

    public SettingOptionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = ViewSettingOptionBinding.inflate(LayoutInflater.from(context), this, true);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SettingOptionView, 0, 0);

        try {
            int icon = a.getResourceId(R.styleable.SettingOptionView_settingIcon, R.drawable.ic_settings);
            String title = a.getString(R.styleable.SettingOptionView_settingTitle);
            String subtitle = a.getString(R.styleable.SettingOptionView_settingSubtitle);

            binding.icon.setImageResource(icon);
            binding.title.setText(title);
            binding.subtitle.setText(subtitle);
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        binding.getRoot().setOnClickListener(l);
    }

    public void setIcon(int resource) {
        binding.icon.setImageResource(resource);
    }
}
