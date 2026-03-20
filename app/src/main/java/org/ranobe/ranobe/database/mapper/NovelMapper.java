package org.ranobe.ranobe.database.mapper;

import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelMetadata;

import java.util.ArrayList;

public class NovelMapper {

    public static Novel ToNovel(NovelMetadata data) {
        if (data == null) return null;

        Novel novel = new Novel(data.url);
        novel.id = data.id;
        novel.sourceId = data.sourceId;
        novel.name = data.name;
        novel.cover = data.cover;
        novel.status = data.status;
        novel.summary = data.summary;
        novel.alternateNames = data.alternateNames != null ? new ArrayList<>(data.alternateNames) : new ArrayList<>();
        novel.authors = data.authors != null ? new ArrayList<>(data.authors) : new ArrayList<>();
        novel.genres = data.genres != null ? new ArrayList<>(data.genres) : new ArrayList<>();
        novel.rating = data.rating;
        novel.year = data.year;

        return novel;
    }

    public static NovelMetadata ToNovelMetadata(Novel novel) {
        if (novel == null) return null;

        NovelMetadata metadata = new NovelMetadata(novel.url);
        metadata.id = novel.id;
        metadata.sourceId = novel.sourceId;
        metadata.name = novel.name;
        metadata.cover = novel.cover;
        metadata.status = novel.status;
        metadata.summary = novel.summary;
        metadata.alternateNames = novel.alternateNames != null ? new ArrayList<>(novel.alternateNames) : new ArrayList<>();
        metadata.authors = novel.authors != null ? new ArrayList<>(novel.authors) : new ArrayList<>();
        metadata.genres = novel.genres != null ? new ArrayList<>(novel.genres) : new ArrayList<>();
        metadata.rating = novel.rating;
        metadata.year = novel.year;

        return metadata;
    }

}
