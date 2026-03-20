package org.ranobe.ranobe.database.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.ranobe.ranobe.models.NovelMetadata;

import java.util.List;

@Dao
public interface NovelMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(NovelMetadata novel);

    @Query("SELECT * FROM novelmetadata ORDER BY name ASC")
    LiveData<List<NovelMetadata>> list();

    @Query("DELETE FROM novelmetadata WHERE url=:novelUrl")
    int delete(String novelUrl);

    @Query("SELECT * FROM novelmetadata WHERE id=:id")
    LiveData<NovelMetadata> get(long id);

    @Query("SELECT * FROM novelmetadata WHERE url=:novelUrl")
    NovelMetadata get(String novelUrl);

    @Query("SELECT * FROM novelmetadata WHERE url=:novelUrl")
    LiveData<NovelMetadata> getByUrl(String novelUrl);
}
