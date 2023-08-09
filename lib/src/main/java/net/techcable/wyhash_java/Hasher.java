package net.techcable.wyhash_java;

import net.techcable.wyhash_java.utils.StringHelpers;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Maintains state for an in-progress hashing algorithm.
 * <p/>
 * Closely inspired by Rust's
 * <a href="https://doc.rust-lang.org/std/hash/trait.Hasher.html">{@code std::hash::Hasher}</a> API.
 */
public interface Hasher {
    /**
     * Get the name of the algorithm as a {@link JvmInternedString}.
     * <p>
     * The resulting string must be {@link String#intern() interned},
     * and is wrapped in a {@link JvmInternedString} to ensure that.
     *
     * @return the name of the algorithm.
     * @see #getAlgorithmName() which returns a string object.
     */
    JvmInternedString getAlgorithmNameInterned();

    /**
     * Get the name of the algorithm.
     * <p>
     *     The resulting string object <em>must</em> be
     *     identical to the {@link JvmInternedString#stringValue()} of
     *     {@link #getAlgorithmNameInterned()}.
     *     This guarantees the result will be {@link String#intern() interned}.
     * </p>
     *
     * @return the name of the algorithm
     * @see #getAlgorithmNameInterned() which places the interned string in a wrapper object
     * @see JvmInternedString#stringValue() for getting the underlying string
     */
    default String getAlgorithmName() {
        return getAlgorithmNameInterned().stringValue();
    }

    default void hashBytes(byte[] bytes) {
        this.hashBytes(bytes, 0, bytes.length);
    }

    void hashBytes(byte[] array, int offset, int length);

    default void hashBytes(ByteBuffer buf) {
        this.hashBytes(buf, buf.position(), buf.limit());
    }

    default void hashBytes(ByteBuffer buf, int start, int end) {
        Objects.checkFromToIndex(start, end, buf.limit());
        final int length = end - start;
        assert length >= 0;
        if (buf.hasArray()) {
            this.hashBytes(buf.array(), buf.arrayOffset() + start, length);
        } else if (buf.isDirect()) {
            this.hashDirectBytes(buf, start, end);
        } else {
            this.hashByteBufferFallback(buf, start, end);
        }
    }

    private void hashByteBufferFallback(ByteBuffer buf, int start, final int end) {
        final int length = end - start;
        byte[] temp = new byte[Math.min(1024, length)];
        while (start < end) {
            int toCopy = Math.min(end - start, temp.length);
            assert toCopy >= 0;
            buf.get(start, temp, 0, toCopy);
            this.hashBytes(temp, 0, toCopy);
            start += toCopy;
        }
        assert start == end;
    }

    void hashDirectBytes(ByteBuffer buf, int start, int end);

    void hashByte(byte b);

    void hashInt(int val);

    void hashLong(long val);

    default void hashString(String str) {
        this.hashString(str, 0, str.length());
    }

    default void hashString(String str, int charOffset, int length) {
        Objects.checkFromIndexSize(charOffset, length, str.length());
        final int endIndex = charOffset + length;
        assert endIndex < str.length();
        // ASCII fast path (TODO: When vector api becomes available, use that...)
        for (int i = charOffset; i < endIndex; i += 4) {
            char a = str.charAt(i);
            char b = str.charAt(i + 1);
            char c = str.charAt(i + 2);
            char d = str.charAt(i + 3);
            if (!StringHelpers.isAllAscii(a, b, c, d)) break;
            hashInt(a & 0xFF);
            hashInt(b & 0xFF);
            hashInt(c & 0xFF);
            hashInt(d & 0xFF);
        }
    }

    private void hashStringFallback(String str, int charOffset, int endIndex) {
        for (int i = charOffset; i < endIndex; i++) {
            char c = str.charAt(i);
            byte byteCast = (byte) c;
        }
    }

    default void hashDouble(double val) {
        hashLong(Double.doubleToLongBits(val));
    }

    default void hashFloat(float val) {
        hashInt(Float.floatToIntBits(val));
    }

    int finishInt();

    long finishLong();
}
