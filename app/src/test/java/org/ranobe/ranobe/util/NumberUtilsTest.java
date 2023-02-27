package org.ranobe.ranobe.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ranobe.ranobe.util.NumberUtils.getRandom;
import static org.ranobe.ranobe.util.NumberUtils.toFloat;
import static org.ranobe.ranobe.util.NumberUtils.toInt;

import org.junit.Test;

public class NumberUtilsTest {

    @Test
    public void toFloat_ShouldReturn0ForInvalidString() {
        assertEquals(0F, toFloat(null), 0F);
        assertEquals(0F, toFloat("somerandom"), 0F);
        assertEquals(0F, toFloat(".kjfd."), 0F);
    }

    @Test
    public void toFloat_ShouldReturnValidFloat() {
        assertEquals(10F, toFloat("Chapter 10"), 0F);
        assertEquals(10.1F, toFloat("Chapter 10.1"), 0F);
        assertEquals(10.2F, toFloat("10.2Random"), 0F);
    }

    @Test
    public void toInt_ShouldReturn0ForInvalidString() {
        assertEquals(0, toInt(null));
        assertEquals(0, toInt("somerandom"));
        assertEquals(0, toInt(".kjfd."));
    }

    @Test
    public void toInt_ShouldReturnValidInt() {
        assertEquals(2022, toInt("Year 2022"));
        assertEquals(2002, toInt("Y2002"));
        assertEquals(2012, toInt("2012Year"));
    }

    @Test
    public void getRandom_ShouldReturnAnyRandom() {
        assertTrue(getRandom(10) <= 10);
    }
}