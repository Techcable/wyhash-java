package net.techcable.wyhash_java.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Backport API for {@code java.lang.foreign.MemorySegment}.
 * <p>
 * Unlike {@link ByteBuffer}, this class should not have any mutable state.
 */
public abstract sealed class MemorySection permits HeapMemorySection, BufferMemorySection, ReverseMemorySection {
    private final ByteOrder order;

    /**
     * Initialize a memory section with the specified byte ordering.
     *
     * @param order the byte ordering.
     */
    protected MemorySection(ByteOrder order) {
        this.order = Objects.requireNonNull(order);
    }

    /**
     * Get the length of this memory section, in terms of bytes.
     *
     * @return the length
     */
    public abstract long length();

    /**
     * Return the byte ordering of this memory section.
     *
     * @return the ordering
     */
    public final ByteOrder order() {
        return this.order;
    }

    /**
     * Get a slice of the memory in the specified range.
     * <p>
     * This returns a <em>view</em> of a subsection of this memory
     * and does not perform any copies.
     * </p>
     * @param startIndex the starting index
     * @param newSize the size of the region
     * @throws IndexOutOfBoundsException if the index is invalid
     * @return a view of the specified subsection
     */
    public abstract MemorySection slice(long startIndex, long newSize);

    /**
     * Get the byte at the specified memory offset
     *
     * @param offset the memory offset
     * @throws IllegalArgumentException if the specified offset is out of bounds
     * @return the byte value
     * @see #getBytes(long, byte[], int, int) for reading an arbitrary number of bytes
     */
    public abstract byte getByte(long offset);

    /**
     * Read enough bytes to completely fill {@code dest}, starting at the specified memory offset.
     * <p>
     * This method copies exactly {@code dest.length} bytes.
     * It is a convenience wrapper around {@link #getBytes(long, byte[], int, int)}.
     *
     * @param offset the memory offset
     * @param dest the destination array to copy into
     * @throws IllegalArgumentException if this section does not have enough bytes after the specified memory offset
     */
    public void getBytes(long offset, byte[] dest) {
        this.getBytes(offset, dest, 0, dest.length);
    }

    /**
     * Read exactly {@code length} bytes into the destination buffer,
     * starting at the given memory offset.
     * <p>
     * Places input at offset {@code destOffset} into the destination array.
     * </p>
     *
     * @param offset the memory offset to start copying from
     * @param dest the destination array
     * @param destOffset the offset within the destination array
     * @param length the number of bytes to copy
     * @throws IllegalArgumentException if this section does not have enough bytes after the specified memory offset
     * @throws IndexOutOfBoundsException if the destination offset (or length) is out of bounds for the destination array
     * @see #getBytes(long, byte[]) for a conveience method when {@code destOffset == 0 && length == dest.length}
     */
    public abstract void getBytes(long offset, byte[] dest, int destOffset, int length);

    /**
     * Read two bytes starting at the specified memory offset,
     * interpreting it as a {@code short} using this section's {@link #order() byte ordering}.
     *
     * @param offset the memory offset to read
     * @throws IllegalArgumentException if the specified offset is out of bounds (or there aren't enough bytes)
     * @return the number that has been read
     * @see #getByte(long) for reading an individual byte
     * @see #getInt(long) for reading an integer
     * @see #order() for the byte-ordering used to interpret the number
     */
    public abstract short getShort(long offset);

    /**
     * Read four bytes starting at the specified memory offset,
     * interpreting it as a {@code int} using this section's {@link #order() byte ordering}.
     *
     * @param offset the memory offset to read
     * @throws IllegalArgumentException if the specified offset is out of bounds (or there aren't enough bytes)
     * @return the number that has been read
     * @see #getByte(long) for reading an individual byte
     * @see #getLong(long) (long) for reading a long
     * @see #order() for the byte-ordering used to interpret the number
     */
    public abstract int getInt(long offset);

    /**
     * Read eight bytes starting at the specified memory offset,
     * interpreting it as a {@code long} using this section's {@link #order() byte ordering}.
     *
     * @param offset the memory offset to read
     * @throws IllegalArgumentException if the specified offset is out of bounds (or there aren't enough bytes)
     * @return the number that has been read
     * @see #getByte(long) for reading an individual byte
     * @see #getInt(long) (long) (long) for reading an integer
     * @see #order() for the byte-ordering used to interpret the number
     */
    public abstract long getLong(long offset);

    /**
     * Create a view of this section
     * with the specified {@link ByteOrder}.
     *
     * @param order the order to view with
     * @return a new view with a different order, or this object if already the correct order
     */
    public MemorySection withOrder(ByteOrder order) {
        Objects.requireNonNull(order, "Null order");
        if (order == this.order) {
            return this;
        } else {
            assert order == reverseByteOrder(order);
            return reversedOrderSection();
        }
    }

    /**
     * Create a view of this section with a reversed {@link ByteOrder}.
     *
     * @return a view with a reversed order
     */
    protected abstract MemorySection reversedOrderSection();

    /**
     * Create a memory section wrapping the specified array.
     *
     * @param bytes the array to wrap
     * @return a wrapped section
     */
    public static MemorySection ofArray(byte[] bytes) {
        return ofArray(bytes, 0, bytes.length);
    }

    /**
     * Create a memory section wrapping a subsection of the specified array.
     *
     * @param bytes the array to wrap
     * @param startOffset the start of the memory to wrap
     * @param length the amount of memory to wrap
     * @throws IndexOutOfBoundsException if there are not {@code length} bytes after the start offset
     * @return a wrapped section
     */
    public static MemorySection ofArray(byte[] bytes, int startOffset, int length) {
        Objects.checkFromIndexSize(startOffset, length, bytes.length);
        return new HeapMemorySection(bytes, startOffset, length);
    }

    /**
     * Create a section wrapping the specified {@link ByteBuffer},
     * starting at position zero and ending at {@link ByteBuffer#limit()}.
     * <p>
     * This function takes an snapshot of the input buffer's
     * {@link ByteBuffer#order() order} &amp; {@link ByteBuffer#limit() limit}
     * as-if by calling {@link ByteBuffer#duplicate()}.
     * </p>
     *
     * @param buffer the buffer to wrap
     * @return a segment wrapping the specified buffer
     * @see ByteBuffer#slice(int, int) for a way to change the start &amp; end positions.
     */
    public static MemorySection ofBuffer(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return ofArray(buffer.array(), buffer.arrayOffset(), buffer.limit());
        } else {
            // NOTE: Defensive copy to avoid mutating length or order
            return new BufferMemorySection(buffer.duplicate());
        }
    }

    /**
     * Get the reverse of the specified {@link ByteOrder}.
     *
     * @param order the order to get the reverse of
     * @return the reverse order
     */
    public static ByteOrder reverseByteOrder(ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return ByteOrder.LITTLE_ENDIAN;
        } else if (order == ByteOrder.LITTLE_ENDIAN) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            throw new IllegalArgumentException("Bad ByteOrder: " + order);
        }
    }
}
