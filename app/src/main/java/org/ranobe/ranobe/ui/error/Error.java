package org.ranobe.ranobe.ui.error;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.FragmentErrorBinding;
import org.ranobe.ranobe.util.NumberUtils;

public class Error extends Fragment {
    private static final String ERR_PARAM = "error";
    private String error;

    public Error() {
        // Required empty public constructor
    }

    public static void navigateToErrorFragment(Activity activity, String error) {
        NavController controller = Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putString(Error.ERR_PARAM, error);
        controller.navigate(R.id.error_fragment, bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            error = getArguments().getString(ERR_PARAM);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentErrorBinding binding = FragmentErrorBinding.inflate(inflater, container, false);
        binding.emoji.setText(Ranobe.SILLY_EMOJI[NumberUtils.getRandom(Ranobe.SILLY_EMOJI.length)]);
        binding.back.setOnClickListener(v -> goBack());
        if (error == null) {
            binding.error.setText(R.string.no_results_err);
        } else {
            binding.error.setText(error);
        }
        return binding.getRoot();
    }

    private void goBack() {
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        controller.navigateUp();
    }
}