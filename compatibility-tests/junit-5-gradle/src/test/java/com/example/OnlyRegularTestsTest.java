package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Only Regular Tests")
class OnlyRegularTestsTest {

    @Test
    @DisplayName("Regular test without TableTest")
    void regularTest() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @DisplayName("Another regular test")
    void anotherRegularTest() {
        assertEquals(6, 2 * 3);
    }
}
