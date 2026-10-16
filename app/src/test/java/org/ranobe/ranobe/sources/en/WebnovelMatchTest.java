package org.ranobe.ranobe.sources.en;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WebnovelMatchTest {
    @Test
    public void keepsRealMatchesOnly() {
        assertTrue(Webnovel.matches("reverend insanity", "Reverend Insanity: The Son", ""));
        assertTrue(Webnovel.matches("reverend insanity", "Rio in RI", "A fan story of Reverend Insanity"));
        assertFalse(Webnovel.matches("reverend insanity", "Richest Rag-picker", "He picks rags and gets rich"));
        assertFalse(Webnovel.matches("shadow slave", "The Villain Professor's Second Chance", ""));
    }
}
