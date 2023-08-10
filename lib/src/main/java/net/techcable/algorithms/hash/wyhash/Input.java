// SPDX-License-Identifier: Apache-2.0 WITH LLVM-Exception

package net.techcable.algorithms.hash.wyhash;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * An abstraction around the supported inputs:
 * {@code byte[]},
 * {@link ByteBuffer},
 * and eventually a Panama FFI MemorySegment.
 * <h3>Performance:</h3>
 * <p>
 * This interface should (hopefully) avoid most dispatch overhead,
 * because there are only ever two subclasses in existence at any given time.
 * This means all call-sites are bimorphic, so Hotspot should be willing to inline them.
 * Furthermore, we are careful to
 * <p/>
 */
/* package */ sealed interface Input permits Input.BufferInput, Input.HeapInput {
    static Input ofArray(byte[] array, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, array.length);
        return new HeapInput(array, offset, length);
    }

    static Input ofBuffer(ByteBuffer buffer) {
        return ofBuffer(buffer, 0, buffer.limit());
    }

    static Input ofBuffer(ByteBuffer buffer, int offset, int length) {
        final int limit = buffer.limit(); // implicit null check
        Objects.checkFromIndexSize(offset, length, limit);
        if (buffer.hasArray()) {
            // want to avoid introducing a second class if at all possible
            return ofArray(buffer.array(), buffer.arrayOffset(), limit);
        } else {
            buffer = buffer.duplicate(); // defensive copy
            if (buffer.limit() != limit) throw new ConcurrentModificationException();
            buffer.limit(length);
            return new BufferInput(buffer, offset);
        }
    }

    default long length() {
        return this.intLength();
    }

    int intLength();

    int getInt(int index);

    long getLong(int index);

    byte getByte(int index);

    // long accessors - only need to override if `long` indexes are supported
    default int getIntL(long index) {
        return this.getInt(Math.toIntExact(index));
    }

    default long getLongL(long index) {
        return this.getLong(Math.toIntExact(index));
    }

    default byte getByteL(long index) {
        return this.getByte(Math.toIntExact(index));
    }

    final class HeapInput implements Input {
        private final byte[] data;
        private final int offset, length;

        private HeapInput(byte[] data, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, data.length);
            this.data = data;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public int intLength() {
            return this.length;
        }

        private static final VarHandle ARRAY_READ_INT_HANDLE =
                MethodHandles.byteArrayViewVarHandle(int[].class, WyHash.REQUIRED_BYTE_ORDER);
        private static final VarHandle ARRAY_READ_LONG_HANDLE =
                MethodHandles.byteArrayViewVarHandle(long[].class, WyHash.REQUIRED_BYTE_ORDER);

        @Override
        public int getInt(int index) {
            Objects.checkFromIndexSize(index, 4, this.length);
            return (int) ARRAY_READ_INT_HANDLE.get(this.data, offset + index);
        }

        @Override
        public long getLong(int index) {
            Objects.checkFromIndexSize(index, 8, this.length);
            return (long) ARRAY_READ_LONG_HANDLE.get(this.data, offset + index);
        }

        @Override
        public byte getByte(int index) {
            Objects.checkIndex(index, this.length);
            return this.data[offset + index];
        }
    }

    final class BufferInput implements Input {
        private final ByteBuffer buffer;
        private final int offset;

        private BufferInput(ByteBuffer buffer, int offset) {
            if (offset < 0 || offset > buffer.limit()) throw new IndexOutOfBoundsException(offset);
            this.buffer = buffer; // implicit null check
            this.offset = offset;
            if (buffer.hasArray()) throw new AssertionError("Use ArrayInput");
        }

        private static final VarHandle BUFFER_READ_INT_HANDLE =
                MethodHandles.byteBufferViewVarHandle(int.class, WyHash.REQUIRED_BYTE_ORDER);
        private static final VarHandle BUFFER_READ_LONG_HANDLE =
                MethodHandles.byteBufferViewVarHandle(long.class, WyHash.REQUIRED_BYTE_ORDER);

        @Override
        public int intLength() {
            return this.buffer.limit() - offset;
        }

        @Override
        public int getInt(int index) {
            Objects.checkFromIndexSize(index, 4, this.intLength());
            return (int) BUFFER_READ_INT_HANDLE.get(this.buffer, offset + index);
        }

        @Override
        public long getLong(int index) {
            Objects.checkFromIndexSize(index, 8, this.intLength());
            return (long) BUFFER_READ_LONG_HANDLE.get(this.buffer, offset + index);
        }

        @Override
        public byte getByte(int index) {
            Objects.checkIndex(index, this.intLength());
            return buffer.get(offset + index);
        }
    }
}
