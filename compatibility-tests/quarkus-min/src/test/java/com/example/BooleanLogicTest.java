package com.example;

import io.github.nchaugen.tabletest.junit.Description;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Boolean Logic")
@Description("Boolean operations and truth tables")
class BooleanLogicTest {

    @DisplayName("AND Operation")
    @Description("Test logical AND")
    @TableTest("""
        a     | b     | result?
        true  | true  | true
        true  | false | false
        false | true  | false
        false | false | false
        """)
    void testAnd(boolean a, boolean b, boolean expectedResult) {
        assertEquals(expectedResult, a && b);
    }

    @DisplayName("OR Operation")
    @Description("Test logical OR")
    @TableTest("""
        a     | b     | result?
        true  | true  | true
        true  | false | true
        false | true  | true
        false | false | false
        """)
    void testOr(boolean a, boolean b, boolean expectedResult) {
        assertEquals(expectedResult, a || b);
    }
}
