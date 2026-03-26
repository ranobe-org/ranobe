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

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Create NovelMetadata table
            database.execSQL("CREATE TABLE IF NOT EXISTS `NovelMetadata` (`id` INTEGER NOT NULL, `sourceId` INTEGER NOT NULL, `name` TEXT, `cover` TEXT, `url` TEXT, `status` TEXT, `summary` TEXT, `alternateNames` TEXT, `authors` TEXT, `genres` TEXT, `rating` REAL NOT NULL, `year` INTEGER NOT NULL, `cachedDate` INTEGER NOT NULL, PRIMARY KEY(`id`))");

            // 2. Create Indices for NovelMetadata
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_NovelMetadata_name` ON `NovelMetadata` (`name`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_NovelMetadata_sourceId` ON `NovelMetadata` (`sourceId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_NovelMetadata_name_sourceId` ON `NovelMetadata` (`name`, `sourceId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_NovelMetadata_url` ON `NovelMetadata` (`url`)");

            // 3. Create ChapterMetadata table
            database.execSQL("CREATE TABLE IF NOT EXISTS `ChapterMetadata` (`url` TEXT NOT NULL, `novelUrl` TEXT, `content` TEXT, `name` TEXT, `updated` TEXT, `id` REAL NOT NULL, `cachedDate` INTEGER NOT NULL, PRIMARY KEY(`url`))");

            // 4. Create Indices for ChapterMetadata
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ChapterMetadata_novelUrl` ON `ChapterMetadata` (`novelUrl`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ChapterMetadata_novelUrl_id` ON `ChapterMetadata` (`novelUrl`, `id`)");

            // 5. Create ReadHistory table
            database.execSQL("CREATE TABLE IF NOT EXISTS `ReadHistory` (`url` TEXT NOT NULL, `novelUrl` TEXT, `content` TEXT, `name` TEXT, `updated` TEXT, `id` REAL NOT NULL, `position` INTEGER NOT NULL, `readerOffset` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `cover` TEXT, `novelName` TEXT, `sourceId` INTEGER NOT NULL, PRIMARY KEY(`url`))");

            // 6. Create Indices for ReadHistory
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ReadHistory_novelUrl` ON `ReadHistory` (`novelUrl`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ReadHistory_novelUrl_timestamp` ON `ReadHistory` (`novelUrl` ASC, `timestamp` DESC)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ReadHistory_url` ON `ReadHistory` (`url`)");
        }
    };
}
