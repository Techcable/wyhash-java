package net.techcable.algorithms.hash.wyhash.memory;

/* package */ final class ReverseMemorySection extends MemorySection {
    private final MemorySection src;

    public ReverseMemorySection(MemorySection src) {
        super(reverseByteOrder(src.order()));
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
    public void getBytes(long offset, byte[] dest, int destOffset, int length) {
        src.getBytes(offset, dest, destOffset, length);
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
