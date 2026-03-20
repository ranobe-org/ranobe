package org.ranobe.ranobe.ui.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.database.mapper.ChapterMapper;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.NovelMetadata;
import org.ranobe.ranobe.models.ReadHistory;

import java.util.List;

public class HistoryViewModel extends ViewModel {


    public LiveData<List<ReadHistory>> getReadHistories() {
        return RanobeDatabase.database().readHistory().getLatestReadPerNovel();
    }

    public LiveData<List<ReadHistory>> getReadHistoriesByNovel(String novelUrl){
        return RanobeDatabase.database().readHistory().listByUrl(novelUrl);
    }

    public LiveData<ReadHistory> getLastReadByNovel(String novelUrl){
        return RanobeDatabase.database().readHistory().getLastReadNovel(novelUrl);
    }

    public void updateReadHistoryPosition(int position,int offset,String chapterUrl){
        RanobeDatabase.databaseExecutor.execute(()-> RanobeDatabase.database().readHistory().updateReadHistoryPosition(position,offset,chapterUrl));
    }

    public void deleteNovelReadHistory(String novelUrl){
        RanobeDatabase.databaseExecutor.execute(() -> RanobeDatabase.database().readHistory().deleteHistoryByNovel(novelUrl));
    }

    public void markAsRead(Chapter chapter){
        RanobeDatabase.databaseExecutor.execute(() -> {
            NovelMetadata novel = RanobeDatabase.database().novelMetadata().get(chapter.novelUrl);
            if(novel==null) return;
            ReadHistory history = ChapterMapper.ToReadHistory(chapter);
            history.cover = novel.cover;
            history.novelName = novel.name;
            history.sourceId = novel.sourceId;

            RanobeDatabase.database().readHistory().save(history);
        });
    }

    public void markAsUnread(Chapter chapter){
        RanobeDatabase.databaseExecutor.execute(() -> {
            ReadHistory history = RanobeDatabase.database().readHistory().get(chapter.url);
            if(history==null) return;
            RanobeDatabase.database().readHistory().deleteHistory(chapter.url);
        });
    }


}
