package org.ranobe.ranobe.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentSettingsBinding;
import org.ranobe.ranobe.ui.settings.adapter.AccentAdapter;

public class Settings extends Fragment {

    private FragmentSettingsBinding binding;

    public Settings() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        binding.themeModeOption.setOnClickListener(v -> binding.themeModeView.setVisibility(getPolarVisibility(binding.themeModeView)));
        binding.accentOption.setOnClickListener(v -> binding.accentView.setVisibility(getPolarVisibility(binding.accentView)));
        binding.lightChip.setOnClickListener(v -> selectTheme(AppCompatDelegate.MODE_NIGHT_NO));
        binding.nightChip.setOnClickListener(v -> selectTheme(AppCompatDelegate.MODE_NIGHT_YES));
        binding.autoChip.setOnClickListener(v -> selectTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        binding.accentView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.accentView.setAdapter(new AccentAdapter(requireActivity()));

        setCurrentThemeMode();
        return binding.getRoot();
    }

    private void setCurrentThemeMode() {
        int mode = Ranobe.getThemeMode(requireActivity().getApplicationContext());
        if (mode == AppCompatDelegate.MODE_NIGHT_NO)
            binding.currentThemeMode.setImageResource(R.drawable.ic_theme_mode_light);
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            binding.currentThemeMode.setImageResource(R.drawable.ic_theme_mode_night);
        else
            binding.currentThemeMode.setImageResource(R.drawable.ic_theme_mode_auto);
    }

    private int getPolarVisibility(View view) {
        int mode = view.getVisibility();
        return mode == View.VISIBLE ? View.GONE : View.VISIBLE;
    }

    private void selectTheme(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme);
        Ranobe.storeThemeMode(requireActivity().getApplicationContext(), theme);
    }
}
