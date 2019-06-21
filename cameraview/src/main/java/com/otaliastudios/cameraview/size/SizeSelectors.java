package com.otaliastudios.cameraview.size;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static utilities to create, join and merge {@link SizeSelector}s instances.
 */
public class SizeSelectors {

    /**
     * A size constraint to easily filter out
     * sizes in a list.
     */
    public interface Filter {
        boolean accepts(@NonNull Size size);
    }

    /**
     * Returns a new {@link SizeSelector} with the given {@link Filter}.
     * This kind of selector will respect the order in the source array.
     *
     * @param filter a filter
     * @return a new selector
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static SizeSelector withFilter(@NonNull Filter filter) {
        return new FilterSelector(filter);
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * whose width is at most equal to the given width.
     *
     * @param width the max width
     * @return a new selector
     */
    @NonNull
    public static SizeSelector maxWidth(final int width) {
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getWidth() <= width;
            }
        });
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * whose width is at least equal to the given width.
     *
     * @param width the min width
     * @return a new selector
     */
    @NonNull
    public static SizeSelector minWidth(final int width) {
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getWidth() >= width;
            }
        });
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * whose height is at most equal to the given height.
     *
     * @param height the max height
     * @return a new selector
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static SizeSelector maxHeight(final int height) {
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getHeight() <= height;
            }
        });
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * whose height is at least equal to the given height.
     *
     * @param height the min height
     * @return a new selector
     */
    @NonNull
    public static SizeSelector minHeight(final int height) {
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getHeight() >= height;
            }
        });
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * which respect the given {@link AspectRatio}. You can pass a tolerance
     * value to include aspect ratios that are slightly different.
     *
     * @param ratio the desired aspect ratio
     * @param delta a small tolerance value
     * @return a new selector
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static SizeSelector aspectRatio(AspectRatio ratio, final float delta) {
        final float desired = ratio.toFloat();
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                float candidate = AspectRatio.of(size.getWidth(), size.getHeight()).toFloat();
                return candidate >= desired - delta && candidate <= desired + delta;
            }
        });
    }

    /**
     * Returns a {@link SizeSelector} that will order sizes from
     * the biggest to the smallest. This means that the biggest size will be taken.
     *
     * @return a new selector
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static SizeSelector biggest() {
        return new SizeSelector() {
            @NonNull
            @Override
            public List<Size> select(@NonNull List<Size> source) {
                Collections.sort(source);
                Collections.reverse(source);
                return source;
            }
        };
    }

    /**
     * Returns a {@link SizeSelector} that will order sizes from
     * the smallest to the biggest. This means that the smallest size will be taken.
     *
     * @return a new selector
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static SizeSelector smallest() {
        return new SizeSelector() {
            @NonNull
            @Override
            public List<Size> select(@NonNull List<Size> source) {
                Collections.sort(source);
                return source;
            }
        };
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * whose area is at most equal to the given area in pixels.
     *
     * @param area the max area
     * @return a new selector
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public static SizeSelector maxArea(final int area) {
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getHeight() * size.getWidth() <= area;
            }
        });
    }

    /**
     * Returns a new {@link SizeSelector} that keeps only sizes
     * whose area is at least equal to the given area in pixels.
     *
     * @param area the min area
     * @return a new selector
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static SizeSelector minArea(final int area) {
        return withFilter(new Filter() {
            @Override
            public boolean accepts(@NonNull Size size) {
                return size.getHeight() * size.getWidth() >= area;
            }
        });
    }

    /**
     * Joins all the given selectors to create a new one that returns
     * the intersection of all the inputs. Basically, all constraints are
     * respected.
     *
     * Keep in mind there is good chance that the final list will be empty.
     *
     * @param selectors input selectors
     * @return a new selector
     */
    @NonNull
    public static SizeSelector and(SizeSelector... selectors) {
        return new AndSelector(selectors);
    }

    /**
     * Creates a new {@link SizeSelector} that 'or's the given filters.
     * If the first selector returns an empty list, the next selector is queried,
     * and so on until a non-empty list is found.
     *
     * @param selectors input selectors
     * @return a new selector
     */
    @NonNull
    public static SizeSelector or(SizeSelector... selectors) {
        return new OrSelector(selectors);
    }


    //region private utilities

    private static class FilterSelector implements SizeSelector {

        private Filter constraint;

        private FilterSelector(@NonNull Filter constraint) {
            this.constraint = constraint;
        }

        @Override
        @NonNull
        public List<Size> select(@NonNull List<Size> source) {
            List<Size> sizes = new ArrayList<>();
            for (Size size : source) {
                if (constraint.accepts(size)) {
                    sizes.add(size);
                }
            }
            return sizes;
        }
    }

    private static class AndSelector implements SizeSelector {

        private SizeSelector[] values;

        private AndSelector(@NonNull SizeSelector... values) {
            this.values = values;
        }

        @Override
        @NonNull
        public List<Size> select(@NonNull List<Size> source) {
            List<Size> temp = source;
            for (SizeSelector selector : values) {
                temp = selector.select(temp);
            }
            return temp;
        }
    }

    private static class OrSelector implements SizeSelector {

        private SizeSelector[] values;

        private OrSelector(@NonNull SizeSelector... values) {
            this.values = values;
        }

        @Override
        @NonNull
        public List<Size> select(@NonNull List<Size> source) {
            List<Size> temp = null;
            for (SizeSelector selector : values) {
                temp = selector.select(source);
                if (!temp.isEmpty()) {
                    break;
                }
            }
            return temp == null ? new ArrayList<Size>() : temp;
        }

    }
    //endregion
}
