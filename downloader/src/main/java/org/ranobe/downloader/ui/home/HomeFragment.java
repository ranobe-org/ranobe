package org.ranobe.downloader.ui.home;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import org.ranobe.core.sources.Source;
import org.ranobe.core.sources.SourceManager;
import org.ranobe.downloader.R;
import org.ranobe.downloader.config.Config;
import org.ranobe.downloader.config.Utils;
import org.ranobe.downloader.databinding.FragmentHomeBinding;
import org.ranobe.downloader.ui.download.DownloadActivity;
import org.ranobe.downloader.ui.main.sheet.SourceSheet;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.seeSources.setOnClickListener(v -> showSources());
        binding.search.setOnClickListener(v -> handleSearch());
        binding.paste.setOnClickListener(v -> pasteFromClipboard());
        binding.searchView.setEndIconOnClickListener(v -> searchNovels());
        return binding.getRoot();
    }

    private void searchNovels() {
        binding.searchField.clearFocus();
        if (binding.searchField.getText() != null && binding.searchField.getText().toString().length() > 0) {
            String keyword = binding.searchField.getText().toString();
            Bundle bundle = new Bundle();
            bundle.putString(Config.KEY_KEYWORD, keyword);
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.search_fragment, bundle);
        }
    }

    private void showSources() {
        SourceSheet sheet = new SourceSheet();
        sheet.show(getParentFragmentManager(), SourceSheet.TAG);
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            CharSequence textToPaste = clipboard.getPrimaryClip().getItemAt(0).getText();
            binding.edLink.setText(textToPaste);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handleLinkField() {
        if (binding.edLink.getText() == null || binding.edLink.getText().toString().length() == 0) {
            binding.tlLink.setError(getString(R.string.please_paste_a_link));
            return true;
        }
        return false;
    }

    private void handleSearch() {
        if (handleLinkField()) return;

        String url = binding.edLink.getText().toString();
        boolean isSupported = checkIfUrlIsSupported(url);
        if (!isSupported) {
            showError(R.string.source_not_supported);
        } else {
            handleSupportedUrl(url);
        }
    }

    private void handleSupportedUrl(String url) {
        startActivity(new Intent(requireActivity(), DownloadActivity.class)
                .putExtra(Config.KEY_URL, url));
    }

    private void showError(int id) {
        binding.tlLink.setError(getString(id));
    }

    private boolean checkIfUrlIsSupported(String url) {
        if (!Utils.isValidURL(url)) {
            showError(R.string.invalid_url);
            return false;
        }
        String domain = Utils.getDomainName(url);
        Source source = SourceManager.getSourceByDomain(domain);
        if (source == null) {
            showError(R.string.website_not_supported);
            return false;
        }
        return true;
    }
}
