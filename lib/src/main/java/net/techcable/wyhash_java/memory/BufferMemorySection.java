package net.techcable.wyhash_java.memory;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class BufferMemorySection extends MemorySection {
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
        return new BufferMemorySection(this.buffer.duplicate().order(reverseOrder(this.order())));
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
    public void getBytes(long offset, byte[] dest, int destOffset, int destLength) {
        Objects.checkFromToIndex(offset, destLength, this.length());
        this.buffer.get((int) offset, dest, destOffset, destLength);
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
}
