package org.ranobe.ranobe.database.mapper;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterMetadata;
import org.ranobe.ranobe.models.ReadHistory;

import java.util.ArrayList;
import java.util.List;

public class ChapterMapper {

    public static Chapter ToChapter(ChapterMetadata metadata) {
        if (metadata == null) return null;

        Chapter chapter = new Chapter(metadata.novelUrl);
        chapter.url = metadata.url;
        chapter.novelUrl = metadata.novelUrl;
        chapter.content = metadata.content;
        chapter.name = metadata.name;
        chapter.updated = metadata.updated;
        chapter.id = metadata.id;

        return chapter;
    }

    public static ChapterMetadata ToChapterMetadata(Chapter chapter) {
        if (chapter == null) return null;

        ChapterMetadata metadata = new ChapterMetadata(chapter.novelUrl);
        metadata.url = chapter.url;
        metadata.novelUrl = chapter.novelUrl;
        metadata.content = chapter.content;
        metadata.name = chapter.name;
        metadata.updated = chapter.updated;
        metadata.id = chapter.id;

        return metadata;
    }

    // List mappings
    public static List<Chapter> ToChapterList(List<ChapterMetadata> metadataList) {
        List<Chapter> chapters = new ArrayList<>();
        if (metadataList != null) {
            for (ChapterMetadata metadata : metadataList) {
                chapters.add(ToChapter(metadata));
            }
        }
        return chapters;
    }

    public static List<ChapterMetadata> ToChapterMetadataList(List<Chapter> chapterList) {
        List<ChapterMetadata> metadataList = new ArrayList<>();
        if (chapterList != null) {
            for (Chapter chapter : chapterList) {
                metadataList.add(ToChapterMetadata(chapter));
            }
        }
        return metadataList;
    }

    // for read history
    public static ReadHistory ToReadHistory(Chapter chapter) {
        if (chapter == null) return null;

        ReadHistory history = new ReadHistory(chapter.novelUrl);
        history.url = chapter.url;
        history.novelUrl = chapter.novelUrl;
        history.content = chapter.content;
        history.name = chapter.name;
        history.updated = chapter.updated;
        history.id = chapter.id;
        history.timestamp = System.currentTimeMillis();

        return history;
    }

    public static Chapter ToChapter(ReadHistory history) {
        if (history == null) return null;

        Chapter chapter = new Chapter(history.novelUrl);
        chapter.url = history.url;
        chapter.novelUrl = history.novelUrl;
        chapter.content = history.content;
        chapter.name = history.name;
        chapter.updated = history.updated;
        chapter.id = history.id;

        return chapter;
    }

}
