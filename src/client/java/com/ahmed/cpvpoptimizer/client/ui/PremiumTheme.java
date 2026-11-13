package com.ahmed.cpvpoptimizer.client.ui;

/**
 * Centralized CSS-like Design Tokens for the Premium Dark Liquid-Glass / Frosted-Glass UI.
 * Derived directly from the 6 visual reference screenshots.
 */
public final class PremiumTheme {
    private PremiumTheme() {}

    // Backdrop & Atmospheric Tokens
    public static final int BG_DARKEN_TOP = 0xB8040507;
    public static final int BG_DARKEN_BOTTOM = 0xD4050608;

    // Multi-Layer Glass Surface Tokens
    public static final int GLASS_PRIMARY = 0xC80C0E10;      // rgba(12, 14, 16, 0.78)
    public static final int GLASS_SECONDARY = 0xA8121417;    // rgba(18, 20, 23, 0.66)
    public static final int GLASS_ELEVATED = 0x94191C20;     // rgba(25, 28, 32, 0.58)
    public static final int GLASS_HOVER = 0xB822262C;        // Hover elevated tint
    public static final int GLASS_SELECTED = 0xD41E2229;     // Active/Selected tile surface

    // Border & Inner Specular Highlight Tokens
    public static final int BORDER_SUBTLE = 0x16FFFFFF;      // rgba(255, 255, 255, 0.085)
    public static final int BORDER_HOVER = 0x30FFFFFF;       // rgba(255, 255, 255, 0.19)
    public static final int BORDER_SELECTED = 0x58FFFFFF;    // rgba(255, 255, 255, 0.35)
    public static final int INNER_HIGHLIGHT = 0x0CFFFFFF;    // rgba(255, 255, 255, 0.047)
    public static final int SHADOW_SOFT = 0x55000000;        // Soft drop shadow
    public static final int SHADOW_DEEP = 0x78000000;

    // Typography Tokens
    public static final int TEXT_PRIMARY = 0xFFF2F3F4;       // Crisp off-white
    public static final int TEXT_SECONDARY = 0xFFA7A9AD;     // Soft silver-gray
    public static final int TEXT_MUTED = 0xFF6E7278;         // Muted slate
    public static final int ACCENT_WHITE = 0xFFFFFFFF;       // Pure luminous white accent
    public static final int ACCENT_GREEN = 0xFF4ADE80;       // Subtle telemetry positive indicator

    // Spacing & Geometry Grid Tokens (4px discipline)
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_20 = 20;
    public static final int SPACE_24 = 24;

    public static final int ROW_HEIGHT = 22;
    public static final int ROW_GAP = 4;

    public static int lerpColor(float t, int c1, int c2) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int rgb, float alphaRatio) {
        int baseAlpha = (rgb >>> 24) & 0xFF;
        int a = Math.max(0, Math.min(255, Math.round(baseAlpha * alphaRatio)));
        return (rgb & 0x00FFFFFF) | (a << 24);
    }
}
