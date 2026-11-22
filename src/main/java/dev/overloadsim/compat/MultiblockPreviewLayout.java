package dev.overloadsim.compat;

/** Geometry shared by the optional JEI compatibility patch and its regression tests. */
public final class MultiblockPreviewLayout {
    private MultiblockPreviewLayout() {}

    public static int fitHeight(int availableHeight, int padding) {
        // 31px toolbar/header, 17px footer, tabs and at least one material row.
        return Math.max(96, availableHeight - 2 * (4 + Math.max(0, padding)));
    }

    public static int clampScroll(int scroll, int viewportHeight, int contentHeight) {
        return Math.clamp(scroll, 0, Math.max(0, contentHeight - viewportHeight));
    }

    public static int contentY(int top, int offset, int scroll) {
        return top + offset - scroll;
    }

    public static boolean contains(int top, int bottom, double y) {
        return y >= top && y < bottom;
    }

    public static float frameZoom(float originalZoom, int sizeX, int sizeY, int sizeZ, int width, int height) {
        double radius = Math.sqrt((double) sizeX * sizeX + (double) sizeY * sizeY + (double) sizeZ * sizeZ) / 2;
        // LDLib's orthographic Y extents are divided by the viewport aspect ratio.
        // A bounding sphere keeps the whole structure framed throughout auto-rotation.
        double aspect = Math.max(1, (double) width / Math.max(1, height));
        return Math.max(originalZoom, (float) (radius * aspect * 1.1));
    }
}
