package com.otaliastudios.cameraview;

import android.support.annotation.NonNull;

/**
 * A simple class representing a size, with width and height values.
 */
public class Size implements Comparable<Size> {

    private int mWidth;
    private int mHeight;

    Size(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /**
     * Flips width and height altogether.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void flip() {
        int temp = mWidth;
        mWidth = mHeight;
        mHeight = temp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof Size) {
            Size size = (Size) o;
            return mWidth == size.mWidth && mHeight == size.mHeight;
        }
        return false;
    }

    @Override
    public String toString() {
        return mWidth + "x" + mHeight;
    }

    @Override
    public int hashCode() {
        return mHeight ^ ((mWidth << (Integer.SIZE / 2)) | (mWidth >>> (Integer.SIZE / 2)));
    }

    @Override
    public int compareTo(@NonNull Size another) {
        return mWidth * mHeight - another.mWidth * another.mHeight;
    }

}
