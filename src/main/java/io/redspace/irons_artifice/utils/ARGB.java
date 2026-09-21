package io.redspace.irons_artifice.utils;

import org.joml.Vector3f;

public final class ARGB {
    private ARGB() {
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static Vector3f vector3fFromRGB24(int color) {
        return new Vector3f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
    }
}
