package org.ranobe.ranobe.util;

import static org.junit.Assert.assertEquals;
import static org.ranobe.ranobe.util.SourceUtils.buildUrl;
import static org.ranobe.ranobe.util.SourceUtils.cleanContent;
import static org.ranobe.ranobe.util.SourceUtils.generateId;

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

}