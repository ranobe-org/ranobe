package org.ranobe.ranobe.ui.details;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.config.RanobeSettings;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.databinding.FragmentDetailsBinding;
import org.ranobe.ranobe.export.ExportJob;
import org.ranobe.ranobe.export.ExportQueue;
import org.ranobe.ranobe.export.ExportText;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.browse.adapter.NovelAdapter;
import org.ranobe.ranobe.ui.chapters.Chapters;
import org.ranobe.ranobe.ui.details.viewmodel.DetailsViewModel;
import org.ranobe.ranobe.ui.downloads.DownloadsActivity;
import org.ranobe.ranobe.ui.downloads.EpubExportPrompt;
import org.ranobe.ranobe.ui.error.Error;
import org.ranobe.ranobe.ui.history.viewmodel.HistoryViewModel;
import org.ranobe.ranobe.ui.search.viewmodel.SearchViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Details extends Fragment {
    private static final int SUMMARY_LINES = 6;
    private final List<Novel> recommendations = new ArrayList<>();
    private FragmentDetailsBinding binding;
    private DetailsViewModel viewModel;
    private HistoryViewModel historyViewModel;
    private EpubExportPrompt exportPrompt;
    private ExportJob exportJob;
    private Novel novel;
    private boolean inLibrary = false;
    private boolean summaryExpanded = false;
    private String shownCover;

    public Details() {
        // Required
    }

    // some sources shout their tags, "WEAKTOSTRONG" reads better as "Weaktostrong"
    private static String displayGenre(String genre) {
        if (genre.length() < 2 || !genre.equals(genre.toUpperCase(Locale.ROOT))) return genre;
        return genre.charAt(0) + genre.substring(1).toLowerCase(Locale.ROOT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            novel = getArguments().getParcelable(Ranobe.KEY_NOVEL);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
        historyViewModel = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
        exportPrompt = new EpubExportPrompt(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        setUpListeners();
        showPreview();
        setUpObservers();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // the chapter list / reader go through the current source; re-pin it when coming back
        // to this page after another novel's details were opened on top of it
        if (novel != null && novel.sourceId > 0)
            RanobeSettings.get().setCurrentSource(novel.sourceId).save();
    }

    // browse / search / library already hand over the title and cover (usually in Glide's
    // memory cache), so show those while the full details load
    private void showPreview() {
        if (novel == null) return;
        if (novel.name != null) binding.novelName.setText(novel.name);
        if (novel.cover != null) loadCover(novel.cover);
    }

    private void loadCover(String cover) {
        shownCover = cover;
        Glide.with(binding.novelCover.getContext()).load(cover).centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade()).into(binding.novelCover);
        Glide.with(binding.novelCover.getContext()).load(cover).centerCrop().into(binding.novelCoverHeader);
    }

    private void setUpListeners() {
        binding.readChapter.setOnClickListener(v -> navigateToChapterList());
        binding.addToLib.setOnClickListener(v -> toggleLibrary());
        binding.share.setOnClickListener(v -> shareLink());
        binding.chaptersCard.setOnClickListener(v -> navigateToChapterList());
        binding.exportCard.setOnClickListener(v -> onExportClick());
        binding.back.setOnClickListener(v -> Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main).navigateUp());
        binding.summaryToggle.setOnClickListener(v -> {
            summaryExpanded = !summaryExpanded;
            binding.summary.setMaxLines(summaryExpanded ? Integer.MAX_VALUE : SUMMARY_LINES);
            binding.summaryToggle.setText(summaryExpanded ? R.string.show_less : R.string.show_more);
        });
        binding.progress.show();

        binding.recommendations.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.recommendations.setAdapter(new NovelAdapter(recommendations, this::openRecommendation));

        // backdrop fades into the page background so the header blends in
        int surface = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSurface);
        binding.headerScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ColorUtils.setAlphaComponent(surface, 0x66), surface}));
    }

    private void shareLink() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TITLE, String.format(Locale.getDefault(), "Read %s light novel for free on Ranobe %s or visit %s", novel.name, Ranobe.RANOBE_GITHUB_LINK, novel.url));
        sendIntent.putExtra(Intent.EXTRA_TEXT, novel.url);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void setUpObservers() {
        viewModel.getError().observe(getViewLifecycleOwner(), this::setUpError);
        viewModel.getDetails(novel).observe(getViewLifecycleOwner(), this::setUpUi);
        historyViewModel.getLastReadByNovel(novel.url).observe(getViewLifecycleOwner(), readHistory -> {
            binding.readChapter.setText(readHistory != null ? R.string.continue_reading : R.string.start_reading);
            boolean named = readHistory != null && readHistory.name != null && !readHistory.name.trim().isEmpty();
            binding.chaptersStatus.setText(named
                    ? getString(R.string.last_read_chapter, readHistory.name.trim())
                    : getString(R.string.all_chapters_desc));
        });
        ExportQueue.get(requireContext()).jobs().observe(getViewLifecycleOwner(), jobs -> showExport());
        RanobeDatabase.database().novels().getByUrl(novel.url).observe(getViewLifecycleOwner(), saved -> {
            inLibrary = saved != null;
            binding.addToLib.setIconResource(inLibrary ? R.drawable.ic_favorite : R.drawable.ic_add_to_lib);
            binding.addToLib.setContentDescription(getString(inLibrary ? R.string.remove_from_library : R.string.add_to_library));
        });
    }

    // an existing export is managed on the downloads screen, otherwise start one
    private void onExportClick() {
        if (novel == null) return;
        if (exportJob != null) {
            startActivity(new Intent(requireContext(), DownloadsActivity.class));
        } else {
            exportPrompt.show(novel);
        }
    }

    private void showExport() {
        if (binding == null || novel == null) return;
        exportJob = ExportQueue.get(requireContext()).findForNovel(novel.url);
        if (exportJob == null) {
            binding.exportStatus.setText(R.string.export_epub_desc);
            binding.exportProgress.setVisibility(View.GONE);
            return;
        }
        binding.exportStatus.setText(ExportText.status(requireContext(), exportJob));
        boolean showProgress = exportJob.status != ExportJob.Status.COMPLETED;
        boolean indeterminate = ExportText.indeterminate(exportJob);
        if (binding.exportProgress.isIndeterminate() != indeterminate) {
            // switching modes only works while hidden
            binding.exportProgress.setVisibility(View.INVISIBLE);
            binding.exportProgress.setIndeterminate(indeterminate);
        }
        binding.exportProgress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (showProgress && !indeterminate)
            binding.exportProgress.setProgressCompat(exportJob.percent(), true);
    }

    private void setUpError(String error) {
        binding.progress.hide();
        Error.navigateToErrorFragment(requireActivity(), error);
    }

    private void navigateToChapterList() {
        if (novel == null) return;
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, novel);
        Chapters chapters = new Chapters();
        chapters.setArguments(bundle);
        chapters.show(getParentFragmentManager(), "chapters-sheet");
    }

    private void setUpUi(Novel novel) {
        this.novel = novel;
        // skip reloading (and re-fading) the cover the preview already shows
        if (novel.cover != null && !novel.cover.equals(shownCover)) loadCover(novel.cover);
        binding.novelName.setText(novel.name);
        binding.summary.setText(novel.summary);
        addChips(novel.genres);

        boolean hasStatus = novel.status != null && !novel.status.trim().isEmpty();
        binding.status.setText(hasStatus ? novel.status.trim() : null);
        binding.status.setVisibility(hasStatus ? View.VISIBLE : View.GONE);

        boolean hasAuthors = novel.authors != null && !novel.authors.isEmpty();
        binding.authors.setText(hasAuthors ? String.join(", ", novel.authors) : null);
        binding.authors.setVisibility(hasAuthors ? View.VISIBLE : View.GONE);

        binding.rating.setText(metaLine(novel));

        // only offer "Show more" when the synopsis is actually cut off
        binding.summary.post(() -> {
            if (binding == null) return;
            Layout layout = binding.summary.getLayout();
            boolean cut = layout != null && layout.getLineCount() > 0
                    && layout.getEllipsisCount(layout.getLineCount() - 1) > 0;
            binding.summaryToggle.setVisibility(cut || summaryExpanded ? View.VISIBLE : View.GONE);
        });

        showRecommendations(novel.recommendations);

        binding.progress.hide();
        binding.progress.setVisibility(View.GONE);
    }

    private void showRecommendations(List<Novel> items) {
        recommendations.clear();
        if (items != null) recommendations.addAll(items);
        binding.recommendationsSection.setVisibility(recommendations.isEmpty() ? View.GONE : View.VISIBLE);
        RecyclerView.Adapter<?> adapter = binding.recommendations.getAdapter();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // another details page on top of this one, back returns here
    private void openRecommendation(Novel item) {
        if (item.sourceId > 0) RanobeSettings.get().setCurrentSource(item.sourceId).save();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Ranobe.KEY_NOVEL, item);
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                .navigate(R.id.details_fragment, bundle);
    }

    // "★ 4.3 · 2012 · Source": only the parts this source provides
    private String metaLine(Novel novel) {
        List<String> parts = new ArrayList<>();
        if (novel.rating > 0) parts.add(String.format(Locale.getDefault(), "★ %.1f", novel.rating));
        if (novel.year > 0) parts.add(String.valueOf(novel.year));
        Source source = SourceManager.getSource(novel.sourceId);
        if (source != null) parts.add(source.metadata().name);
        return String.join(" · ", parts);
    }

    private void addChips(List<String> genres) {
        binding.genresLayout.removeAllViews();
        if (genres == null) return;

        LayoutInflater inflater = LayoutInflater.from(binding.genresLayout.getContext());
        for (String genre : genres) {
            if (genre.trim().isEmpty()) continue;
            Chip chip = (Chip) inflater.inflate(R.layout.item_genre_chip, binding.genresLayout, false);
            chip.setText(displayGenre(genre.trim()));
            chip.setOnClickListener(v -> searchGenre(genre));
            binding.genresLayout.addView(chip);
        }
    }

    // the source's genre page when it has one, otherwise a search for the genre's name
    private void searchGenre(String genre) {
        if (novel == null) return;
        String link = novel.genreLinks == null ? null : novel.genreLinks.get(genre);
        SearchViewModel search = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        search.searchGenre(displayGenre(genre.trim()), link, novel.sourceId);
        BottomNavigationView navbar = requireActivity().findViewById(R.id.navbar);
        if (navbar != null) navbar.setSelectedItemId(R.id.search_fragment);
    }

    private void toggleLibrary() {
        if (novel == null) return;
        Novel target = novel;
        if (inLibrary) {
            RanobeDatabase.databaseExecutor.execute(() -> RanobeDatabase.database().novels().delete(target.url));
            Snackbar.make(binding.getRoot(), R.string.removed_from_library, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, v -> RanobeDatabase.databaseExecutor.execute(() -> RanobeDatabase.database().novels().save(target)))
                    .show();
        } else {
            RanobeDatabase.databaseExecutor.execute(() -> RanobeDatabase.database().novels().save(target));
            Snackbar.make(binding.getRoot(), R.string.added_to_library, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        shownCover = null;
    }
}
