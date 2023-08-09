package net.techcable.wyhash_java;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Wraps a string that has been {@link String#intern() interned} by the JVM.
 */
@SuppressWarnings("StringEquality")
public final class JvmInternedString implements Comparable<JvmInternedString> {
    private final String stringValue;
    private JvmInternedString(String stringValue) {
        Objects.requireNonNull(stringValue, "Null string");
        this.stringValue = stringValue.intern();
    }

    /**
     * Create an interned string with the specified text.
     * <p>
     * This will implicitly run {@link String#intern()} if not already done.
     *
     * @param text the value of the string
     * @throws NullPointerException if the string is null
     * @return an interned string
     */
    public static JvmInternedString of(String text) {
        return new JvmInternedString(text);
    }

    /**
     * Assume that the specified text is interned, converting it into a {@link JvmInternedString}.
     * <p>
     * This verifies at runtime that the text is interned,
     * and throws an exception if it is not.
     *
     * @param stringObj the string object that is assumed to be interned.
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the string has not already been interned
     * @return the interned string wrapper object
     */
    public static JvmInternedString assumeInterned(String stringObj) {
        var res = new JvmInternedString(stringObj);
        if (res.stringValue != stringObj) {
            throw new IllegalArgumentException("Not actually interned: " + stringObj);
        }
        return res;
    }

    /**
     * Return the value of the underlying (interned) string.
     * <p/>
     * The resulting object is guaranteed to be interned by {@link String#intern()}.
     *
     * @return the interned string object
     */
    public String stringValue() {
        return this.stringValue;
    }

    /**
     * Check if this string equals the specified other value.
     *
     * @param other the value to compare against
     * @return true if equal
     */
    public boolean equalsString(String other) {
        return this.stringValue.equals(other);
    }

    /**
     * Check if this object is equal
     * <p>
     * This only returns true if the other object is a {@link JvmInternedString},
     * and <em>always returns false for {@link String} arguments</em>.
     * <p>
     * This should be significantly faster than {@link String#equals(Object)},
     * because it only needs to compare object identity.
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof JvmInternedString interned &&
                interned.stringValue == this.stringValue;
    }

    /**
     * Return the hash code of the string object.
     * <p/>
     * Behaves identically to {@link String#hashCode()}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.stringValue.hashCode();
    }

    /**
     * Convert this wrapper to an interned string object.
     * <p>
     * Returns the same underlying string object as {@link #stringValue()}.
     *
     * @return the interned string value
     */
    @Override
    public String toString() {
        return this.stringValue;
    }

    @Override
    public int compareTo(@NotNull JvmInternedString o) {
        return this.stringValue == o.stringValue ? 0 : this.stringValue.compareTo(o.stringValue);
    }
}
