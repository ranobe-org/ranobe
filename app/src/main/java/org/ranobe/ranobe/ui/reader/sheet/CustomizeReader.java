package org.ranobe.ranobe.ui.reader.sheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.ranobe.ranobe.App;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.SheetCustomizeReaderBinding;
import org.ranobe.ranobe.ui.reader.sheet.adapter.ReaderThemeAdapter;

public class CustomizeReader extends BottomSheetDialogFragment implements ReaderThemeAdapter.OnReaderThemeSelected {
    private final OnOptionSelection listener;

    public CustomizeReader(OnOptionSelection listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SheetCustomizeReaderBinding binding = SheetCustomizeReaderBinding.inflate(inflater, container, false);

        binding.fontSlider.setValue(Ranobe.getReaderFont(App.getContext()));
        binding.fontSlider.addOnChangeListener((slider, value, fromUser) -> listener.setFontSize(value));

        binding.readerThemeList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.readerThemeList.setAdapter(new ReaderThemeAdapter(this));

        binding.bionicReadingToggle.setChecked(Ranobe.getBionicReader());
        binding.bionicReadingToggle.setOnCheckedChangeListener((cb, b) -> listener.setBionicReading(b));

        binding.volumeScrollToggle.setChecked(Ranobe.isVolumeKeyScrollEnabled());
        binding.scrollSpeedSetting.getRoot().setVisibility(Ranobe.isVolumeKeyScrollEnabled() ? View.VISIBLE : View.GONE);
        binding.volumeScrollToggle.setOnCheckedChangeListener((cb, b) -> {
            binding.scrollSpeedSetting.getRoot().setVisibility(b ? View.VISIBLE : View.GONE);
            listener.setVolumeKeyScroll(b);
        });

        int currentSpeed = Ranobe.getVolumeScrollSpeed();
        binding.scrollSpeedSetting.speedSlider.setValue(currentSpeed);
        binding.scrollSpeedSetting.speedLabel.setText(Ranobe.getSpeedLabel(requireContext(), currentSpeed));
        binding.scrollSpeedSetting.speedSlider.setLabelFormatter(value -> Ranobe.getSpeedLabel(requireContext(), (int) value));
        binding.scrollSpeedSetting.speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser) return;
            int speed = (int) value;
            Ranobe.setVolumeScrollSpeed(requireContext(), speed);
            binding.scrollSpeedSetting.speedLabel.setText(Ranobe.getSpeedLabel(requireContext(), speed));
            listener.setVolumeScrollSpeed(speed);
        });
        binding.showImagesToggle.setChecked(Ranobe.getShowImages());
        binding.showImagesToggle.setOnCheckedChangeListener((cb, b) -> listener.setShowImages(b));
        return binding.getRoot();
    }

    @Override
    public void select(String themeName) {
        listener.setReaderTheme(themeName);
    }

    public interface OnOptionSelection {
        void setFontSize(float size);

        void setReaderTheme(String themeName);

        void setBionicReading(boolean isBionicReading);

        void setVolumeKeyScroll(boolean isVolumeKeyScroll);

        void setVolumeScrollSpeed(int speed);
    
        void setShowImages(boolean showImages);
    }
}
