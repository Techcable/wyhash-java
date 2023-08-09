package net.techcable.wyhash_java.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Miscellaneous math utilities needed for {@link net.techcable.wyhash_java.WyHash}.
 */
public final class MathUtils {
    private static final MethodHandle UNSIGNED_MULTIPLY_HIGH_METHOD;

    static {
        final MethodType methodType = MethodType.methodType(long.class, long.class, long.class);
        MethodHandle unsignedMultiplyHigh;
        try {
            /*
             * Use the JDK impl if possible, because hopefully we can use the intrinsic.
             */
            try {
                //noinspection JavaLangInvokeHandleSignature
                unsignedMultiplyHigh =
                        MethodHandles.publicLookup().findStatic(Math.class, "unsignedMultiplyHigh", methodType);
            } catch (NoSuchMethodException ignored) {
                try {
                    unsignedMultiplyHigh = MethodHandles.lookup()
                            .findStatic(MathUtils.class, "unsignedMultiplyHighFallback", methodType);
                } catch (NoSuchMethodException e) {
                    // Should not be possible
                    throw new RuntimeException("Failed to find fallback method", e);
                }
            }
        } catch (IllegalAccessException e) {
            // Both methods should be accessible...
            throw new RuntimeException("Failed to access method", e);
        }
        UNSIGNED_MULTIPLY_HIGH_METHOD = unsignedMultiplyHigh;
    }

    private MathUtils() {}

    /**
     * Perform an unsigned 128-bit multiplication and return the most-significant 64 bits.
     * <p>
     * As of Java 18, this is included in the JDK as
     * <a href="https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/lang/Math.html#unsignedMultiplyHigh(long,long)">
     * Math.unsignedMultiplyHigh</a>
     * </p>
     *
     * @param x the first value to multiply
     * @param y the second value to multiply
     * @return the upper 64-bits of the unsigned multiplication
     * @see Math#multiplyHigh(long, long) for signed multiply high
     */
    public static long unsignedMultiplyHigh(long x, long y) {
        try {
            return (long) UNSIGNED_MULTIPLY_HIGH_METHOD.invokeExact(x, y);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Perform a "full" multiplication of the two specified {@code long} values.
     * <p>
     * Returns a {@link Int128 128-bit integer object} containing both parts of the results.
     * </p>
     *
     * @param x the first value to multiply
     * @param y the second value to multiply
     * @return the product of multiplication, as an {@link Int128}
     * @see #unsignedMultiplyHigh(long, long) just multiplying the high bits
     * @see Int128 the wrapper class used to store 128-bit integers
     */
    public static Int128 unsignedMultiplyFull(long x, long y) {
        return new Int128(x * y, unsignedMultiplyHigh(x, y));
    }

    /**
     * Fallback implementation of {@link #unsignedMultiplyHigh(long, long)}.
     *
     * @param x the x parameter
     * @param y the y parameter
     * @return the high bits of the product
     */
    @VisibleForTesting
    /* package */ static long unsignedMultiplyHighFallback(long x, long y) {
        /*
         * Based on Hackers Delight, 2nd ed, Section 8-3.
         * This is the same fallback impl that the JDK uses.
         */
        long p = Math.multiplyHigh(x, y);
        p += (x >> 63) & y;
        p += (y >> 63) & x;
        return p;
    }
}
