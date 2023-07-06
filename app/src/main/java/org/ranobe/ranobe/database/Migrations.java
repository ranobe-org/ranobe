package org.ranobe.ranobe.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migrations {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingList` (`chapterUrl` TEXT NOT NULL, `novelUrl` TEXT, `read` INTEGER NOT NULL, `created` INTEGER, PRIMARY KEY(`chapterUrl`))");
        }
    };
}
