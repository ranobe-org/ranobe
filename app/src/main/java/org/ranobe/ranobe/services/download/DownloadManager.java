package org.ranobe.ranobe.services.download;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.List;

public class DownloadManager {
    private final DownloadStatusListener listener;
    private final String novelUrl;
    private final int sourceId;

    public DownloadManager(int sourceId, String novelUrl, DownloadStatusListener listener) {
        this.listener = listener;
        this.novelUrl = novelUrl;
        this.sourceId = sourceId;
    }

    public void start() {
        new Repository(sourceId).chapters(novelUrl, new Repository.Callback<List<ChapterItem>>() {
            @Override
            public void onComplete(List<ChapterItem> result) {
                downloadChapters(result);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                listener.error(e.getLocalizedMessage());
            }
        });
    }

    private void downloadChapters(List<ChapterItem> items) {
        int max = items.size();
        int current = 0;
        Repository repository = new Repository(sourceId);

        for (int i = 0; i < items.size(); i++) {
            ChapterItem item = items.get(i);
            current += 1;

            int finalCurrent = current;
            repository.chapter(novelUrl, item.url, new Repository.Callback<Chapter>() {
                @Override
                public void onComplete(Chapter chapter) {
                    chapter.name = item.name;
                    chapter.updated = item.updated;
                    chapter.id = item.id;
                    RanobeDatabase.databaseExecutor.execute(() -> {
                        RanobeDatabase.database().chapters().save(chapter);
                        listener.complete(finalCurrent, max);
                    });
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    listener.error(e.getLocalizedMessage());
                }
            });
        }
    }

    public interface DownloadStatusListener {
        void complete(int current, int max);

        void error(String error);
    }
}
