package org.ranobe.ranobe.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.ranobe.ranobe.models.ReadHistory;

import java.util.List;
@Dao
public interface ReadHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(ReadHistory chapter);

    @Query("SELECT * FROM readhistory WHERE novelUrl=:novelUrl ORDER BY timestamp DESC")
    LiveData<List<ReadHistory>> listByUrl(String novelUrl);

    @Query("SELECT * FROM readhistory WHERE url=:chapterUrl")
    ReadHistory get(String chapterUrl);

    @Query("SELECT * FROM readhistory where novelUrl=:novelUrl ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReadHistory> getLastReadNovel(String novelUrl);

    @Query("SELECT * FROM readhistory WHERE timestamp IN (SELECT MAX(timestamp) FROM ReadHistory GROUP BY novelUrl) ORDER BY timestamp DESC")
    LiveData<List<ReadHistory>> getLatestReadPerNovel();

    @Query("DELETE FROM readhistory WHERE url=:chapterUrl")
    int deleteHistory(String chapterUrl);

    @Query("DELETE FROM readhistory WHERE novelUrl=:novelUrl")
    int deleteHistoryByNovel(String novelUrl);

    @Query("UPDATE readhistory SET position = :position, readerOffset=:offset WHERE url = :chapterUrl")
    int updateReadHistoryPosition(int position,int offset, String chapterUrl);
}
