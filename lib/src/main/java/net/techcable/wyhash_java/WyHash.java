package net.techcable.wyhash_java;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;

import net.techcable.wyhash_java.memory.MemorySection;
import net.techcable.wyhash_java.utils.Int128;
import net.techcable.wyhash_java.utils.MathUtils;

public final class WyHash {
    private long seed;
    private long a, b;
    private final long secret0, secret1, secret2, secret3;
    private final long originalSeed;

    private WyHash(long seed, Secret secret) {
        this.originalSeed = seed;
        this.secret0 = secret.a;
        this.secret1 = secret.b;
        this.secret2 = secret.c;
        this.secret3 = secret.d;
        this.reset();
    }

    /**
     * Reset the internal state of this hasher.
     */
    private void reset() {
        this.a = this.b = -1;
        this.seed = this.originalSeed;
    }

    public static WyHash create() {
        return new WyHash(0, Secret.DEFAULT);
    }

    public WyHash withSeed(long seed) {
        return new WyHash(seed, this.getSecret());
    }

    public WyHash withSecret(Secret secret) {
        return new WyHash(this.originalSeed, secret);
    }

    public Secret getSecret() {
        return new Secret(this.secret0, this.secret1, this.secret2, this.secret3);
    }

    public long getSeed() {
        return this.originalSeed;
    }

    private void setBothAB(Int128 vals) {
        a = vals.lowBits();
        b = vals.highBits();
    }

    //
    // Implementation: Ported from C
    //

    private static long wyMix(long a, long b) {
        long low = a * b;
        long high = MathUtils.unsignedMultiplyHigh(a, b);
        return low ^ high;
    }

    public static final ByteOrder REQUIRED_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    // _wyr3
    private static long readThreeOrFewerBytes(MemorySection section, long size) {
        assert size > 0 && size <= 3;
        /* return (((uint64_t)p[0])<<16)|(((uint64_t)p[k>>1])<<8)|p[k-1]; */
        return (long) section.getByte(0) << 16 | (long) section.getByte(size >> 1) << 8 | section.getByte(size - 1);
    }

    public long wyHash(byte[] bytes) {
        return wyHash(bytes, 0, bytes.length);
    }

    public long wyHash(byte[] bytes, int startOffset, int length) {
        Objects.checkFromIndexSize(startOffset, length, bytes.length);
        return wyHash(MemorySection.ofArray(bytes, startOffset, length));
    }

    public long wyHash(MemorySection originalSection) {
        this.reset();
        final MemorySection section = originalSection.withOrder(REQUIRED_BYTE_ORDER);
        final long length = section.length();
        assert length >= 0;
        this.seed ^= wyMix(seed ^ secret0, secret1);
        if (length <= 16) {
            if (length >= 4) {
                /* a=(_wyr4(p)<<32)|_wyr4(p+((len>>3)<<2)) */
                this.a = ((long) originalSection.getInt(0) << 32) | ((long) originalSection.getInt((length >> 3) << 2));
                /*  b=(_wyr4(p+len-4)<<32)|_wyr4(p+len-4-((len>>3)<<2)) */
                this.b = (long) originalSection.getInt(length - 4) << 32
                        | (long) originalSection.getInt(length - 4 - ((length >> 3) << 2));
            } else if (length > 0) {
                this.a = readThreeOrFewerBytes(section, length);
                this.b = 0;
            } else {
                a = b = 0;
            }
        } else {
            // manually outlined for speed
            this.wyHashLarge(section, length);
        }
        a ^= secret1;
        b ^= seed;
        setBothAB(MathUtils.unsignedMultiplyFull(a, b));
        return wyMix(a ^ secret0 ^ length, b ^ secret1);
    }

    private void wyHashLarge(MemorySection section, long length) {
        long i = length;
        long offset = 0;
        if (i > 48) {
            long see1 = seed, see2 = see1;
            do {
                seed = wyMix(section.getLong(offset) ^ secret1, section.getLong(offset + 8) ^ seed);
                see1 = wyMix(section.getLong(offset + 16) ^ secret2, section.getLong(offset + 24) ^ see1);
                see2 = wyMix(section.getLong(offset + 32) ^ secret3, section.getLong(offset + 40) ^ see2);
                offset += 48;
                i -= 48;
            } while (i > 48);
            seed ^= see1 ^ see2;
        }
        while (i > 16) {
            seed = wyMix(section.getLong(offset) ^ secret1, section.getLong(offset + 8) ^ seed);
            i -= 16;
            offset += 16;
        }
        this.a = section.getLong(offset + i - 16);
        this.b = section.getLong(offset + i - 8);
    }

    /**
     * A secret value used to provide (some) collision resistance to the hash.
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @param d the fourth value
     */
    public record Secret(long a, long b, long c, long d) {
        /**
         * The number of entries in a secret.
         */
        public static final int LENGTH = 4;

        /**
         * Get the value at the specified index.
         *
         * @param index the index to get at
         * @throws IndexOutOfBoundsException if the index is not valid
         * @return the value at the specified index
         */
        public long get(int index) {
            return switch (index) {
                case 0 -> a;
                case 1 -> b;
                case 2 -> c;
                case 3 -> d;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        /**
         * Convert this value into an array.
         *
         * @return this value as an array
         */
        public long[] toArray() {
            return new long[] {a, b, c, d};
        }

        /**
         * Convert the specified array of values into a secret.
         *
         * @param array the array of values
         * @throws IllegalArgumentException if the array does not have exactly {@link #LENGTH} entries.
         * @return a secret with the specified values
         * @see #toArray() the reverse direction: converting from a Secret into an array
         */
        public static Secret fromArray(long[] array) {
            if (array.length != LENGTH) {
                throw new IllegalArgumentException("Unexpected length: " + array.length);
            }
            return new Secret(array[0], array[1], array[2], array[3]);
        }

        /**
         * The default values of the secret,
         * used if nothing else is specified.
         */
        public static final Secret DEFAULT =
                new Secret(0xa0761d6478bd642fL, 0xe7037ed1a0b428dbL, 0x8ebc6af09c88c6e3L, 0x589965cc75374cc3L);

        @Override
        public String toString() {
            return Arrays.stream(this.toArray())
                    .mapToObj(HexFormat.of().withPrefix("0x")::toHexDigits)
                    .collect(Collectors.joining(", ", "Secret(", ")"));
        }
    }
}
