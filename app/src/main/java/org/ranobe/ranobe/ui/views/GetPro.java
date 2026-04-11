package org.ranobe.ranobe.ui.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentGetProBinding;

public class GetPro extends BottomSheetDialogFragment {

    public static final String TAG = "get-pro-sheet";

    private FragmentGetProBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGetProBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_text, Ranobe.PRO_APP_FEATURES);
        binding.featuresList.setAdapter(adapter);
        binding.getProButton.setOnClickListener(v -> openLink());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void openLink() {
        requireActivity().startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Ranobe.RANOBE_PRO_APP_LINK)
        ));
    }
}
