package org.ranobe.ranobe.ui.downloads;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.databinding.ActivityDownloadsBinding;
import org.ranobe.ranobe.export.EpubOutput;
import org.ranobe.ranobe.export.ExportJob;
import org.ranobe.ranobe.export.ExportQueue;
import org.ranobe.ranobe.ui.views.EmptyState;

/**
 * Every EPUB export with its progress, and the controls to pause, resume, cancel or open it.
 */
public class DownloadsActivity extends AppCompatActivity implements DownloadsAdapter.Listener {
    private ActivityDownloadsBinding binding;
    private ExportQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDownloadsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        DownloadsAdapter adapter = new DownloadsAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setItemAnimator(null);
        binding.list.setAdapter(adapter);

        queue = ExportQueue.get(this);
        queue.jobs().observe(this, jobs -> {
            adapter.submit(jobs);
            if (jobs.isEmpty()) {
                EmptyState.show(binding.emptyState, R.drawable.ic_download, R.string.downloads_empty_title, R.string.downloads_empty);
            } else {
                EmptyState.hide(binding.emptyState);
            }
        });
    }

    @Override
    public void onAction(ExportJob job, DownloadsAdapter.Action action) {
        switch (action) {
            case PAUSE:
                queue.pause(job.id);
                break;
            case RESUME:
            case RETRY:
                queue.resume(job.id);
                break;
            case BUILD_ANYWAY:
                queue.buildAnyway(job.id);
                break;
            case CANCEL:
                confirmCancel(job);
                break;
            case REMOVE:
                queue.cancel(job.id);
                break;
            case OPEN:
                open(job);
                break;
            case SHARE:
                share(job);
                break;
        }
    }

    private void confirmCancel(ExportJob job) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_cancel_title)
                .setMessage(R.string.export_cancel_message)
                .setPositiveButton(R.string.export_cancel, (d, w) -> queue.cancel(job.id))
                .setNegativeButton(R.string.export_keep, null)
                .show();
    }

    private void open(ExportJob job) {
        try {
            startActivity(EpubOutput.viewIntent(Uri.parse(job.outputUri)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.export_no_reader, Snackbar.LENGTH_LONG).show();
        }
    }

    private void share(ExportJob job) {
        startActivity(Intent.createChooser(EpubOutput.shareIntent(Uri.parse(job.outputUri), job.novel.name), job.outputName));
    }
}
