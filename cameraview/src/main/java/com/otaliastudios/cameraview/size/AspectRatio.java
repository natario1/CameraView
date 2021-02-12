package com.otaliastudios.cameraview.size;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;


/**
 * A simple class representing an aspect ratio.
 */
public class AspectRatio implements Comparable<AspectRatio> {

    @VisibleForTesting final static HashMap<String, AspectRatio> sCache
            = new HashMap<>(16);

    /**
     * Creates an aspect ratio for the given size.
     * @param size the size
     * @return a (possibly cached) aspect ratio
     */
    @NonNull
    public static AspectRatio of(@NonNull Size size) {
        return AspectRatio.of(size.getWidth(), size.getHeight());
    }

    /**
     * Creates an aspect ratio with the given values.
     * @param x the width
     * @param y the height
     * @return a (possibly cached) aspect ratio
     */
    @NonNull
    public static AspectRatio of(int x, int y) {
        int gcd = gcd(x, y);
        if (gcd > 0) x /= gcd;
        if (gcd > 0) y /= gcd;
        String key = x + ":" + y;
        AspectRatio cached = sCache.get(key);
        if (cached == null) {
            cached = new AspectRatio(x, y);
            sCache.put(key, cached);
        }
        return cached;
    }

    /**
     * Parses an aspect ratio string, for example those previously obtained
     * with {@link #toString()}.
     *
     * @param string a string of the format x:y where x and y are integers
     * @return a (possibly cached) aspect ratio
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static AspectRatio parse(@NonNull String string) {
        String[] parts = string.split(":");
        if (parts.length != 2) {
            throw new NumberFormatException("Illegal AspectRatio string. Must be x:y");
        }
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        return of(x, y);
    }

    private final int mX;
    private final int mY;

    private AspectRatio(int x, int y) {
        mX = x;
        mY = y;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public boolean matches(@NonNull Size size) {
        return equals(AspectRatio.of(size));
    }

    public boolean matches(@NonNull Size size, float tolerance) {
        return Math.abs(toFloat() - AspectRatio.of(size).toFloat()) <= tolerance;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof AspectRatio) {
            return toFloat() == ((AspectRatio) o).toFloat();
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return mX + ":" + mY;
    }

    public float toFloat() {
        return (float) mX / mY;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(toFloat());
    }

    @Override
    public int compareTo(@NonNull AspectRatio another) {
        return Float.compare(toFloat(), another.toFloat());
    }

    /**
     * Returns a flipped aspect ratio, which means inverting its dimensions.
     * @return a flipped aspect ratio
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @NonNull
    public AspectRatio flip() {
        return AspectRatio.of(mY, mX);
    }

    // Note: gcd(0,X) = gcd(X,0) = X (even for X=0)
    private static int gcd(int a, int b) {
        while (b != 0) {
            int c = b;
            b = a % b;
            a = c;
        }
        return a;
    }
}