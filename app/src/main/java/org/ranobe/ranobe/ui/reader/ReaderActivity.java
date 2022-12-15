package org.ranobe.ranobe.ui.reader;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.databinding.ActivityReaderBinding;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ReaderTheme;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.reader.sheet.CustomizeReader;
import org.ranobe.ranobe.ui.reader.viewmodel.ReaderViewModel;

public class ReaderActivity extends AppCompatActivity implements CustomizeReader.OnOptionSelection {
    private ActivityReaderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(Ranobe.getThemeMode(getApplicationContext()));
        setContentView(binding.getRoot());
        binding.customize.setOnClickListener(v-> setUpCustomizeReader());
        setUpReaderTheme();

        String chapterUrl = getIntent().getStringExtra("chapter");
        ReaderViewModel viewModel = new ViewModelProvider(this).get(ReaderViewModel.class);

        Source source = SourceManager.getSource(1);
        viewModel.getChapter().observe(this, this::setChapter);
        viewModel.getError().observe(this, this::setError);
        viewModel.chapter(source, chapterUrl);
    }

    private void setUpReaderTheme() {
        String currentReaderTheme = Ranobe.getReaderTheme(this);
        float currentFontSize = Ranobe.getReaderFont(this);
        if(currentReaderTheme != null) {
            setReaderTheme(currentReaderTheme);
        }
        if (currentFontSize != 0) {
            setFontSize(currentFontSize);
        }
    }

    private void setUpCustomizeReader() {
        CustomizeReader sheet = new CustomizeReader(this);
        sheet.show(getSupportFragmentManager(), "customize-sheet");
    }

    private void setChapter(Chapter chapter) {
        binding.content.setText(chapter.content);
        binding.progress.setVisibility(View.GONE);
    }

    private void setError(String msg) {
        if (msg.length() == 0) return;
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void setFontSize(float size) {
        binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        Ranobe.storeReaderFont(this, size);
    }

    @Override
    public void setReaderTheme(String themeName) {
        ReaderTheme theme = Ranobe.themes.get(themeName);
        if(theme != null) {
            binding.readerView.setBackgroundColor(theme.getBackground());
            binding.content.setTextColor(theme.getText());
            Ranobe.storeReaderTheme(this, themeName);
        }
    }

    @Override
    public float getFontSize() {
        float px = binding.content.getTextSize();
        return px / getResources().getDisplayMetrics().scaledDensity;
    }
}