package com.codexpedia.app.watchweather.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilityTest {

    ////////////////////////////////////////////////////////////////
    //capitalizeEveryFristLetter method test
    ////////////////////////////////////////////////////////////////
    @Test
    public void testCapitalizeLetter1() {
        String actual = Utility.capitalizeEveryFristLetter("-d morning!");
        String expected = "-d Morning!";
        assertEquals("Should print -d Morning!", expected, actual);
    }

    @Test
    public void testCapitalizeLetter2() {
        String actual = Utility.capitalizeEveryFristLetter("hello, good morning!");
        String expected = "Hello, Good Morning!";
        assertEquals("Should print Hello, Good Morning!", expected, actual);
    }

    @Test
    public void testCapitalizeLetter3() {
        String actual = Utility.capitalizeEveryFristLetter("");
        String expected = "";
        assertEquals("Should return empty string.", expected, actual);
    }

    @Test
    public void testCapitalizeLetter4() {
        String actual = Utility.capitalizeEveryFristLetter(null);
        String expected = "";
        assertEquals("Should return empty string.", expected, actual);
    }




    ////////////////////////////////////////////////////////////////
    //formatDate method test
    ////////////////////////////////////////////////////////////////
    @Test
    public void testSunday() {
        String actual = Utility.formatDate("2015-12-27");
        String expected = "Sunday, December 27, 2015";
        assertEquals("Should be Sunday, December 27, 2015", expected, actual);
    }
    @Test
    public void testMonday() {
        String actual = Utility.formatDate("2015-12-28");
        String expected = "Monday, December 28, 2015";
        assertEquals("Should be Monday, December 28, 2015", expected, actual);
    }
    @Test
    public void testTuesday() {
        String actual = Utility.formatDate("2015-12-29");
        String expected = "Tuesday, December 29, 2015";
        assertEquals("Should be Tuesday, December 29, 2015", expected, actual);
    }
    @Test
    public void testWednesday() {
        String actual = Utility.formatDate("2015-12-30");
        String expected = "Wednesday, December 30, 2015";
        assertEquals("Should be Wednesday, December 30, 2015", expected, actual);
    }
    @Test
    public void testThursday() {
        String actual = Utility.formatDate("2015-12-31");
        String expected = "Thursday, December 31, 2015";
        assertEquals("Should be Thursday, December 31, 2015", expected, actual);
    }
    @Test
    public void testFriday() {
        String actual = Utility.formatDate("2016-1-1");
        String expected = "Friday, January 01, 2016";
        assertEquals("Should be Friday, January 1, 2016", expected, actual);
    }
    @Test
    public void testSaturday() {
        String actual = Utility.formatDate("2016-01-02");
        String expected = "Saturday, January 02, 2016";
        assertEquals("Should be Saturday, January 2, 2016", expected, actual);
    }
    @Test
    public void nullInputTest() {
        String actual = Utility.formatDate(null);
        String expected = "";
        assertEquals("The null input should return empty string", expected, actual);
    }
    @Test
    public void badStringTest() {
        String actual = Utility.formatDate("-2-2016");
        String expected = "";
        assertEquals("The bad input should return empty string", expected, actual);
    }
}
