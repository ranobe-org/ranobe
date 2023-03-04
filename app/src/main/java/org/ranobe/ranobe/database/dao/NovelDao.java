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

    @Query("SELECT * FROM novel")
    LiveData<List<Novel>> list();
}
