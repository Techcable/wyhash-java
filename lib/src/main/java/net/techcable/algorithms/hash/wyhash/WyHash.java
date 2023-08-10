// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import net.techcable.algorithms.hash.wyhash.memory.MemorySection;
import net.techcable.algorithms.hash.wyhash.utils.Int128;
import net.techcable.algorithms.hash.wyhash.utils.MathUtils;

/**
 * Represents the configuration options for the
 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a> algorithm.
 * <p>
 *     The default configuration can be obtained via {@link WyHash#of()}.
 *     The two parameter options are a {@link WyHash.Secret} and a "seed",
 *     set via {@link #withSecret(Secret)} and {@link #withSeed(long)} respectively.
 * </p>
 */
public final class WyHash {
    private final long secret0, secret1, secret2, secret3;
    private final long initialSeed;

    private WyHash(long seed, Secret secret) {
        this.initialSeed = seed;
        Objects.requireNonNull(secret, "Null secret");
        this.secret0 = secret.a;
        this.secret1 = secret.b;
        this.secret2 = secret.c;
        this.secret3 = secret.d;
    }

    private static final WyHash DEFAULT = new WyHash(0, Secret.DEFAULT);

    /**
     * Get a {@link WyHash} instance with the default configuration.
     *
     * @return the instance with the configuration
     */
    public static WyHash of() {
        return DEFAULT;
    }

    /**
     * Return a new instance with the specified seed.
     *
     * @param seed the seed for the new instance
     * @return a new hash config with the specified seed
     */
    public WyHash withSeed(long seed) {
        return seed == this.initialSeed ? this : new WyHash(seed, this.getSecret());
    }

    /**
     * Return a new instance using the specified secret.
     *
     * @param secret the secret for the new instance
     * @return a new hash config with the specified secret
     */
    public WyHash withSecret(@NotNull Secret secret) {
        return new WyHash(this.initialSeed, secret);
    }

    /**
     * Get the secret used for hashing.
     * <p>
     * This should be hidden from the user, to help resist hash-DOS attacks.
     * </p>
     *
     * @return the secret
     * @see #withSeed(long) for a way to change the secret
     */
    @NotNull
    public Secret getSecret() {
        return new Secret(secret0, secret1, secret2, secret3);
    }

    /**
     * Get the seed used for hashing.
     *
     * @return the seed
     * @see #withSeed(long) to create a new instance with a different seed
     */
    public long getSeed() {
        return this.initialSeed;
    }

    //
    // Implementation: Ported from C
    //

    private static long wyMix(long a, long b) {
        long low = a * b;
        long high = MathUtils.unsignedMultiplyHigh(a, b);
        return low ^ high;
    }

    /**
     * The {@link ByteOrder} that is required for the wyhash algorithm.
     */
    public static final ByteOrder REQUIRED_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    // _wyr3
    private static long readThreeOrFewerBytes(MemorySection section, long size) {
        assert size > 0 && size <= 3;
        /* return (((uint64_t)p[0])<<16)|(((uint64_t)p[k>>1])<<8)|p[k-1]; */
        return (long) section.getByte(0) << 16 | (long) section.getByte(size >> 1) << 8 | section.getByte(size - 1);
    }

    /**
     * Hash the specified byte array.
     *
     * @param bytes the byte array to hash.
     * @return the computed hash code
     */
    public long wyHash(byte[] bytes) {
        return wyHash(bytes, 0, bytes.length);
    }

    /**
     * Hash a sub-region of the specified byte array,.
     *
     * @param bytes the array to hash
     * @param startOffset the start index of where to begin hashing
     * @param length the number of bytes to hash
     * @throws IndexOutOfBoundsException if the specified offset &amp; length are out of bounds
     * @return the computed hash code
     */
    public long wyHash(byte[] bytes, int startOffset, int length) {
        Objects.checkFromIndexSize(startOffset, length, bytes.length);
        return wyHash(MemorySection.ofArray(bytes, startOffset, length));
    }

    /**
     * Hash the specified {@link MemorySection}.
     *
     * @param section the section to hash
     * @return the computed hash code
     */
    public long wyHash(MemorySection section) {
        return this.wyHash(new State(this), section.withOrder(REQUIRED_BYTE_ORDER));
    }

    /**
     * Represents temporary state of an individual hasher run.
     * <p>
     * This class is an implementation detail of {@link WyHash}.
     * </p>
     */
    private static final class State {
        private long seed;
        private long a, b;

        private State(WyHash setup) {
            this.seed = setup.initialSeed;
        }

        private void setBothAB(Int128 vals) {
            a = vals.lowBits();
            b = vals.highBits();
        }
    }

    private long wyHash(State state, MemorySection section) {
        final long length = section.length();
        assert length >= 0;
        state.seed ^= wyMix(initialSeed ^ secret0, secret1);
        if (length <= 16) {
            if (length >= 4) {
                /* a=(_wyr4(p)<<32)|_wyr4(p+((len>>3)<<2)) */
                state.a = ((long) section.getInt(0) << 32) | ((long) section.getInt((length >> 3) << 2));
                /*  b=(_wyr4(p+len-4)<<32)|_wyr4(p+len-4-((len>>3)<<2)) */
                state.b = (long) section.getInt(length - 4) << 32
                        | (long) section.getInt(length - 4 - ((length >> 3) << 2));
            } else if (length > 0) {
                state.a = readThreeOrFewerBytes(section, length);
                state.b = 0;
            } else {
                state.a = state.b = 0;
            }
        } else {
            // manually outlined for speed
            this.wyHashLarge(state, section);
        }
        state.a ^= secret1;
        state.b ^= state.seed;
        state.setBothAB(MathUtils.unsignedMultiplyFull(state.a, state.b));
        return wyMix(state.a ^ secret0 ^ length, state.b ^ secret1);
    }

    private void wyHashLarge(State state, MemorySection section) {
        long i = section.length();
        assert i > 16;
        long offset = 0;
        if (i > 48) {
            long see1 = state.seed, see2 = see1;
            do {
                state.seed = wyMix(section.getLong(offset) ^ secret1, section.getLong(offset + 8) ^ state.seed);
                see1 = wyMix(section.getLong(offset + 16) ^ secret2, section.getLong(offset + 24) ^ see1);
                see2 = wyMix(section.getLong(offset + 32) ^ secret3, section.getLong(offset + 40) ^ see2);
                offset += 48;
                i -= 48;
            } while (i > 48);
            state.seed ^= see1 ^ see2;
        }
        while (i > 16) {
            state.seed = wyMix(section.getLong(offset) ^ secret1, section.getLong(offset + 8) ^ state.seed);
            i -= 16;
            offset += 16;
        }
        state.a = section.getLong(offset + i - 16);
        state.b = section.getLong(offset + i - 8);
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

        private String joinToString(String prefix, String suffix) {
            return Arrays.stream(this.toArray())
                    .mapToObj(HexFormat.of().withPrefix("0x")::toHexDigits)
                    .collect(Collectors.joining(", ", prefix, suffix));
        }

        @Override
        public String toString() {
            return this.joinToString("Secret(", ")");
        }
    }

    @Override
    public String toString() {
        if (this.equals(DEFAULT)) {
            return "WyHash.DEFAULT";
        } else {
            var secret = this.getSecret();
            String secretRepr = secret.equals(Secret.DEFAULT) ? "Secret.DEFAULT" : secret.joinToString("[", "]");
            return "WyHash[seed=0x" + Long.toHexString(this.getSeed()) + ", secret=" + secretRepr + "]";
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getSeed(), this.getSecret());
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || obj instanceof WyHash hashConfig
                        && this.getSeed() == hashConfig.getSeed()
                        && this.getSecret().equals(hashConfig.getSecret());
    }
}
