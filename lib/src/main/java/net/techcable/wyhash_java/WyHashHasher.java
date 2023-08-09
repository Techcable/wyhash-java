package net.techcable.wyhash_java;

import net.techcable.wyhash_java.memory.MemorySection;

import java.nio.ByteBuffer;

public class WyHashHasher implements Hasher {
    private static final String ALGORITHM_NAME = "wyhash4"
    private static final JvmInternedString ALGORITHM_NAME_INTERNED = JvmInternedString.asssumeInterned("")

    @Override
    public JvmInternedString getAlgorithmNameInterned() {
        return ALGORITHM_NAME_INTERNED;
    }

    @Override
    public void hashBytes(byte[] array, int offset, int length) {
    }

    @Override
    public void hashDirectBytes(ByteBuffer buf, int start, int end) {

    }

    @Override
    public void hashByte(byte b) {

    }

    @Override
    public void hashInt(int val) {

    }

    @Override
    public void hashLong(long val) {

    }

    @Override
    public int finishInt() {
        return 0;
    }

    @Override
    public long finishLong() {
        return 0;
    }

    //
    // Implementation: Ported from C
    //


    private static long wyRotate(long x) {
        return Long.rotateRight(x, 32);
    }

    private record Int128(long low, long high) {
        // _wymum
        public Int128 multiplySelf() {
            return new Int128(
                    low * high,
                    Math.multiplyHigh(low, high)
            );
        }

        // _wymix
        public long multiplySelfXor() {
            var res = this.multiplySelf();
            return res.low ^ res.high;
        }
    }

    private long wyr(MemorySection section,) {

    }
}
