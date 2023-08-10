// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash.utils;

import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.Objects;

/**
 * A 128-bit integer value.
 *
 * @param lowBits the least-significant 64 bits
 * @param highBits the most-significant 64 bits
 */
public record Int128(long lowBits, long highBits) {
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    /**
     * Convert this integer to a hexidecimal string.
     * <p>
     *     Unlike {@link Long#toHexString(long)},
     *     leading zeroes are <em>not</em> stripped.
     *     However, {@code _} separators are included every four digits (two bytes).
     * </p>
     *
     * @return a hex representation
     */
    public String toHexString() {
        final int expectedCapacity = (BYTES * 2) + (BYTES / 4) + 2;
        var res = new StringBuilder(expectedCapacity);
        res.append("0x");
        for (int i = 0; i < BYTES; i++) {
            if ((i & 2) == 0 && i != 0) res.append('_');
            HEX_FORMAT.toHexDigits(res, this.getByteAt(i, ByteOrder.LITTLE_ENDIAN));
        }
        assert res.length() <= expectedCapacity;
        return res.toString();
    }

    @Override
    public String toString() {
        return toHexString() + "U128";
    }

    /**
     * The number of bytes in an 128-bit integer ({@code 16}).
     */
    public static final int BYTES = 16;

    /**
     * Get the byte at the specified index, using a specific byte order.
     *
     * @param index the index of the byte within this value
     * @param order the byte ordering to interpret this as
     * @throws IndexOutOfBoundsException if the byte index is invalid
     * @return the byte value
     */
    public byte getByteAt(int index, ByteOrder order) {
        Objects.checkIndex(index, BYTES);
        Objects.requireNonNull(order, "Null ByteOrder");
        if (order == ByteOrder.BIG_ENDIAN) {
            return this.getByteBigEndianAt(index);
        } else if (order == ByteOrder.LITTLE_ENDIAN) {
            return this.getByteLittleEndianAt(index);
        } else {
            throw new AssertionError(order);
        }
    }

    private byte getByteLittleEndianAt(int index) {
        if (index < Long.BYTES) {
            return (byte) ((lowBits >>> (index << 3)) & 0xFF);
        } else {
            index -= Long.BYTES;
            assert index < Long.BYTES;
            return (byte) ((highBits >>> (index << 3)) & 0xFF);
        }
    }

    private byte getByteBigEndianAt(int index) {
        return getByteLittleEndianAt(BYTES - index);
    }
}
