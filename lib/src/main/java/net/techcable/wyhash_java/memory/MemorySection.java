package net.techcable.wyhash_java.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Backport API for {@code java.lang.foreign.MemorySegment}.
 */
public abstract sealed class MemorySection permits HeapMemorySection, BufferMemorySection {
    public abstract long length();

    public abstract ByteOrder order();

    public abstract MemorySection slice(long startIndex, long newSize);

    public abstract byte getByte(long offset);

    public void getBytes(long offset, byte[] dest) {
        this.getBytes(offset, dest, 0, dest.length);
    }

    public abstract void getBytes(long offset, byte[] dest, int destOffset, int destLength);

    public abstract short getShort(long offset);

    public abstract int getInt(long offset);

    public abstract long getLong(long offset);

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
}
