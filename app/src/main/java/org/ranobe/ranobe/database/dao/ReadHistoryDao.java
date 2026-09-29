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

    // latest row of each novel; matching timestamps per novel (not globally) so another novel's
    // older row with the same timestamp can't slip in. Chapter text isn't needed for the list.
    @Query("SELECT url, novelUrl, NULL AS content, name, updated, id, position, readerOffset, timestamp, cover, novelName, sourceId " +
            "FROM readhistory r WHERE timestamp = (SELECT MAX(timestamp) FROM readhistory WHERE novelUrl = r.novelUrl) " +
            "GROUP BY novelUrl ORDER BY timestamp DESC LIMIT 100")
    LiveData<List<ReadHistory>> getLatestReadPerNovel();

    @Query("SELECT r.* FROM readhistory r INNER JOIN novel n ON r.novelUrl = n.url ORDER BY timestamp DESC LIMIT 1")
    LiveData<ReadHistory> getLastReadHistory();

    @Query("DELETE FROM readhistory WHERE url=:chapterUrl")
    int deleteHistory(String chapterUrl);

    @Query("DELETE FROM readhistory WHERE novelUrl=:novelUrl")
    int deleteHistoryByNovel(String novelUrl);

    @Query("DELETE FROM readhistory")
    int deleteAll();

    // history used to store full chapter text, which the reader never reads back
    @Query("UPDATE readhistory SET content = NULL WHERE content IS NOT NULL")
    int clearStoredContent();

    @Query("UPDATE readhistory SET position = :position, readerOffset=:offset WHERE url = :chapterUrl")
    int updateReadHistoryPosition(int position, int offset, String chapterUrl);
}
