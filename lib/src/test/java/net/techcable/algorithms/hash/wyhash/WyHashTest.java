// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class WyHashTest {
    @ParameterizedTest
    @DisplayName("Test wyHash for byte[]")
    @MethodSource("testData")
    public void wyHashTest(TestData data) {
        var ascii = data.getAsciiMsg();
        long actual = WyHash.of().withSeed(data.seed).wyHash(ascii);
        assertEquals(data.expectedHash, actual);
    }

    private static final long RAND_SEED = 0x404bc51e92426043L;

    @ParameterizedTest
    @DisplayName("Test wyHash for byte[] with offset")
    @MethodSource("testData")
    public void wyHashTestOffset(TestData testData) {
        final byte[] ascii = testData.getAsciiMsg();
        var rand = new Random(RAND_SEED);
        final byte[] randomData = new byte[2048 + MAX_MSG_LENGTH_ASCII];
        rand.nextBytes(randomData);
        int[] offsets = new int[] {0, 1, 2, 3, 4, 6, 7, 8, 13, 16, 19, 31, 32, 127, 257, 482, 582, 1000, 1024, 2000};
        var hasher = WyHash.of().withSeed(testData.seed);
        for (int offset : offsets) {
            byte[] bytes = randomData.clone();
            System.arraycopy(ascii, 0, bytes, offset, ascii.length);
            long actualHash = hasher.wyHash(bytes, offset, ascii.length);
            assertEquals(testData.expectedHash(), actualHash, () -> "Bad hash for offset " + offset);
        }
        assertArrayEquals(Arrays.stream(offsets).sorted().toArray(), offsets);
    }

    @ParameterizedTest
    @DisplayName("Test wyHash for DirectByteBuffer")
    @MethodSource("testData")
    public void wyHashDirectByteBufferTest(TestData data) {
        var ascii = data.getAsciiMsg();
        var buffer = ByteBuffer.allocateDirect(ascii.length);
        assertEquals(ascii.length, buffer.limit());
        buffer.put(ascii);
        assertEquals(ascii.length, buffer.position());
        long actual = WyHash.of().withSeed(data.seed).wyHash(ascii);
        assertEquals(data.expectedHash, actual);
    }

    public record TestData(String msg, long seed, long expectedHash) {
        public byte[] getAsciiMsg() {
            return this.msg.getBytes(StandardCharsets.US_ASCII);
        }
    }

    private static final List<String> TEST_MSGS = List.of(
            "",
            "a",
            "abc",
            "message digest",
            "abcdefghijklmnopqrstuvwxyz",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890");
    private static final int MAX_MSG_LENGTH_ASCII =
            TEST_MSGS.stream().mapToInt(String::length).max().orElseThrow();
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
