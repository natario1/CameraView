package com.otaliastudios.cameraview;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.HashMap;


/**
 * A simple class representing an aspect ratio.
 */
public class AspectRatio implements Comparable<AspectRatio> {

    final static HashMap<String, AspectRatio> sCache = new HashMap<>(16);

    /**
     * Creates an aspect ratio with the given values.
     * @param x the width
     * @param y the height
     * @return a (possibly cached) aspect ratio
     */
    public static AspectRatio of(int x, int y) {
        int gcd = gcd(x, y);
        x /= gcd;
        y /= gcd;
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
    public static AspectRatio parse(@NonNull String string) {
        String[] parts = string.split(":");
        if (parts.length != 2) {
            throw new NumberFormatException("Illegal AspectRatio string. Must be x:y");
        }
        int x = Integer.valueOf(parts[0]);
        int y = Integer.valueOf(parts[1]);
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

    public boolean matches(Size size) {
        int gcd = gcd(size.getWidth(), size.getHeight());
        int x = size.getWidth() / gcd;
        int y = size.getHeight() / gcd;
        return mX == x && mY == y;
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
            AspectRatio ratio = (AspectRatio) o;
            return mX == ratio.mX && mY == ratio.mY;
        }
        return false;
    }

    @Override
    public String toString() {
        return mX + ":" + mY;
    }

    public float toFloat() {
        return (float) mX / mY;
    }

    @Override
    public int hashCode() {
        return mY ^ ((mX << (Integer.SIZE / 2)) | (mX >>> (Integer.SIZE / 2)));
    }

    @Override
    public int compareTo(@NonNull AspectRatio another) {
        if (equals(another)) {
            return 0;
        } else if (toFloat() - another.toFloat() > 0) {
            return 1;
        }
        return -1;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public AspectRatio inverse() {
        return AspectRatio.of(mY, mX);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int c = b;
            b = a % b;
            a = c;
        }
        return a;
    }
}