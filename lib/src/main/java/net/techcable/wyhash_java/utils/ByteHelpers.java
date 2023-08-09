package net.techcable.wyhash_java.utils;

public final class ByteHelpers {
    private ByteHelpers() {}

    private static long shortToUnsignedLong(short s) {
        return ((long) s) & 0xFFFF;
    }


    public static long longFromShorts(short a, short b, short c, short d) {
        return shortToUnsignedLong(a)
                | (shortToUnsignedLong(b) << 16)
                | (shortToUnsignedLong(c) << 32)
                | (shortToUnsignedLong(d) << 48);
    }
}
