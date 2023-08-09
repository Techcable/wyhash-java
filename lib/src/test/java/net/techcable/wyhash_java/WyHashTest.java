package net.techcable.wyhash_java;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.techcable.wyhash_java.memory.MemorySection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WyHashTest {
    @ParameterizedTest
    @MethodSource("testData")
    public void wyHashTest(TestData data) {
        var ascii = data.msg.getBytes(StandardCharsets.US_ASCII);
        long actual = WyHash.wyHash(MemorySection.ofArray(ascii, 0, ascii.length), data.seed, WyHash.Secret.DEFAULT);
        assertEquals(data.expectedHash, actual);
    }

    record TestData(String msg, long seed, long expectedHash) {}

    private static final List<String> TEST_MSGS = List.of(
            "",
            "a",
            "abc",
            "message digest",
            "abcdefghijklmnopqrstuvwxyz",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            "1234567890123456789012345678901234567890123456789012345678901234567890" + "1234567890");
    private static final List<Long> TEST_HASHES = List.of(
            0x0409638ee2bde459L,
            0xa8412d091b5fe0a9L,
            0x32dd92e4b2915153L,
            0x8619124089a3a16bL,
            0x7a43afb61d7f5f40L,
            0xff42329b90e50d58L,
            0xc39cab13b115aad3L);

    /*
     * The expected data, based on the output of `test_vector` in the original repo:
     * https://github.com/wangyi-fudan/wyhash/blob/77e50f267fbc7b8e2/test_vector.cppw
     *
     * WARNING: the `vector<uint64_t>` is NOT the expected hashes.
     * I'm not sure what it is.
     * To get the expected hashes, you have to actually run the program.
     */
    static List<TestData> testData() {
        if (TEST_HASHES.size() != TEST_MSGS.size()) throw new AssertionError();
        return IntStream.range(0, TEST_MSGS.size())
                .mapToObj((i) -> new TestData(TEST_MSGS.get(i), i, TEST_HASHES.get(i)))
                .toList();
    }
}
