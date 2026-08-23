package org.tabletest.reporter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The front matter a project declares in the {@code frontMatter:} section of its
 * {@code tabletest-reporter.yaml} sidecar, written above every AsciiDoc and Markdown page so a site
 * generator can read it. Most entries are literal and identical on every page. Three keys are the
 * exception: {@code title}, {@code weight} and {@code generated} are filled by the reporter when
 * their declared value is {@code true}, because it knows the page's title, its position in the
 * declared reading order, and when the run happened.
 *
 * <p>Declared order is kept, so the generated block reads as it was written. An entry whose derived
 * value the reporter cannot supply for a page is dropped rather than written empty. The HTML format
 * writes no front matter at all — it is a complete page, not source for a generator.
 *
 * @param entries the declared keys in order, each mapped to its literal value or to {@link #DERIVED}
 */
public record FrontMatter(Map<String, Object> entries) {

    /** The marker a derived key carries instead of a literal value. */
    static final Object DERIVED = new Object();

    /** The keys the reporter fills itself, when they are declared as {@code true}. */
    private static final Set<String> DERIVABLE = Set.of("title", "weight", "generated");

    /** The absent case: no front matter is written, and both text formats report as before. */
    public static final FrontMatter NONE = new FrontMatter(Map.of());

    public FrontMatter {
        entries = new LinkedHashMap<>(entries);
    }

    /**
     * Parses the front matter from a raw YAML map (as loaded by {@link ContextLoader}). A document
     * with no {@code frontMatter:} section yields {@link #NONE}.
     */
    public static FrontMatter parse(Map<String, Object> yaml) {
        if (yaml == null || !(yaml.get("frontMatter") instanceof Map<?, ?> declared)) {
            return NONE;
        }
        Map<String, Object> entries = new LinkedHashMap<>();
        declared.forEach(
                (key, value) -> entries.put(String.valueOf(key), literalOrDerived(String.valueOf(key), value)));
        return new FrontMatter(entries);
    }

    /** True when at least one entry was declared, so the text formats have a block to write. */
    public boolean isPresent() {
        return !entries.isEmpty();
    }

    /** The declared keys, in the order they were written. */
    public List<String> keys() {
        return List.copyOf(entries.keySet());
    }

    /**
     * The entries for one page, each carrying the {@code key}, the {@code value} as written or as
     * derived, and a {@code yaml} scalar that is quoted and escaped where YAML needs it. An entry
     * whose derived value is null for this page is left out.
     *
     * @param title the page's title, for a derived {@code title}
     * @param weight the page's position among its siblings, for a derived {@code weight}
     * @param generatedAt the run timestamp, for a derived {@code generated}
     */
    public List<Map<String, Object>> entriesFor(String title, Integer weight, String generatedAt) {
        List<Map<String, Object>> rendered = new ArrayList<>();
        entries.forEach((key, declared) -> {
            Object value = declared == DERIVED ? derive(key, title, weight, generatedAt) : declared;
            if (value != null) {
                rendered.add(entry(key, value));
            }
        });
        return List.copyOf(rendered);
    }

    private static Object literalOrDerived(String key, Object value) {
        return DERIVABLE.contains(key) && Boolean.TRUE.equals(value) ? DERIVED : value;
    }

    private static Object derive(String key, String title, Integer weight, String generatedAt) {
        return switch (key) {
            case "title" -> title;
            case "weight" -> weight;
            case "generated" -> generatedAt;
            default -> null;
        };
    }

    private static Map<String, Object> entry(String key, Object value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("value", value);
        entry.put("yaml", yamlScalar(value));
        return entry;
    }

    /**
     * A YAML scalar for the value. Numbers and booleans are written bare; everything else is quoted,
     * so a colon, a hash or a leading digit in a title cannot end the value early.
     */
    private static String yamlScalar(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
