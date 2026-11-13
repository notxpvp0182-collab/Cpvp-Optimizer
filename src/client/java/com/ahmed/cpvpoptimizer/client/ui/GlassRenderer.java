package com.ahmed.cpvpoptimizer.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;

import java.lang.reflect.Method;

/**
 * Reusable Frosted-Glass / Liquid-Glass Rendering Engine.
 * Provides chamfered rounded glass panels, soft drop shadows, 1px luminous borders,
 * top specular highlights, monochrome pill switches, and safe native framebuffer blur.
 */
public final class GlassRenderer {
    private static boolean blurLookupDone = false;
    private static Method blurZeroArg = null;
    private static Method blurFloatArg = null;

    private GlassRenderer() {}

    /**
     * Triggers Minecraft's native Gaussian blur post-processing pass when in-world,
     * followed by the dark atmospheric vignette from Screenshots 1-6.
     */
    public static void renderFrostedBackdrop(DrawContext ctx, int width, int height, float openProgress) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null && client.gameRenderer != null) {
            invokeSafeNativeBlur(client.gameRenderer);
        }

        int topColor = PremiumTheme.withAlpha(PremiumTheme.BG_DARKEN_TOP, openProgress);
        int bottomColor = PremiumTheme.withAlpha(PremiumTheme.BG_DARKEN_BOTTOM, openProgress);
        ctx.fillGradient(0, 0, width, height, topColor, bottomColor);

        // Subtle top and bottom cinematic vignette bands
        int vigAlpha = Math.round(70 * openProgress) << 24;
        ctx.fillGradient(0, 0, width, Math.min(48, height / 5), vigAlpha, 0x00000000);
        ctx.fillGradient(0, Math.max(0, height - Math.min(48, height / 5)), width, height, 0x00000000, vigAlpha);
    }

    private static void invokeSafeNativeBlur(GameRenderer renderer) {
        try {
            if (!blurLookupDone) {
                blurLookupDone = true;
                for (Method m : GameRenderer.class.getDeclaredMethods()) {
                    if (m.getName().contains("renderBlur") || m.getName().contains("blur")) {
                        if (m.getParameterCount() == 0) {
                            m.setAccessible(true);
                            blurZeroArg = m;
                            break;
                        } else if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == float.class) {
                            m.setAccessible(true);
                            blurFloatArg = m;
                            break;
                        }
                    }
                }
            }
            if (blurZeroArg != null) {
                blurZeroArg.invoke(renderer);
            } else if (blurFloatArg != null) {
                blurFloatArg.invoke(renderer, 1.0f);
            }
        } catch (Throwable ignored) {
            // Safe fallback to translucent dark glass if shader is busy in current render pass
        }
    }

    /**
     * Draws a chamfered rounded frosted-glass container with outer soft shadow,
     * translucent dark fill, subtle 1px border, and upper inner specular highlight.
     */
    public static void drawGlassPanel(DrawContext ctx, int x, int y, int w, int h,
                                      int fillColor, int borderColor, boolean drawShadow) {
        if (w <= 4 || h <= 4) return;

        // 1. Soft Outer Shadow
        if (drawShadow) {
            ctx.fill(x - 1, y + 1, x + w + 1, y + h + 2, PremiumTheme.SHADOW_SOFT);
            ctx.fill(x + 1, y + h, x + w - 1, y + h + 3, PremiumTheme.SHADOW_DEEP);
        }

        // 2. Chamfered Rounded Glass Fill (1px corner cut for clean anti-aliased rounded look)
        ctx.fill(x + 1, y, x + w - 1, y + 1, fillColor);
        ctx.fill(x, y + 1, x + w, y + h - 1, fillColor);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, fillColor);

        // 3. Inner Top Specular Highlight (gives genuine frosted glass depth)
        ctx.fill(x + 2, y + 1, x + w - 2, y + 2, PremiumTheme.INNER_HIGHLIGHT);

        // 4. Thin Luminous 1px Rounded Border
        ctx.fill(x + 1, y, x + w - 1, y + 1, borderColor);             // Top edge
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, borderColor);     // Bottom edge
        ctx.fill(x, y + 1, x + 1, y + h - 1, borderColor);             // Left edge
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, borderColor);     // Right edge
    }

    /**
     * Draws the sleek monochrome frosted pill switch from Screenshot 4.
     */
    public static void drawFrostedSwitch(DrawContext ctx, int x, int y, int w, int h, float animProgress) {
        int trackFill = PremiumTheme.lerpColor(animProgress, 0xAA0A0C0E, 0xCC3A3F47);
        int trackBorder = PremiumTheme.lerpColor(animProgress, 0x22FFFFFF, 0x66FFFFFF);

        drawGlassPanel(ctx, x, y, w, h, trackFill, trackBorder, false);

        int knobSize = h - 4;
        int minX = x + 2;
        int maxX = x + w - knobSize - 2;
        int knobX = minX + Math.round(animProgress * (maxX - minX));
        int knobY = y + 2;

        int knobColor = PremiumTheme.lerpColor(animProgress, 0xFF7A7F87, 0xFFFFFFFF);
        ctx.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knobColor);
    }

    /**
     * Draws a sleek monochrome horizontal progress bar (Screenshot 6 style).
     */
    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        ctx.fill(x, y, x + w, y + h, 0x88090B0E);
        ctx.fill(x, y, x + w, y + 1, 0x18FFFFFF);
        int fillW = Math.round(w * ratio);
        if (fillW > 0) {
            ctx.fill(x, y, x + fillW, y + h, PremiumTheme.TEXT_PRIMARY);
        }
    }
}
