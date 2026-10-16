package org.ranobe.ranobe.export;

import org.ranobe.ranobe.util.SourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a novel as an EPUB 3 package that also carries an EPUB 2 NCX table of contents, so older
 * readers can open it too. Every chapter is its own XHTML document.
 * <p>
 * Chapter text uses the app's content format: paragraphs split by blank lines and images as
 * {@code [img]path[/img]}, where the path is relative to the job's work folder. Images must
 * already be JPEG, PNG or GIF.
 */
final class EpubBuilder {
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n");
    private static final String CSS = "body { margin: 0 4%; line-height: 1.5; }\n"
            + "h1, h2 { text-align: center; line-height: 1.3; }\n"
            + "h2.chapter { margin: 1.5em 0 1em; page-break-before: always; break-before: page; }\n"
            + "p { margin: 0 0 0.9em; text-indent: 0; }\n"
            + "div.image { text-align: center; margin: 1em 0; }\n"
            + "div.image img { max-width: 100%; height: auto; }\n"
            + "div.cover { text-align: center; margin: 0; padding: 0; }\n"
            + "div.cover img { max-width: 100%; max-height: 100vh; }\n"
            + "p.meta { text-align: center; font-style: italic; }\n"
            + "p.summary { margin-top: 2em; }\n";

    private EpubBuilder() {
    }

    static void write(Book book, OutputStream out, Progress progress) throws IOException {
        if (book.sections.isEmpty()) throw new IOException("No chapters to put in the book");
        String coverFormat = book.cover == null ? null : ImageInfo.format(book.cover);
        if (coverFormat != null && !ImageInfo.isEpubCore(coverFormat)) coverFormat = null;

        ZipOutputStream zip = new ZipOutputStream(out);
        // the mimetype entry must come first, uncompressed and without extra fields
        putStored(zip, "mimetype", "application/epub+zip".getBytes(StandardCharsets.US_ASCII));
        putText(zip, "META-INF/container.xml", containerXml());
        putText(zip, "OEBPS/styles.css", CSS);

        if (coverFormat != null) {
            putText(zip, "OEBPS/text/cover.xhtml", coverXhtml(book, "../images/cover." + coverFormat));
            putFile(zip, "OEBPS/images/cover." + coverFormat, book.cover);
        }
        putText(zip, "OEBPS/text/title.xhtml", titleXhtml(book));

        // images in reading order, keyed by their href inside the book
        Map<String, File> images = new LinkedHashMap<>();
        int total = book.sections.size();
        for (int s = 0; s < total; s++) {
            Section section = book.sections.get(s);
            putText(zip, "OEBPS/" + chapterHref(s), chapterXhtml(book, section, images));
            progress.onChapter(s + 1, total);
        }
        for (Map.Entry<String, File> image : images.entrySet()) {
            putFile(zip, "OEBPS/" + image.getKey(), image.getValue());
        }

        putText(zip, "OEBPS/content.opf", opf(book, coverFormat, images.keySet()));
        putText(zip, "OEBPS/nav.xhtml", nav(book, coverFormat != null));
        putText(zip, "OEBPS/toc.ncx", ncx(book));
        zip.finish();
        zip.flush();
    }

    private static String chapterHref(int section) {
        return String.format(Locale.ROOT, "text/c%05d.xhtml", section + 1);
    }

    private static String containerXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
                + "  <rootfiles>\n"
                + "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n"
                + "  </rootfiles>\n"
                + "</container>\n";
    }

    private static String opf(Book book, String coverFormat, Iterable<String> images) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));

        StringBuilder b = new StringBuilder();
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"book-id\" xml:lang=\"")
                .append(esc(book.language)).append("\">\n")
                .append("  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n")
                .append("    <dc:identifier id=\"book-id\">").append(esc(book.id)).append("</dc:identifier>\n")
                .append("    <dc:title>").append(esc(book.title)).append("</dc:title>\n")
                .append("    <dc:language>").append(esc(book.language)).append("</dc:language>\n");
        if (!isBlank(book.author))
            b.append("    <dc:creator>").append(esc(book.author)).append("</dc:creator>\n");
        if (!isBlank(book.description)) {
            b.append("    <dc:description>").append(esc(book.description)).append("</dc:description>\n");
        }
        if (!isBlank(book.source))
            b.append("    <dc:source>").append(esc(book.source)).append("</dc:source>\n");
        b.append("    <meta property=\"dcterms:modified\">").append(iso.format(new Date())).append("</meta>\n");
        // EPUB 2 readers find the cover through this
        if (coverFormat != null) b.append("    <meta name=\"cover\" content=\"img-cover\"/>\n");
        b.append("  </metadata>\n")
                .append("  <manifest>\n")
                .append("    <item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n")
                .append("    <item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n")
                .append("    <item id=\"css\" href=\"styles.css\" media-type=\"text/css\"/>\n");
        if (coverFormat != null) {
            b.append("    <item id=\"page-cover\" href=\"text/cover.xhtml\" media-type=\"application/xhtml+xml\"/>\n")
                    .append("    <item id=\"img-cover\" href=\"images/cover.").append(coverFormat).append("\" media-type=\"")
                    .append(ImageInfo.mediaType(coverFormat)).append("\" properties=\"cover-image\"/>\n");
        }
        b.append("    <item id=\"page-title\" href=\"text/title.xhtml\" media-type=\"application/xhtml+xml\"/>\n");
        for (int s = 0; s < book.sections.size(); s++) {
            b.append("    <item id=\"chapter-").append(s + 1).append("\" href=\"").append(chapterHref(s))
                    .append("\" media-type=\"application/xhtml+xml\"/>\n");
        }
        int i = 0;
        for (String href : images) {
            String format = href.substring(href.lastIndexOf('.') + 1);
            b.append("    <item id=\"img-").append(++i).append("\" href=\"").append(esc(href))
                    .append("\" media-type=\"").append(ImageInfo.mediaType(format)).append("\"/>\n");
        }
        b.append("  </manifest>\n")
                .append("  <spine toc=\"ncx\">\n");
        if (coverFormat != null) b.append("    <itemref idref=\"page-cover\"/>\n");
        b.append("    <itemref idref=\"page-title\"/>\n");
        for (int s = 0; s < book.sections.size(); s++) {
            b.append("    <itemref idref=\"chapter-").append(s + 1).append("\"/>\n");
        }
        b.append("  </spine>\n");
        if (coverFormat != null) {
            b.append("  <guide>\n")
                    .append("    <reference type=\"cover\" title=\"Cover\" href=\"text/cover.xhtml\"/>\n")
                    .append("  </guide>\n");
        }
        b.append("</package>\n");
        return b.toString();
    }

    private static String nav(Book book, boolean hasCover) {
        StringBuilder b = new StringBuilder();
        b.append(xhtmlHead(book.language, book.title, false))
                .append("<body>\n")
                .append("  <nav epub:type=\"toc\" id=\"toc\">\n    <h1>").append(esc(book.title)).append("</h1>\n    <ol>\n");
        for (int s = 0; s < book.sections.size(); s++) {
            b.append("      <li><a href=\"").append(chapterHref(s)).append("\">")
                    .append(esc(book.sections.get(s).title)).append("</a></li>\n");
        }
        b.append("    </ol>\n  </nav>\n")
                .append("  <nav epub:type=\"landmarks\" id=\"landmarks\" hidden=\"\">\n    <ol>\n");
        if (hasCover)
            b.append("      <li><a epub:type=\"cover\" href=\"text/cover.xhtml\">Cover</a></li>\n");
        b.append("      <li><a epub:type=\"titlepage\" href=\"text/title.xhtml\">Title page</a></li>\n")
                .append("      <li><a epub:type=\"bodymatter\" href=\"").append(chapterHref(0)).append("\">Start</a></li>\n")
                .append("    </ol>\n  </nav>\n</body>\n</html>\n");
        return b.toString();
    }

    private static String ncx(Book book) {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\" xml:lang=\"")
                .append(esc(book.language)).append("\">\n")
                .append("  <head>\n")
                .append("    <meta name=\"dtb:uid\" content=\"").append(esc(book.id)).append("\"/>\n")
                .append("    <meta name=\"dtb:depth\" content=\"1\"/>\n")
                .append("    <meta name=\"dtb:totalPageCount\" content=\"0\"/>\n")
                .append("    <meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n")
                .append("  </head>\n")
                .append("  <docTitle><text>").append(esc(book.title)).append("</text></docTitle>\n")
                .append("  <navMap>\n");
        for (int s = 0; s < book.sections.size(); s++) {
            int order = s + 1;
            b.append("    <navPoint id=\"nav-").append(order).append("\" playOrder=\"").append(order).append("\">\n")
                    .append("      <navLabel><text>").append(esc(book.sections.get(s).title)).append("</text></navLabel>\n")
                    .append("      <content src=\"").append(chapterHref(s)).append("\"/>\n")
                    .append("    </navPoint>\n");
        }
        b.append("  </navMap>\n</ncx>\n");
        return b.toString();
    }

    private static String coverXhtml(Book book, String imageHref) {
        return xhtmlHead(book.language, "Cover", true)
                + "<body epub:type=\"cover\">\n"
                + "  <div class=\"cover\"><img src=\"" + esc(imageHref) + "\" alt=\"" + esc(book.title) + "\"/></div>\n"
                + "</body>\n</html>\n";
    }

    private static String titleXhtml(Book book) {
        StringBuilder b = new StringBuilder();
        b.append(xhtmlHead(book.language, book.title, true))
                .append("<body epub:type=\"titlepage\">\n")
                .append("  <h1>").append(esc(book.title)).append("</h1>\n");
        if (!isBlank(book.author))
            b.append("  <p class=\"meta\">").append(esc(book.author)).append("</p>\n");
        if (!isBlank(book.description)) {
            for (String paragraph : PARAGRAPH_BREAK.split(book.description.trim())) {
                if (paragraph.trim().isEmpty()) continue;
                b.append("  <p class=\"summary\">").append(lines(paragraph.trim())).append("</p>\n");
            }
        }
        if (!isBlank(book.source))
            b.append("  <p class=\"meta\">").append(esc(book.source)).append("</p>\n");
        b.append("</body>\n</html>\n");
        return b.toString();
    }

    private static String chapterXhtml(Book book, Section section, Map<String, File> images) throws IOException {
        String content = readText(section.text);
        StringBuilder b = new StringBuilder(content.length() + 1024);
        b.append(xhtmlHead(book.language, section.title, true))
                .append("<body>\n")
                .append("  <h2 class=\"chapter\">").append(esc(section.title)).append("</h2>\n");

        Matcher matcher = SourceUtils.IMAGE_TAG.matcher(content);
        int last = 0;
        while (matcher.find()) {
            paragraphs(b, content.substring(last, matcher.start()));
            image(b, book, matcher.group(1).trim(), images);
            last = matcher.end();
        }
        paragraphs(b, content.substring(last));
        b.append("</body>\n</html>\n");
        return b.toString();
    }

    private static void paragraphs(StringBuilder b, String text) {
        for (String paragraph : PARAGRAPH_BREAK.split(text)) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) b.append("  <p>").append(lines(trimmed)).append("</p>\n");
        }
    }

    // single line breaks inside a paragraph are kept, some sources only use those
    private static String lines(String paragraph) {
        StringBuilder b = new StringBuilder();
        String[] lines = paragraph.split("\\s*\\n\\s*");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) b.append("<br/>");
            b.append(esc(lines[i]));
        }
        return b.toString();
    }

    // images the runner couldn't fetch stay as bare urls, those are dropped from the book
    private static void image(StringBuilder b, Book book, String path, Map<String, File> images) {
        if (!path.startsWith("images/")) return;
        File file = new File(book.baseDir, path);
        if (!file.isFile()) return;
        images.put(path, file);
        b.append("  <div class=\"image\"><img src=\"../").append(esc(path)).append("\" alt=\"\"/></div>\n");
    }

    private static String xhtmlHead(String language, String title, boolean css) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE html>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" xml:lang=\""
                + esc(language) + "\" lang=\"" + esc(language) + "\">\n"
                + "<head>\n  <meta charset=\"UTF-8\"/>\n"
                + "  <title>" + esc(title) + "</title>\n"
                + (css ? "  <link rel=\"stylesheet\" type=\"text/css\" href=\"../styles.css\"/>\n" : "")
                + "</head>\n";
    }

    private static String readText(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        int read = 0;
        try (InputStream in = new FileInputStream(file)) {
            while (read < data.length) {
                int n = in.read(data, read, data.length - read);
                if (n < 0) break;
                read += n;
            }
        }
        return new String(data, 0, read, StandardCharsets.UTF_8);
    }

    private static void putStored(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.DEFLATED);
        zip.putNextEntry(entry);
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // images are already compressed, storing them saves time on big books
    private static void putFile(ZipOutputStream zip, String name, File file) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        CRC32 crc = new CRC32();
        try (InputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) crc.update(buffer, 0, read);
        }
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(file.length());
        entry.setCompressedSize(file.length());
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) zip.write(buffer, 0, read);
        }
        zip.closeEntry();
    }

    // escapes text for XML, dropping characters XML 1.0 can't carry at all
    static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    b.append("&amp;");
                    break;
                case '<':
                    b.append("&lt;");
                    break;
                case '>':
                    b.append("&gt;");
                    break;
                case '"':
                    b.append("&quot;");
                    break;
                case '\'':
                    b.append("&apos;");
                    break;
                default:
                    if (c >= 0x20 || c == '\n' || c == '\r' || c == '\t') {
                        if (c != 0xFFFE && c != 0xFFFF) b.append(c);
                    }
            }
        }
        return b.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    interface Progress {
        // may throw to abort the build
        void onChapter(int done, int total);
    }

    static final class Book {
        final List<Section> sections = new ArrayList<>();
        String id;
        String title;
        String author;
        String description;
        String language = "en";
        String source;
        File cover;
        File baseDir;
    }

    static final class Section {
        final String title;
        final File text;

        Section(String title, File text) {
            this.title = title;
            this.text = text;
        }
    }
}
