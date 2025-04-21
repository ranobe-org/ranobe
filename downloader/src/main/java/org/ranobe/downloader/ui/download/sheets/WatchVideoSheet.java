package org.ranobe.downloader.ui.download.sheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.ranobe.downloader.databinding.SheetWatchVideoBinding;

public class WatchVideoSheet extends BottomSheetDialogFragment {
    public static final String TAG = "watch-video-sheet";
    private final OnContinueClicked listener;

    public WatchVideoSheet(OnContinueClicked listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SheetWatchVideoBinding binding = SheetWatchVideoBinding.inflate(inflater, container, false);
        binding.btnContinue.setOnClickListener(v -> {
            this.dismiss();
            listener.onContinue();
        });
        return binding.getRoot();
    }

    public interface OnContinueClicked {
        void onContinue();
    }
}
