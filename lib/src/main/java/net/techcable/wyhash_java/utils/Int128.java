package net.techcable.wyhash_java.utils;

import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.Objects;

public record Int128(long lowBits, long highBits) {
    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

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

    public static final int BYTES = Long.BYTES * 2;

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
