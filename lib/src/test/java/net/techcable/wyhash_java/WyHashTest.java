package net.techcable.wyhash_java;

import net.techcable.wyhash_java.memory.MemorySection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WyHashTest {
    @ParameterizedTest
    @MethodSource("testData")
    public void wyHashTest(TestData data) {
        var ascii = data.msg.getBytes(StandardCharsets.US_ASCII);
        long actual = WyHash.wyHash(
                MemorySection.ofArray(ascii, 0, ascii.length),
                data.seed,
                WyHash.Secret.DEFAULT
        );
        assertEquals(data.expectedHash, actual);
    }

    record TestData(
            String msg,
            long seed,
            long expectedHash
    ) { }

    private static final List<String> TEST_MSGS = List.of(
            "",
            "a",
            "abc",
            "message digest",
            "abcdefghijklmnopqrstuvwxyz",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            "1234567890123456789012345678901234567890123456789012345678901234567890" +
            "1234567890"
    );
    private static final List<Long> TEST_HASHES = List.of(
            0x42bc986dc5eec4d3L,
            0x84508dc903c31551L,
            0x0bc54887cfc9ecb1L,
            0x6e2ff3298208a67cL,
            0x9a64e42e897195b9L,
            0x9199383239c32554L,
            0x7c1ccf6bba30f5a5L
    );
    static List<TestData> testData() {
        if (TEST_HASHES.size() != TEST_MSGS.size()) throw new AssertionError();
        return IntStream.range(0, TEST_MSGS.size()).mapToObj((i) -> new TestData(
                TEST_MSGS.get(i),
                i,
                TEST_HASHES.get(i)
        )).toList();
    }
}
