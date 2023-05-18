package org.ranobe.ranobe.database.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.ranobe.ranobe.models.Chapter;

import java.util.List;

@Dao
public interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(Chapter chapter);

    @Query("SELECT * FROM chapter WHERE novelUrl=:novelUrl ORDER BY id ASC")
    LiveData<List<Chapter>> list(String novelUrl);

    @Query("DELETE FROM chapter WHERE url=:chapterUrl")
    int delete(String chapterUrl);

    @Query("SELECT * FROM chapter WHERE url=:chapterUrl")
    LiveData<Chapter> get(String chapterUrl);
}
