// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/* package */ final class BufferMemorySection extends MemorySection {
    private final ByteBuffer buffer;
    /* package */ BufferMemorySection(ByteBuffer buffer) {
        super(buffer.order());
        this.buffer = Objects.requireNonNull(buffer, "Null buffer");
        if (buffer.hasArray()) throw new IllegalArgumentException("Should prefer HeapMemorySection");
    }

    @Override
    public long length() {
        return this.buffer.limit();
    }

    @Override
    protected MemorySection reversedOrderSection() {
        return new BufferMemorySection(this.buffer.duplicate().order(reverseByteOrder(this.order())));
    }

    @Override
    public MemorySection slice(long startIndex, long newSize) {
        Objects.checkFromIndexSize(startIndex, newSize, this.length());
        return new BufferMemorySection(this.buffer.slice((int) startIndex, (int) newSize));
    }

    @Override
    public byte getByte(long offset) {
        Objects.checkIndex(offset, length());
        return buffer.get((int) offset);
    }

    @Override
    public void getBytes(long offset, byte[] dest, int destOffset, int length) {
        Objects.checkFromToIndex(offset, length, this.length());
        this.buffer.get((int) offset, dest, destOffset, length);
    }

    @Override
    public short getShort(long offset) {
        Objects.checkIndex(offset, length());
        return buffer.getShort((int) offset);
    }

    @Override
    public int getInt(long offset) {
        Objects.checkIndex(offset, length());
        return buffer.getInt((int) offset);
    }

    @Override
    public long getLong(long offset) {
        Objects.checkIndex(offset, length());
        return buffer.getLong((int) offset);
    }

    @Override
    public MemorySection withOrder(ByteOrder order) {
        return null;
    }
}
