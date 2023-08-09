package net.techcable.wyhash_java;

import java.lang.invoke.*;
import java.nio.ByteOrder;
import java.util.Objects;

import net.techcable.wyhash_java.memory.MemorySection;

public final class WyHash {
    private static final MethodHandle unsignedMultiplyHighImpl;

    static {
        final MethodType mtype = MethodType.methodType(long.class, long.class, long.class);
        MethodHandle unsignedMultiplyHigh;
        try {
            try {
                // Prefer JDK impl (hopefully can use intrinsic)
                unsignedMultiplyHigh =
                        MethodHandles.publicLookup().findStatic(Math.class, "unsignedMultiplyHigh", mtype);
            } catch (NoSuchMethodException ignored) {
                try {
                    unsignedMultiplyHigh =
                            MethodHandles.lookup().findStatic(WyHash.class, "unsignedMultiplyHighFallback", mtype);
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(e);
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        unsignedMultiplyHighImpl = unsignedMultiplyHigh;
    }

    private static long unsignedMultiplyHigh(long x, long y) {
        try {
            return (long) unsignedMultiplyHighImpl.invokeExact(x, y);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static long unsignedMultiplyHighFallback(long x, long y) {
        // Taken from JDK:
        // https://github.com/openjdk/jdk/blob/593ba2fe47ce/src/java.base/share/classes/java/lang/Math.java#L1428-L1432
        long result = Math.multiplyHigh(x, y);
        result += (y & (x >> 63));
        result += (x & (y >> 63));
        return result;
    }

    //
    // Implementation: Ported from C
    //

    private record Int128(long low, long high) {
        // _wymum
        public Int128 wyMum() {
            return new Int128(low * high, unsignedMultiplyHigh(low, high));
        }

        // _wymix
        public long wyMix() {
            var res = this.wyMum();
            return res.low ^ res.high;
        }
    }

    public static final ByteOrder REQUIRED_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    // _wyr3
    private static long readThreeOrFewerBytes(MemorySection section, long size) {
        assert size > 0 && size <= 3;
        /* return (((uint64_t)p[0])<<16)|(((uint64_t)p[k>>1])<<8)|p[k-1]; */
        return (long) section.getByte(0) << 16 | (long) section.getByte(size >> 1) << 8 | section.getByte(size - 1);
    }

    public static long wyHash(byte[] bytes) {
        return wyHash(bytes, 0, bytes.length);
    }

    public static long wyHash(byte[] bytes, int startOffset, int length) {
        Objects.checkFromIndexSize(startOffset, length, bytes.length);
        // TODO: Allow customizing secret & seed
        return wyHash(MemorySection.ofArray(bytes, startOffset, length), 0, Secret.DEFAULT);
    }

    public static long wyHash(MemorySection originalSection, long seed, Secret secret) {
        final MemorySection section = originalSection.withOrder(REQUIRED_BYTE_ORDER);
        final long length = section.length();
        Objects.checkFromIndexSize(0, length, section.length());
        seed ^= new Int128(seed ^ secret.a, secret.b).wyMix();
        long a, b;
        if (length <= 16) {
            if (length >= 4) {
                /* a=(_wyr4(p)<<32)|_wyr4(p+((len>>3)<<2)) */
                a = ((long) originalSection.getInt(0) << 32) | ((long) originalSection.getInt((length >> 3) << 2));
                /*  b=(_wyr4(p+len-4)<<32)|_wyr4(p+len-4-((len>>3)<<2)) */
                b = (long) originalSection.getInt(length - 4) << 32
                        | (long) originalSection.getInt(length - 4 - ((length >> 3) << 2));
            } else if (length > 0) {
                a = readThreeOrFewerBytes(section, length);
                b = 0;
            } else {
                a = b = 0;
            }
        } else {
            var large = wyHashLarge(section, length, seed, secret);
            a = large.a;
            b = large.b;
            seed = large.seed;
        }
        a ^= secret.b;
        b ^= seed;
        var res = new Int128(a, b).wyMum();
        a = res.low;
        b = res.high;
        return new Int128(a ^ secret.a ^ length, b ^ secret.b).wyMix();
    }

    private static LargeHashResult wyHashLarge(MemorySection section, long length, long seed, Secret secret) {
        long i = length;
        long offset = 0;
        if (i > 48) {
            long see1 = seed, see2 = see1;
            do {
                seed = new Int128(section.getLong(offset) ^ secret.b, section.getLong(offset + 8) ^ seed).wyMix();
                see1 = new Int128(section.getLong(offset + 16) ^ secret.c, section.getLong(offset + 24) ^ see1).wyMix();
                see2 = new Int128(section.getLong(offset + 32) ^ secret.d, section.getLong(offset + 40) ^ see2).wyMix();
                offset += 48;
                i -= 48;
            } while (i > 48);
            seed ^= see1 ^ see2;
        }
        while (i > 16) {
            seed = new Int128(section.getLong(offset) ^ secret.b, section.getLong(offset + 8) ^ seed).wyMix();
            i -= 16;
            offset += 16;
        }
        return new LargeHashResult(section.getLong(offset + i - 16), section.getLong(offset + i - 8), seed);
    }

    private record LargeHashResult(long a, long b, long seed) {}

    public record Secret(long a, long b, long c, long d) {
        public static final Secret DEFAULT =
                new Secret(0xa0761d6478bd642fL, 0xe7037ed1a0b428dbL, 0x8ebc6af09c88c6e3L, 0x589965cc75374cc3L);
    }
}
