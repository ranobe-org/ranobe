package org.ranobe.ranobe.ui.search.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Search, one source at a time. Every source tab keeps its own results and paging, so switching
 * tabs back and forth never refetches, and a tab only searches once it is opened.
 * <p>
 * The query and the open tab survive process death through the {@link SavedStateHandle}, the
 * results are fetched again when the tab is shown. Repository callbacks arrive on worker threads,
 * all state is only touched on the main thread.
 */
public class SearchViewModel extends ViewModel {
    private static final String KEY_QUERY = "query";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_GENRE_URL = "genre_url";
    private static final String KEY_GENRE_SOURCE = "genre_source";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final SavedStateHandle saved;
    private final MutableLiveData<String> query;
    private final Map<Integer, Paging> pagings = new HashMap<>();

    public SearchViewModel(SavedStateHandle saved) {
        this.saved = saved;
        this.query = saved.getLiveData(KEY_QUERY, null);
    }

    public LiveData<String> getQuery() {
        return query;
    }

    /**
     * The source whose genre page the query is, -1 for a plain text search.
     */
    public int genreSource() {
        Integer id = saved.get(KEY_GENRE_SOURCE);
        return id == null ? -1 : id;
    }

    /**
     * Novels of a genre: the source the link belongs to lists its genre page, other tabs search
     * for the genre's name.
     */
    @MainThread
    public void searchGenre(String name, @Nullable String genreUrl, int sourceId) {
        if (name == null || name.trim().isEmpty()) return;
        saved.set(KEY_GENRE_URL, genreUrl);
        saved.set(KEY_GENRE_SOURCE, genreUrl == null ? -1 : sourceId);
        query.setValue(name.trim());
        select(sourceId);
    }

    /**
     * Keeps the text but searches it as words again.
     */
    @MainThread
    public void clearGenre(int sourceId) {
        saved.set(KEY_GENRE_URL, null);
        saved.set(KEY_GENRE_SOURCE, -1);
        select(sourceId);
    }

    @Nullable
    public String currentQuery() {
        return query.getValue();
    }

    /**
     * The source whose tab was open last, -1 before any was.
     */
    public int selectedSource() {
        Integer id = saved.get(KEY_SOURCE);
        return id == null ? -1 : id;
    }

    @MainThread
    public LiveData<State> state(int sourceId) {
        return paging(sourceId).live;
    }

    /**
     * A new query from the search field, run on the open tab. The other tabs pick it up when
     * they are opened.
     */
    @MainThread
    public void submit(String keyword, int sourceId) {
        String next = keyword == null ? null : keyword.trim();
        if (next == null || next.isEmpty()) return;
        // typed text is always a plain search
        saved.set(KEY_GENRE_URL, null);
        saved.set(KEY_GENRE_SOURCE, -1);
        query.setValue(next);
        select(sourceId);
    }

    /**
     * The tab of this source is now showing, searches it for the current query if it hasn't been.
     */
    @MainThread
    public void select(int sourceId) {
        saved.set(KEY_SOURCE, sourceId);
        String current = query.getValue();
        if (current == null) return;
        Paging paging = paging(sourceId);
        State state = paging.state;
        String key = keyFor(sourceId, current);
        if (key.equals(paging.key)) {
            // same query (e.g. back from details): keep what is there, but don't leave a failed
            // first page sitting empty
            if (state.items.isEmpty() && state.error != null && !state.loading) load(sourceId, paging);
            return;
        }
        paging.reset(current, key, genreUrlFor(sourceId));
        load(sourceId, paging);
    }

    // what a tab's results are for: a genre page on the genre's own source, the text elsewhere
    private String keyFor(int sourceId, String text) {
        String genre = genreUrlFor(sourceId);
        return genre != null ? "genre:" + genre : "text:" + text;
    }

    @Nullable
    private String genreUrlFor(int sourceId) {
        return genreSource() == sourceId ? saved.get(KEY_GENRE_URL) : null;
    }

    /**
     * Fetches the next page, ignored while one is loading or once the source has no more.
     */
    @MainThread
    public void loadMore(int sourceId) {
        Paging paging = paging(sourceId);
        State state = paging.state;
        if (state.query == null || state.loading || state.endReached || state.error != null) return;
        load(sourceId, paging);
    }

    /**
     * Tries the page that failed again.
     */
    @MainThread
    public void retry(int sourceId) {
        Paging paging = paging(sourceId);
        if (paging.state.query == null || paging.state.loading) return;
        load(sourceId, paging);
    }

    private Paging paging(int sourceId) {
        Paging paging = pagings.get(sourceId);
        if (paging == null) {
            paging = new Paging();
            pagings.put(sourceId, paging);
        }
        return paging;
    }

    private void load(int sourceId, Paging paging) {
        State state = paging.state;
        int generation = paging.generation;
        int nextPage = state.page + 1;
        String keyword = state.query;
        paging.publish(state.copy().loading(true).error(null).build());

        Filter filter = new Filter();
        if (paging.genreUrl != null) filter.addFilter(Filter.FILTER_GENRE, paging.genreUrl);
        else filter.addFilter(Filter.FILTER_KEYWORD, keyword);
        new Repository(sourceId).search(filter, nextPage, new Repository.Callback<List<Novel>>() {
            @Override
            public void onComplete(List<Novel> result) {
                main.post(() -> onPage(paging, generation, nextPage, result));
            }

            @Override
            public void onError(Exception e) {
                String message = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getSimpleName();
                main.post(() -> onFailed(paging, generation, message));
            }
        });
    }

    private void onPage(Paging paging, int generation, int page, List<Novel> result) {
        // a newer search replaced the one that asked for this page
        if (generation != paging.generation) return;
        List<Novel> items = new ArrayList<>(paging.state.items);
        int added = 0;
        if (result != null) {
            for (Novel novel : result) {
                if (novel != null && novel.url != null && paging.seenUrls.add(novel.url)) {
                    items.add(novel);
                    added++;
                }
            }
        }
        // an empty page, or one that only repeats earlier results, means the source has no more
        paging.publish(paging.state.copy().items(items).page(page).loading(false).endReached(added == 0).build());
    }

    private void onFailed(Paging paging, int generation, String message) {
        if (generation != paging.generation) return;
        paging.publish(paging.state.copy().loading(false).error(message).build());
    }

    private static final class Paging {
        final MutableLiveData<State> live = new MutableLiveData<>(State.IDLE);
        final Set<String> seenUrls = new HashSet<>();
        State state = State.IDLE;
        // bumped by every new query so late answers for an older one are dropped
        int generation = 0;
        // what the results are for, see keyFor
        String key;
        String genreUrl;

        void reset(String query, String key, String genreUrl) {
            generation++;
            this.key = key;
            this.genreUrl = genreUrl;
            seenUrls.clear();
            publish(new State(query, Collections.emptyList(), 0, false, false, null));
        }

        void publish(State next) {
            state = next;
            live.setValue(next);
        }
    }

    /**
     * What one source tab shows. Immutable, a new one is published for every change.
     */
    public static final class State {
        static final State IDLE = new State(null, Collections.emptyList(), 0, false, false, null);

        // null until this tab was searched
        @Nullable
        public final String query;
        @NonNull
        public final List<Novel> items;
        // last page that was loaded, 0 before the first
        public final int page;
        public final boolean loading;
        public final boolean endReached;
        // why the last page failed, cleared when it is tried again
        @Nullable
        public final String error;

        State(@Nullable String query, @NonNull List<Novel> items, int page, boolean loading, boolean endReached, @Nullable String error) {
            this.query = query;
            this.items = Collections.unmodifiableList(items);
            this.page = page;
            this.loading = loading;
            this.endReached = endReached;
            this.error = error;
        }

        public boolean isIdle() {
            return query == null;
        }

        Builder copy() {
            return new Builder(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State s = (State) o;
            return page == s.page && loading == s.loading && endReached == s.endReached
                    && Objects.equals(query, s.query) && items.equals(s.items) && Objects.equals(error, s.error);
        }

        @Override
        public int hashCode() {
            return Objects.hash(query, items, page, loading, endReached, error);
        }
    }

    private static final class Builder {
        private final String query;
        private List<Novel> items;
        private int page;
        private boolean loading;
        private boolean endReached;
        private String error;

        Builder(State s) {
            query = s.query;
            items = s.items;
            page = s.page;
            loading = s.loading;
            endReached = s.endReached;
            error = s.error;
        }

        Builder items(List<Novel> items) {
            this.items = items;
            return this;
        }

        Builder page(int page) {
            this.page = page;
            return this;
        }

        Builder loading(boolean loading) {
            this.loading = loading;
            return this;
        }

        Builder endReached(boolean endReached) {
            this.endReached = endReached;
            return this;
        }

        Builder error(String error) {
            this.error = error;
            return this;
        }

        State build() {
            return new State(query, items, page, loading, endReached, error);
        }
    }
}
