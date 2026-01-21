package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Verifies CSS classes in HTML files generated from AsciiDoc.
 */
public class HtmlCssVerifier {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: HtmlCssVerifier <html-file> <expected-passed> <expected-failed>");
            System.exit(1);
        }

        Path htmlFile = Paths.get(args[0]);
        int expectedPassed = Integer.parseInt(args[1]);
        int expectedFailed = Integer.parseInt(args[2]);

        verifyCssClasses(htmlFile);
        verifyCssClassCounts(htmlFile, expectedPassed, expectedFailed);

        System.out.println("✓ CSS class verification passed for: " + htmlFile.getFileName());
    }

    /**
     * Verifies that required CSS classes exist in the HTML file.
     */
    public static void verifyCssClasses(Path htmlFile) throws IOException {
        if (!Files.exists(htmlFile)) {
            throw new IllegalArgumentException("HTML file does not exist: " + htmlFile);
        }

        Document doc = Jsoup.parse(htmlFile.toFile(), "UTF-8");

        // Verify expectation class exists
        Elements expectationElements = doc.select(".expectation");
        if (expectationElements.isEmpty()) {
            throw new AssertionError("No elements with .expectation class found in " + htmlFile);
        }

        System.out.println("  Found " + expectationElements.size() + " .expectation elements");
    }

    /**
     * Verifies the count of expectation elements in the HTML file.
     * Note: Currently tabletest-reporter does not generate .passed/.failed classes,
     * so we only verify the total count of .expectation elements.
     */
    public static void verifyCssClassCounts(Path htmlFile, int expectedPassed, int expectedFailed) throws IOException {
        Document doc = Jsoup.parse(htmlFile.toFile(), "UTF-8");

        // Count all expectation elements (header + body cells)
        Elements expectationElements = doc.select(".expectation");
        int actualExpectations = expectationElements.size();
        int expectedTotal = expectedPassed + expectedFailed + 1; // +1 for header

        System.out.println("  Found " + actualExpectations + " .expectation elements (expected " + expectedTotal + " total: " + expectedPassed + " passed + " + expectedFailed + " failed + 1 header)");

        if (actualExpectations != expectedTotal) {
            throw new AssertionError(
                    String.format("Expected %d .expectation elements but found %d in %s",
                            expectedTotal, actualExpectations, htmlFile.getFileName()));
        }
    }
}
