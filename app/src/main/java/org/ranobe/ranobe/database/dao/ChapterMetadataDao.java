package org.ranobe.ranobe.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


import org.ranobe.ranobe.models.ChapterMetadata;

import java.util.List;

@Dao
public interface ChapterMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(ChapterMetadata chapter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveAll(List<ChapterMetadata> chapters);

    @Query("SELECT * FROM chaptermetadata WHERE novelUrl=:novelUrl ORDER BY id ASC")
    LiveData<List<ChapterMetadata>> list(String novelUrl);

    @Query("SELECT * FROM chaptermetadata WHERE novelUrl=:novelUrl ORDER BY id ASC")
    List<ChapterMetadata> listByUrl(String novelUrl);

    @Query("DELETE FROM chaptermetadata WHERE url=:chapterUrl")
    int delete(String chapterUrl);

    @Query("DELETE FROM chaptermetadata WHERE novelUrl=:novelUrl")
    int deleteByNovel(String novelUrl);

    @Query("SELECT * FROM chaptermetadata WHERE url=:chapterUrl")
    LiveData<ChapterMetadata> get(String chapterUrl);
}
