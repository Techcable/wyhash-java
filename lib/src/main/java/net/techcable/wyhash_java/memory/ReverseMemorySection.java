package net.techcable.wyhash_java.memory;

import java.nio.ByteOrder;

public final class ReverseMemorySection extends MemorySection {
    private final MemorySection src;

    public ReverseMemorySection(MemorySection src) {
        super(reverseOrder(src.order()));
        this.src = src;
    }

    @Override
    public long length() {
        return src.length();
    }

    @Override
    protected MemorySection reversedOrderSection() {
        return this.src;
    }

    @Override
    public MemorySection slice(long startIndex, long newSize) {
        return new ReverseMemorySection(src.slice(startIndex, newSize));
    }

    @Override
    public byte getByte(long offset) {
        return src.getByte(offset);
    }

    @Override
    public void getBytes(long offset, byte[] dest, int destOffset, int destLength) {
        src.getBytes(offset, dest, destOffset, destLength);
    }

    @Override
    public short getShort(long offset) {
        return Short.reverseBytes(src.getShort(offset));
    }

    @Override
    public int getInt(long offset) {
        return Integer.reverseBytes(src.getInt(offset));
    }

    @Override
    public long getLong(long offset) {
        return Long.reverseBytes(src.getLong(offset));
    }
}
