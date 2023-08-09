package net.techcable.wyhash_java.utils;

public final class StringHelpers {
    private StringHelpers() {}

    private static final short NON_ASCII_CHAR_MASK = (short) 0xFF80;
    public static boolean isAscii(char c) {
        return (c & NON_ASCII_CHAR_MASK) == 0;
    }
    private static final long NON_ASCII_MASK_LONG = ByteHelpers.longFromShorts(
            NON_ASCII_CHAR_MASK,
            NON_ASCII_CHAR_MASK,
            NON_ASCII_CHAR_MASK,
            NON_ASCII_CHAR_MASK
    );
    public static boolean isAllAscii(char a, char b, char c, char d) {
        return (ByteHelpers.longFromShorts((short) a, (short) b, (short) c, (short) d) & NON_ASCII_MASK_LONG) == 0;
    }
}