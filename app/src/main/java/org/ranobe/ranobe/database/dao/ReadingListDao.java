package org.ranobe.ranobe.database.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.ranobe.ranobe.database.models.ReadingList;

import java.util.List;

@Dao
public interface ReadingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(ReadingList readingList);

    @Query("UPDATE readinglist SET read = read + 1 WHERE chapterUrl=:chapterUrl")
    void updateReadCount(String chapterUrl);

    @Query("SELECT chapterUrl FROM readinglist WHERE novelUrl=:novelUrl ORDER BY created DESC")
    LiveData<List<String>> list(String novelUrl);

    @Query("DELETE FROM readinglist WHERE chapterUrl=:chapterUrl")
    void delete(String chapterUrl);

    @Query("SELECT * FROM readinglist WHERE novelUrl=:novelUrl AND chapterUrl=:chapterUrl LIMIT 1")
    ReadingList get(String novelUrl, String chapterUrl);

    @Query("SELECT COUNT(*) FROM readinglist WHERE novelUrl=:novelUrl ORDER BY created DESC")
    LiveData<Integer> countOfReadForNovel(String novelUrl);
}
