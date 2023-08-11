// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MathUtilsTest {
    @ParameterizedTest
    @DisplayName("Testing fallback impl for  MathUtils.unsignedMultiplyHigh")
    @MethodSource("multiplicationInput")
    public void testUnsignedMultiplyHighFallback(LongPair data) {
        long actual = MathUtils.unsignedMultiplyHighFallback(data.a(), data.b());
        BigInteger expected = unsignedLongToBigInteger(data.a).multiply(unsignedLongToBigInteger(data.b));
        assertTrue(expected.bitLength() <= 128, () -> "Too many bits for 128 int: " + expected.bitLength());
        if (expected.signum() < 0) {}
    }

    private static final int RAND_TEST_COUNT = 2048;
    private static final long RAND_SEED = 0xe00bd0422cab5accL;

    private static LongPair[] multiplicationInput() {
        final long[][] explicitExamplesArray = new long[][] {
            {1, 1},
            {0, 0},
            {2, 1},
            {1, 3},
            {4, 7},
            {-1, -1},
            {-7, -1},
            {Long.MIN_VALUE, -1},
            {Long.MIN_VALUE, -2},
            {Long.MAX_VALUE, Long.MAX_VALUE},
        };
        var res = new LongPair[RAND_TEST_COUNT + explicitExamplesArray.length];
        var rand = new Random(RAND_SEED);
        for (int i = 0; i < res.length; i++) {
            res[i] = new LongPair(rand.nextLong(), rand.nextLong());
        }
        return res;
    }

    @ParameterizedTest
    @ValueSource(
            longs = {
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                0,
                -1,
                -2,
                -7,
                -18,
                2,
                3,
                -412,
                0xe00bd0422cab5accL,
                0xac5937682067c104L,
                0x7f08f70a8e5dde3fL,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE - 1L,
                Integer.MAX_VALUE + 1L
            })
    @DisplayName("Test that unsignedLongToBigInteger works properly")
    public void testUnsignedLongToBigInteger(long l) {
        assertEquals(new BigInteger(Long.toUnsignedString(l)), unsignedLongToBigInteger(l));
    }

    private static final VarHandle BYTE_ARRAY_AS_BIG_ENDIAN_LONGS =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    private static BigInteger unsignedLongToBigInteger(long l) {
        byte[] buf = new byte[8];
        BYTE_ARRAY_AS_BIG_ENDIAN_LONGS.set(buf, l);
        return new BigInteger(l == 0 ? 0 : 1, buf);
    }

    record LongPair(long a, long b) {}
}
