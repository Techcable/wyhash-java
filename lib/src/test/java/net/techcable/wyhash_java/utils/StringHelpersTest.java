package net.techcable.wyhash_java.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class StringHelpersTest {
    private static final char MAX_ASCII = '\u007f';

    @ParameterizedTest
    @MethodSource("asciiTestChars")
    public void testIsAscii(AsciiTestChar test) {
        assertEquals(test.shouldBeAscii, StringHelpers.isAscii(test.charValue));
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Test
    public void testIsAsciiBulk() {
        assertTrue(StringHelpers.isAllAscii(MAX_ASCII, MAX_ASCII, MAX_ASCII, MAX_ASCII));
        assertTrue(StringHelpers.isAllAscii('\0', '\0', '\0', '\0'));
        assertTrue(StringHelpers.isAllAscii(MAX_ASCII, '\0', 'A', 'z'));
        assertFalse(StringHelpers.isAllAscii(MAX_ASCII, '\0', (char) (MAX_ASCII + 1), 'z'));
        assertFalse(StringHelpers.isAllAscii('\u2200', '\0', 'a', 'z'));
        assertFalse(StringHelpers.isAllAscii('A', 'Z', 'd', '\u2000'));
    }

    public record AsciiTestChar(
            char charValue,
            boolean shouldBeAscii
    ) {
        @Override
        public String toString() {
            return "{c=0x" + Integer.toHexString(charValue) + (shouldBeAscii ? ", ascii}" : "}");
        }
    }
    private static List<AsciiTestChar> asciiTestChars() {
        return genAsciiTestChars();
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static List<AsciiTestChar> genAsciiTestChars() {
        List<AsciiTestChar> res = new ArrayList<>(0xFF + 4);
        IntStream.rangeClosed(0, MAX_ASCII)
                .mapToObj((c) -> new AsciiTestChar((char) c, true))
                .forEach(res::add);
        IntStream.rangeClosed(MAX_ASCII + 1, 0xFF)
                .mapToObj((c) -> new AsciiTestChar((char) c, false))
                .forEach(res::add);
        res.add(new AsciiTestChar('\u01FF', false));
        res.add(new AsciiTestChar('\u11FF', false));
        res.add(new AsciiTestChar('\u2200', false));
        return Collections.unmodifiableList(res);
    }
}
