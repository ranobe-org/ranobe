package org.ranobe.downloader.writer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.ranobe.core.models.Chapter;
import org.ranobe.core.models.Novel;
import org.ranobe.core.network.repository.Repository;
import org.ranobe.core.util.ListUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;

public class EpubExporter {
    private final EpubWriter epubWriter;
    private final Book book;
    private final Context context;
    private final Uri outputFileUri;
    private final MutableLiveData<Chapter> chapterDownloaded;
    private final MutableLiveData<Integer> progress;
    private final MutableLiveData<Boolean> complete;

    public EpubExporter(Context context, Uri outputFileUri) {
        this.context = context;
        this.outputFileUri = outputFileUri;
        epubWriter = new EpubWriter();
        book = new Book();
        chapterDownloaded = new MutableLiveData<>();
        progress = new MutableLiveData<>();
        complete = new MutableLiveData<>();
    }

    public MutableLiveData<Chapter> listenToDownloadedChapters() {
        return chapterDownloaded;
    }

    public MutableLiveData<Integer> listenToProgress() {
        return progress;
    }

    public MutableLiveData<Boolean> listenToComplete() {
        return complete;
    }

    public void writeNovel(Novel novel) {
        Metadata metadata = book.getMetadata();
        String description = String.join("\n", novel.summary, novel.url, "Created by ranobe.org. Find them on github @ranobe-org");

        metadata.addTitle(novel.name);
        metadata.addDescription(description);
        metadata.addType("light-novel");
        for (String author : novel.authors) {
            metadata.addAuthor(new Author(author));
        }

        Glide.with(context)
                .asFile()
                .load(novel.cover)
                .into(new CustomTarget<File>() {
                    @Override
                    public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                        try {
                            book.setCoverImage(new Resource(new FileInputStream(resource), "cover.png"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

        getChapters(novel);

    }

    private void writeChapterToBook(float chapterNumber, String chapter) {
        byte[] htmlContent = addHtmlToString(chapter).getBytes(StandardCharsets.UTF_8);
        Resource xhtmlResource = new Resource(htmlContent, MediatypeService.XHTML);
        book.addSection("Chapter " + chapterNumber, xhtmlResource);
    }

    private String addHtmlToString(String chapter) {
        String[] paras = chapter.split("\n", -1);
        String joined = String.join("</p><p>", paras);
        return "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head/><body><p>" + joined + "</p></body></html>";
    }

    private void writeEpubToFile(Novel novel) throws Exception {
        String filename = novel.name + ".epub";
        DocumentFile directory = DocumentFile.fromTreeUri(context, outputFileUri);
        DocumentFile file = directory.createFile(getMimeType(), filename);
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(file.getUri(), "w");
        FileOutputStream stream = new FileOutputStream(pfd.getFileDescriptor());
        epubWriter.write(book, stream);
        pfd.close();
        complete.postValue(true);
    }

    private String getMimeType() {
        String fallback = "*/*";
        try {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension("epub");
            return mime == null ? fallback : mime;
        } catch (Exception e) {
            e.printStackTrace();
            return fallback;
        }
    }

    private void getChapters(Novel novel) {
        new Repository(novel.sourceId).chapters(novel, new Repository.Callback<List<Chapter>>() {
            @Override
            public void onComplete(List<Chapter> result) {
                try {
                    handleChapters(novel, ListUtils.sortById(result), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private int getPercentage(int size, int index) {
        return (index * 100) / size;
    }

    private void handleChapters(Novel novel, List<Chapter> chapters, int index) throws Exception {
        if (index == chapters.size()) {
            writeEpubToFile(novel);
            return;
        }
        new Repository(novel.sourceId).chapter(chapters.get(index), new Repository.Callback<Chapter>() {
            @Override
            public void onComplete(Chapter result) {
                try {
                    writeChapterToBook(result.id, result.content);
                    chapterDownloaded.postValue(result);
                    progress.postValue(getPercentage(chapters.size(), index + 1));
                    handleChapters(novel, chapters, index + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
