package com.example;

import io.github.nchaugen.tabletest.junit.Description;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DisplayName("Collection Operations")
@Description("Tests for list operations")
class CollectionTest {

    @DisplayName("List Size")
    @Description("Calculate size of lists")
    @TableTest("""
        elements      | size?
        []            | 0
        [1]           | 1
        [1, 2, 3]     | 3
        """)
    void testListSize(List<Integer> elements, int expectedSize) {
        assertEquals(expectedSize, elements.size());
    }

    @DisplayName("List Contains")
    @Description("Check if list contains element")
    @TableTest("""
        list      | element | contains?
        [1, 2, 3] | 2       | true
        [1, 2, 3] | 5       | false
        """)
    void testContains(List<Integer> list, int element, boolean expectedContains) {
        assertEquals(expectedContains, list.contains(element));
    }
}
