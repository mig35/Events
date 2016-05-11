package com.azoft.events;

import java.util.Arrays;

class Objects {

    /**
     * Returns true if both arguments are null,
     * the result of {@link Arrays#equals} if both arguments are primitive arrays,
     * the result of {@link Arrays#deepEquals} if both arguments are arrays of reference types,
     * and the result of {@link #equals} otherwise.
     */
    public static boolean deepEquals(final Object a, final Object b) {
        if (null == a || null == b) {
            //noinspection ObjectEquality
            return a == b;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        }
        if (a instanceof boolean[] && b instanceof boolean[]) {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof char[] && b instanceof char[]) {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        if (a instanceof double[] && b instanceof double[]) {
            return Arrays.equals((double[]) a, (double[]) b);
        }
        if (a instanceof float[] && b instanceof float[]) {
            return Arrays.equals((float[]) a, (float[]) b);
        }
        if (a instanceof int[] && b instanceof int[]) {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if (a instanceof long[] && b instanceof long[]) {
            return Arrays.equals((long[]) a, (long[]) b);
        }
        if (a instanceof short[] && b instanceof short[]) {
            return Arrays.equals((short[]) a, (short[]) b);
        }
        return a.equals(b);
    }

    /**
     * Null-safe equivalent of {@code a.equals(b)}.
     */
    public static boolean equals(final Object a, final Object b) {
        return null == a ? null == b : a.equals(b);
    }

    public static boolean equalsTargetIds(final String aTargetId, final String bTargetId) {
        if (null == aTargetId || null == bTargetId) {
            return false;
        }
        return aTargetId.equals(bTargetId);
    }

    public static boolean equalsTargets(final Object aTarget, final Object bTarget) {
        if (null == aTarget || null == bTarget) {
            return false;
        }
        return aTarget.equals(bTarget);
    }
}