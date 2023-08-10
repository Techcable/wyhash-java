// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jetbrains.annotations.VisibleForTesting;

import net.techcable.algorithms.hash.wyhash.WyHash;

/**
 * Miscellaneous math utilities needed for {@link WyHash}.
 */
public final class MathUtils {
    // Want to prefer JDK impl, because it is @IntrinsicCandidate
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
