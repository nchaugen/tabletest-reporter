package com.example;

import io.github.nchaugen.tabletest.junit.Description;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("String Utilities")
@Description("String manipulation operations")
class StringUtilsTest {

    @DisplayName("Uppercase Conversion")
    @Description("Convert strings to uppercase")
    @TableTest("""
        input | uppercase?
        hello | HELLO
        world | WORLD
        """)
    void testUppercase(String input, String expectedUppercase) {
        assertEquals(expectedUppercase, input.toUpperCase());
    }

    @DisplayName("String Length")
    @Description("Calculate string length")
    @TableTest("""
        text  | length?
        a     | 1
        hello | 5
        """)
    void testLength(String text, int expectedLength) {
        assertEquals(expectedLength, text.length());
    }
}
