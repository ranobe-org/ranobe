package org.ranobe.ranobe.database.dao;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.ranobe.ranobe.models.Novel;

import java.util.List;

@Dao
public interface NovelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(Novel novel);

    @Query("SELECT * FROM novel ORDER BY name ASC")
    LiveData<List<Novel>> list();

    @Query("DELETE FROM novel WHERE url=:novelUrl")
    int delete(String novelUrl);

    @Query("SELECT * FROM novel WHERE id=:id")
    LiveData<Novel> get(long id);

    @Query("SELECT * FROM novel WHERE url=:novelUrl")
    LiveData<Novel> getByUrl(String novelUrl);
}
