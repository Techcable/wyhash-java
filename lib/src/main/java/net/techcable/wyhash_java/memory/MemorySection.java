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

    protected MemorySection(ByteOrder order) {
        this.order = Objects.requireNonNull(order);
    }

    public abstract long length();

    public final ByteOrder order() {
        return this.order;
    }

    public abstract MemorySection slice(long startIndex, long newSize);

    public abstract byte getByte(long offset);

    public void getBytes(long offset, byte[] dest) {
        this.getBytes(offset, dest, 0, dest.length);
    }

    public abstract void getBytes(long offset, byte[] dest, int destOffset, int destLength);

    public abstract short getShort(long offset);

    public abstract int getInt(long offset);

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
            assert order == reverseOrder(order);
            return reversedOrderSection();
        }
    }

    protected abstract MemorySection reversedOrderSection();

    public static MemorySection ofArray(byte[] bytes) {
        return ofArray(bytes, 0, bytes.length);
    }

    public static MemorySection ofArray(byte[] bytes, int startOffset, int length) {
        Objects.checkFromIndexSize(startOffset, length, bytes.length);
        return new HeapMemorySection(bytes, startOffset, length);
    }

    public static MemorySection ofBuffer(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return ofArray(buffer.array(), buffer.arrayOffset(), buffer.limit());
        } else {
            return new BufferMemorySection(buffer);
        }
    }

    /**
     * Get the reverse of the specified {@link ByteOrder}.
     *
     * @param order the order to get the reverse of
     * @return the reverse order
     */
    public static ByteOrder reverseOrder(ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return ByteOrder.LITTLE_ENDIAN;
        } else if (order == ByteOrder.LITTLE_ENDIAN) {
            return ByteOrder.BIG_ENDIAN;
        } else {
            throw new IllegalArgumentException("Bad ByteOrder: " + order);
        }
    }
}
