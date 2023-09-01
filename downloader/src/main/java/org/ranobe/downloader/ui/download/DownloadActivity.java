package org.ranobe.downloader.ui.download;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.core.models.Novel;
import org.ranobe.core.sources.Source;
import org.ranobe.core.sources.SourceManager;
import org.ranobe.downloader.config.Config;
import org.ranobe.downloader.config.Utils;
import org.ranobe.downloader.databinding.ActivityDownloadBinding;
import org.ranobe.downloader.ui.download.sheets.SetFileLocationSheet;
import org.ranobe.downloader.writer.EpubExporter;

import java.util.Locale;
import java.util.Objects;

public class DownloadActivity extends AppCompatActivity {
    private ActivityDownloadBinding binding;
    private ActivityResultLauncher<Intent> documentLauncher;

    private DownloadViewModel viewModel;
    private String url;
    private Source source;
    private Novel novel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("download");
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        viewModel = new ViewModelProvider(DownloadActivity.this).get(DownloadViewModel.class);
        viewModel.getError().observe(this, this::handleError);
        url = getIntent().getStringExtra(Config.KEY_URL);
        source = SourceManager.getSourceByDomain(Utils.getDomainName(url));
        fetchNovelDetails();
        fetchChapters();
        initializeLauncher();

        binding.btnYesSaveNovel.setOnClickListener(v -> handleSaveNovelClicked());
    }

    private void handleError(Exception e) {
        Snackbar.make(binding.getRoot(), e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "error occurred", BaseTransientBottomBar.LENGTH_LONG).show();
    }

    private void initializeLauncher() {
        documentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Uri baseDocumentTreeUri = Objects.requireNonNull(result.getData()).getData();
                final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                getContentResolver().takePersistableUriPermission(baseDocumentTreeUri, takeFlags);
                exportNovelTo(baseDocumentTreeUri);
            } else {
                Log.e("FileUtility", "Some Error Occurred : " + result);
            }
        });
    }

    private void handleSaveNovelClicked() {
        selectFile();
    }

    private void selectFile() {
        SetFileLocationSheet locationSheet = new SetFileLocationSheet(() -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            documentLauncher.launch(intent);
        });
        locationSheet.show(getSupportFragmentManager(), SetFileLocationSheet.TAG);
    }

    private void fetchNovelDetails() {
        viewModel.getNovel(source, new Novel(url)).observe(this, this::setUpUi);
    }

    private void fetchChapters() {
        viewModel.getChapters(source, new Novel(url)).observe(this, chapters ->
                binding.txtNoOfChapters.setText(String.format(
                        Locale.getDefault(),
                        "%d chapters will be downloaded",
                        chapters.size()
                )));
    }

    private void setUpUi(Novel novel) {
        this.novel = novel;
        Glide.with(this).load(novel.cover).into(binding.novelCover);
        Glide.with(this).load(novel.cover).into(binding.novelCoverHeader);
        binding.novelName.setText(novel.name);
        binding.rating.setRating(novel.rating);
        binding.summary.setText(novel.summary);
        binding.status.setText(novel.status);
        if (novel.authors != null) {
            binding.authors.setText(String.join(", ", novel.authors));
        }
        binding.progress.hide();
        binding.saveNovelLayout.setVisibility(View.VISIBLE);
    }

    private void exportNovelTo(Uri fileUri) {
        EpubExporter exporter = new EpubExporter(this, fileUri);
        exporter.listenToDownloadedChapters().observe(this, chapter -> {
            binding.tvChapterName.setText(chapter.name);
        });
        exporter.listenToProgress().observe(this, progress -> {
            binding.downloadProgress.setProgress(progress);
            binding.downloadPercentage.setText(String.format(Locale.getDefault(), "%s%%", progress));
        });
        exporter.listenToComplete().observe(this, isDone -> {
            if (isDone) {
                Snackbar.make(binding.getRoot(), "novel written successfully", BaseTransientBottomBar.LENGTH_LONG).show();
            }
        });
        exporter.writeNovel(novel);
        binding.downloadNovelLayout.setVisibility(View.VISIBLE);
    }
}