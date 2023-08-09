package net.techcable.wyhash_java.memory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;

public final class HeapMemorySection extends MemorySection {
    private final byte[] bytes;
    private final int startOffset, length;

    @Override
    public ByteOrder order() {
        return BYTE_ORDER;
    }

    /* package */ HeapMemorySection(byte[] bytes, int startOffset, int length) {
        Objects.checkFromIndexSize(startOffset, length, bytes.length);
        this.bytes = bytes;
        this.startOffset = startOffset;
        this.length = length;
        assert this.length + startOffset <= bytes.length;
    }

    @Override
    public long length() {
        return this.length;
    }

    @Override
    public MemorySection slice(long startIndex, long newSize) {
        Objects.checkFromIndexSize(startIndex, newSize, this.length);
        return new HeapMemorySection(
                this.bytes,
                this.startOffset + (int) startIndex,
                (int) newSize
        );
    }

    @Override
    public byte getByte(long offset) {
        Objects.checkIndex(offset, length);
        return this.bytes[this.startOffset + (int) offset];
    }

    @Override
    public void getBytes(long offset, byte[] dest, int destOffset, int destLength) {
        Objects.checkFromIndexSize(offset, destLength, this.length);
        Objects.checkFromIndexSize(destOffset, destLength, dest.length);
        System.arraycopy(
                this.bytes,
                this.startOffset + (int) offset,
                dest,
                destOffset,
                destLength
        );
    }

    @Override
    public short getShort(long offset) {
        Objects.checkFromIndexSize(offset, 2, length);
        return (short) LOAD_SHORT.get(this.bytes, (int) offset + this.startOffset);
    }

    @Override
    public int getInt(long offset) {
        Objects.checkFromIndexSize(offset, 4, length);
        return (int) LOAD_INT.get(this.bytes, (int) offset + this.startOffset);
    }

    @Override
    public long getLong(long offset) {
        Objects.checkFromIndexSize(offset, 8, length);
        return (long) LOAD_LONG.get(this.bytes, (int) offset + this.startOffset);
    }

    private static final ByteOrder BYTE_ORDER = ByteOrder.nativeOrder();

    private static final VarHandle LOAD_SHORT = MethodHandles.byteArrayViewVarHandle(
            int.class,
            BYTE_ORDER
    );
    private static final VarHandle LOAD_INT = MethodHandles.byteArrayViewVarHandle(
            int.class,
            BYTE_ORDER
    );
    private static final VarHandle LOAD_LONG = MethodHandles.byteArrayViewVarHandle(
            int.class,
            BYTE_ORDER
    );
}
