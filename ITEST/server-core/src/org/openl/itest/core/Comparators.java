package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

final class Comparators {

    private static final int REGULAR_ARCHIVE_FILE_SIGN = 0x504B0304;
    private static final int EMPTY_ARCHIVE_FILE_SIGN = 0x504B0506;
    private static final String CRLF = "\r\n";

    private Comparators() {
    }

    static void txt(String message, byte[] expected, byte[] actual) {
        txt(message, new String(expected, StandardCharsets.UTF_8), new String(actual, StandardCharsets.UTF_8));
    }

    static void txt(String message, String expected, String actual) {
        if (actual == null) {
            assertEquals(expected, actual, message);
        }
        String regExp = getRegExp(expected);
        boolean matches = trimExtraSpaces(actual).matches(regExp);
        if (!matches) {
            fail(message);
        }
    }

    static void xml(String message, Object expected, Object actual) {
        DifferenceEvaluator evaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, matchByPattern());
        Iterator<Difference> differences = DiffBuilder.compare(expected)
                .withTest(actual)
                .ignoreWhitespace()
                .checkForSimilar()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes, ElementSelectors.byName))
                .withDifferenceEvaluator(evaluator)
                .build()
                .getDifferences()
                .iterator();
        if (differences.hasNext()) {
            fail(message + "\n" + differences.next());
        }
    }

    private static DifferenceEvaluator matchByPattern() {
        return (comparison, outcome) -> {
            if (outcome == ComparisonResult.DIFFERENT) {
                Node control = comparison.getControlDetails().getTarget();
                Node test = comparison.getTestDetails().getTarget();
                if (control != null && test != null) {
                    String controlValue = control.getNodeValue();
                    String testValue = test.getNodeValue();
                    if (controlValue != null && testValue != null) {
                        String regExp = getRegExp(controlValue);
                        String noSpaces = trimExtraSpaces(testValue);
                        if (noSpaces.equals(regExp) || Pattern.compile(regExp).matcher(noSpaces).matches()) {
                            return ComparisonResult.SIMILAR;
                        }
                    }
                }

                return outcome;
            }
            return outcome;
        };
    }

    private static String trimExtraSpaces(String testValue) {
        return testValue.trim().replaceAll("\\s+", " ");
    }

    private static String getRegExp(String text) {
        return patternToRegexp(trimExtraSpaces(text));
    }

    static void compareJsonObjects(JsonNode expectedJson, JsonNode actualJson, String path) {
        if (Objects.equals(expectedJson, actualJson)) {
            return;
        }
        if (expectedJson == null || actualJson == null) {
            failDiff(expectedJson, actualJson, path);
        } else if (expectedJson.isTextual()) {
            // try to compare by a pattern
            String regExp = patternToRegexp(expectedJson.asText());
            String actualText = actualJson.isTextual() ? actualJson.asText() : actualJson.toString();
            try {
                if (!Pattern.compile(regExp).matcher(actualText).matches()) {
                    failDiff(expectedJson, actualJson, path);
                }
            } catch (PatternSyntaxException e) {
                failDiff(expectedJson, actualJson, path);
            }
        } else if (expectedJson.isArray() && actualJson.isArray()) {
            for (int i = 0; i < expectedJson.size() || i < actualJson.size(); i++) {
                compareJsonObjects(expectedJson.get(i), actualJson.get(i), path + "[" + i + "]");
            }
        } else if (expectedJson.isObject() && actualJson.isObject()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            expectedJson.fieldNames().forEachRemaining(names::add);
            actualJson.fieldNames().forEachRemaining(names::add);

            for (String name : names) {
                compareJsonObjects(expectedJson.get(name), actualJson.get(name), path + " > " + name);
            }
        } else {
            failDiff(expectedJson, actualJson, path);
        }
    }

    static String patternToRegexp(String pattern) {
        return pattern
                .replace("\\", "\\\\")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("$", "\\$")
                .replace("^", "\\^")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replaceAll("#+", "[#\\\\d]+")
                .replaceAll("@+", "[@\\\\w]+")
                .replaceAll("\\*+", "[^\uFFFF]*");
    }

    private static void failDiff(JsonNode expectedJson, JsonNode actualJson, String path) {
        assertEquals(expectedJson, actualJson, "Path: \\" + path);
    }

    static void zip(byte[] expectedBytes, byte[] actualBytes) throws IOException {
        final Map<String, byte[]> expectedZipEntries = getZipEntries(expectedBytes);
        final Map<String, byte[]> actualZipEntries = getZipEntries(actualBytes);

        final Iterator<Map.Entry<String, byte[]>> actual = actualZipEntries.entrySet().iterator();
        while (actual.hasNext()) {
            final Map.Entry<String, byte[]> actualEntry = actual.next();
            if (expectedZipEntries.containsKey(actualEntry.getKey())) {
                assertArrayEquals(expectedZipEntries.remove(actualEntry.getKey()),
                        actualEntry.getValue(),
                        String.format("Zip entry [%s]: ", actualEntry.getKey()));
                actual.remove();
            }
        }

        boolean failed = false;
        StringBuilder errorMessage = new StringBuilder();
        Function<String, String> tab = s -> "    " + s;
        if (!actualZipEntries.isEmpty()) {
            failed = true;
            errorMessage.append("UNEXPECTED entries:")
                    .append(CRLF)
                    .append(actualZipEntries.keySet().stream().map(tab).collect(Collectors.joining(CRLF)));
        }
        if (!expectedZipEntries.isEmpty()) {
            if (failed) {
                errorMessage.append(CRLF);
            } else {
                failed = true;
            }
            errorMessage.append("MISSED entries:")
                    .append(CRLF)
                    .append(expectedZipEntries.keySet().stream().map(tab).collect(Collectors.joining(CRLF)));
        }
        if (failed) {
            fail(errorMessage.toString());
        }
    }

    private static Map<String, byte[]> getZipEntries(byte[] src) throws IOException {
        validateZipFileSign(src);
        Map<String, byte[]> dest = new HashMap<>();
        try (ZipInputStream actual = new ZipInputStream(new ByteArrayInputStream(src))) {
            ZipEntry actualEntry;
            while ((actualEntry = actual.getNextEntry()) != null) {
                if (actualEntry.getName().endsWith("/")) {
                    // skip folder
                    continue;
                }
                ByteArrayOutputStream target = new ByteArrayOutputStream();
                actual.transferTo(target);
                dest.put(actualEntry.getName(), target.toByteArray());
            }
        }
        return dest;
    }

    private static void validateZipFileSign(byte[] src) {
        if (src.length < 4) {
            fail("Incorrect zip archive");
        }
        int sign = ((src[0] << 24) + (src[1] << 16) + (src[2] << 8) + src[3]);
        if (sign != REGULAR_ARCHIVE_FILE_SIGN && sign != EMPTY_ARCHIVE_FILE_SIGN) {
            fail("Provided stream is not matched zip structure");
        }
    }
}
