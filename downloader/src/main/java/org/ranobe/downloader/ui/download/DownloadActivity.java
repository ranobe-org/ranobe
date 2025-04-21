package org.ranobe.downloader.ui.download;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.core.models.Novel;
import org.ranobe.core.sources.Source;
import org.ranobe.core.sources.SourceManager;
import org.ranobe.downloader.R;
import org.ranobe.downloader.config.Config;
import org.ranobe.downloader.config.Utils;
import org.ranobe.downloader.databinding.ActivityDownloadBinding;
import org.ranobe.downloader.ui.download.sheets.SetFileLocationSheet;
import org.ranobe.downloader.ui.download.sheets.WatchVideoSheet;
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

    private RewardedAd rewardedAd;
    private AdView adView;
    private final static String TAG = "DEBUG:ATUL";

    private final FullScreenContentCallback callback = new FullScreenContentCallback() {

        @Override
        public void onAdDismissedFullScreenContent() {
            // Called when ad is dismissed.
            // Set the ad reference to null so you don't show the ad a second time.
            Log.d(TAG, "Ad dismissed fullscreen content.");
            rewardedAd = null;
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            // Called when ad fails to show.
            Log.e(TAG, "Ad failed to show fullscreen content.");
            rewardedAd = null;
        }

        @Override
        public void onAdImpression() {
            // Called when an impression is recorded for an ad.
            Log.d(TAG, "Ad recorded an impression.");
        }

    };

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

        // ads
        setBannerAd();
        loadVideoAd();

        binding.btnYesSaveNovel.setOnClickListener(v -> handleSaveNovelClicked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        if (rewardedAd != null) {
            rewardedAd = null;
        }
    }

    private void setBannerAd() {
        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.ad_banner_download_id));
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));

        binding.adViewContainer.removeAllViews();
        binding.adViewContainer.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void loadVideoAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.ad_video_id),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.toString());
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        rewardedAd.setFullScreenContentCallback(callback);
                        Log.d(TAG, "Ad was loaded.");
                    }
                });
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
        watchVideo();
    }

    private void watchVideo() {
        WatchVideoSheet videoSheet = new WatchVideoSheet(() -> {
            if (rewardedAd != null) {
                rewardedAd.show(DownloadActivity.this, rewardItem -> selectFile());
            } else {
                selectFile();
            }
        });
        videoSheet.show(getSupportFragmentManager(), WatchVideoSheet.TAG);
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