package org.ranobe.ranobe.ui.error;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.FragmentErrorBinding;
import org.ranobe.ranobe.network.Cloudflare;
import org.ranobe.ranobe.ui.views.EmptyState;
import org.ranobe.ranobe.ui.webview.WebViewActivity;

public class Error extends Fragment {
    private static final String ERR_PARAM = "error";
    private final ActivityResultLauncher<String> verify =
            registerForActivityResult(new WebViewActivity.Verify(), passed -> goBack());
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
        String message = error == null || error.trim().isEmpty() ? getString(R.string.no_results_err) : error;
        EmptyState.show(binding.emptyState, R.drawable.ic_close, R.string.error_title, message,
                R.string.retry, v -> goBack());

        // after a Cloudflare check the previous screen loads again on its own
        String blocked = Cloudflare.blockedUrl(error);
        binding.verify.setVisibility(blocked != null ? View.VISIBLE : View.GONE);
        binding.verify.setOnClickListener(v -> verify.launch(blocked));
        return binding.getRoot();
    }

    private void goBack() {
        if (!isAdded()) return;
        NavController controller = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        controller.navigateUp();
    }
}