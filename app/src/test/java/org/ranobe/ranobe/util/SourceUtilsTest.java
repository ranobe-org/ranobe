package org.ranobe.ranobe.util;

import static org.junit.Assert.assertEquals;
import static org.ranobe.ranobe.util.SourceUtils.buildUrl;
import static org.ranobe.ranobe.util.SourceUtils.cleanContent;
import static org.ranobe.ranobe.util.SourceUtils.generateId;

import org.jsoup.Jsoup;
import org.junit.Test;

public class SourceUtilsTest {
    @Test
    public void buildUrl_ShouldHandleInvalidString() {
        assertEquals("api/base", buildUrl("api/", null, "base"));
        assertEquals("base", buildUrl(null, "base"));
    }

    @Test
    public void buildUrl_ShouldReturnConcatResult() {
        assertEquals("api/base", buildUrl("api", "/", "base"));
        assertEquals("api/v1/base", buildUrl("api", "/v1/", "base"));
    }

    @Test
    public void cleanContent_ShouldReturnCleanString() {
        assertEquals("this\n\n text", cleanContent("this\n\n\n text"));
        assertEquals("this\n\n text\n\n", cleanContent("this\n\n\n text\n\n\n"));
    }

    @Test
    public void generateId_ShouldReturnSomeId() {
        generateId("someurl");
    }


    @Test
    public void contentFromHtml_KeepsParagraphsAndImages() {
        String html = "<div class='words'><div><div><p>First   line.</p><i></i></div></div>"
                + "<div><p>Second <em>line</em>.</p></div>"
                + "<p><img src='https://example.com/a.png'></p>"
                + "<blockquote><p>Quoted</p></blockquote></div>";
        String content = SourceUtils.contentFromHtml(Jsoup.parse(html).selectFirst("div.words"));
        assertEquals("First line.\n\nSecond line.\n\n[img]https://example.com/a.png[/img]\n\nQuoted", content);
    }

    @Test
    public void contentFromHtml_FallsBackToText() {
        assertEquals("just text", SourceUtils.contentFromHtml(Jsoup.parse("<div>just text</div>").selectFirst("div")));
    }

    @Test
    public void parseIsoDay_AcceptsWithAndWithoutMillis() {
        String day = java.text.SimpleDateFormat.getDateInstance(java.text.SimpleDateFormat.MEDIUM)
                .format(new java.util.GregorianCalendar(2019, java.util.Calendar.DECEMBER, 3, 12, 0).getTime());
        assertEquals(day, SourceUtils.parseIsoDay("2019-12-03T12:00:00Z"));
        assertEquals(day, SourceUtils.parseIsoDay("2019-12-03T12:00:00.000Z"));
        assertEquals("", SourceUtils.parseIsoDay("not a date"));
    }
}
