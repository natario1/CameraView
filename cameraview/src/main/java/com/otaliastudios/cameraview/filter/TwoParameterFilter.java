package com.otaliastudios.cameraview.filter;

/**
 * A special {@link Filter} that accepts two floats parameters.
 * This is done by extending {@link OneParameterFilter}.
 *
 * The parameters will always be between 0F and 1F, so subclasses should
 * map this range to their internal range if needed.
 *
 * A standardized range is useful for different applications. For example:
 * - Filter parameters can be easily mapped to gestures since the range is fixed
 * - {@link BaseFilter} can use this setters and getters to make a filter copy
 */
public interface TwoParameterFilter extends OneParameterFilter {

    /**
     * Sets the second parameter.
     * The value should always be between 0 and 1.
     *
     * @param value parameter
     */
    void setParameter2(float value);

    /**
     * Returns the second parameter.
     * The returned value should always be between 0 and 1.
     *
     * @return parameter
     */
    float getParameter2();
}
