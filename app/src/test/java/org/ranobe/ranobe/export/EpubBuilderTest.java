package org.ranobe.ranobe.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

public class EpubBuilderTest {
    // smallest valid PNG header, enough for the format sniffing
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static void parse(String name, byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // the DOCTYPE has no DTD, nothing to fetch
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new AssertionError(name + " isn't well formed: " + e.getMessage(), e);
        }
    }

    private static void write(File file, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        return out.toByteArray();
    }

    @Test
    public void writesAWellFormedBook() throws Exception {
        File dir = temp.newFolder("job");
        File images = new File(dir, "images");
        assertTrue(images.mkdirs());
        write(new File(images, "i1.png"), PNG);
        write(new File(dir, "cover.png"), PNG);
        write(new File(dir, "c00001.txt"), "First line\nsame paragraph\n\nA <tag> & \"quote\"\n\n[img]images/i1.png[/img]\n\nAfter".getBytes(StandardCharsets.UTF_8));
        write(new File(dir, "c00002.txt"), "Only text\u0001 with a control char\n\n[img]https://example.com/missing.png[/img]".getBytes(StandardCharsets.UTF_8));

        EpubBuilder.Book book = new EpubBuilder.Book();
        book.id = "urn:uuid:test";
        book.title = "Title & Co";
        book.author = "Someone";
        book.description = "Line one\n\nLine two";
        book.source = "https://example.com/novel?a=1&b=2";
        book.cover = new File(dir, "cover.png");
        book.baseDir = dir;
        book.sections.add(new EpubBuilder.Section("Chapter 1", new File(dir, "c00001.txt")));
        book.sections.add(new EpubBuilder.Section("Chapter <2>", new File(dir, "c00002.txt")));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int[] progress = new int[1];
        EpubBuilder.write(book, out, (done, total) -> progress[0] = done);
        assertEquals(2, progress[0]);

        Map<String, byte[]> entries = new LinkedHashMap<>();
        String first = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (first == null) {
                    first = entry.getName();
                    assertEquals(ZipEntry.STORED, entry.getMethod());
                }
                entries.put(entry.getName(), read(zip));
            }
        }
        assertEquals("mimetype", first);
        assertEquals("application/epub+zip", new String(entries.get("mimetype"), StandardCharsets.US_ASCII));
        assertTrue(entries.containsKey("OEBPS/images/cover.png"));
        assertTrue(entries.containsKey("OEBPS/images/i1.png"));

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.endsWith(".xhtml") || name.endsWith(".opf") || name.endsWith(".ncx") || name.endsWith(".xml")) {
                parse(name, entry.getValue());
            }
        }

        String chapter1 = new String(entries.get("OEBPS/text/c00001.xhtml"), StandardCharsets.UTF_8);
        assertTrue(chapter1.contains("<p>First line<br/>same paragraph</p>"));
        assertTrue(chapter1.contains("A &lt;tag&gt; &amp; &quot;quote&quot;"));
        assertTrue(chapter1.contains("<img src=\"../images/i1.png\""));

        // images that were never fetched are dropped, not linked
        String chapter2 = new String(entries.get("OEBPS/text/c00002.xhtml"), StandardCharsets.UTF_8);
        assertFalse(chapter2.contains("example.com"));

        String opf = new String(entries.get("OEBPS/content.opf"), StandardCharsets.UTF_8);
        assertTrue(opf.contains("href=\"images/i1.png\" media-type=\"image/png\""));
        assertTrue(opf.contains("properties=\"cover-image\""));
        assertTrue(opf.indexOf("idref=\"chapter-1\"") < opf.indexOf("idref=\"chapter-2\""));
    }

    @Test
    public void bookWithoutCoverLeavesCoverOut() throws Exception {
        File dir = temp.newFolder("plain");
        write(new File(dir, "c00001.txt"), "Hello".getBytes(StandardCharsets.UTF_8));
        EpubBuilder.Book book = new EpubBuilder.Book();
        book.id = "urn:uuid:plain";
        book.title = "Plain";
        book.baseDir = dir;
        book.sections.add(new EpubBuilder.Section("One", new File(dir, "c00001.txt")));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EpubBuilder.write(book, out, (done, total) -> {
        });
        String all = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        assertFalse(all.contains("text/cover.xhtml"));
    }

    @Test(expected = IOException.class)
    public void refusesAnEmptyBook() throws Exception {
        EpubBuilder.Book book = new EpubBuilder.Book();
        book.id = "x";
        book.title = "Empty";
        EpubBuilder.write(book, new ByteArrayOutputStream(), (done, total) -> {
        });
    }
}
