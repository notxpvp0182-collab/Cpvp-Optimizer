package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import net.minecraft.client.gui.DrawContext;

/**
 * Minimalist, thin monochrome icon family matching Screenshots 2, 3, 4, 5, and 6.
 */
public final class CustomModuleIcons {
    private CustomModuleIcons() {}

    /**
     * Draws the centered 16x16 Category Tile Icon (Screenshots 2, 3, 4).
     */
    public static void drawCategoryIcon(DrawContext ctx, ModuleCategory category, int x, int y, int color) {
        switch (category) {
            case COMBAT -> {
                // Shield with checkmark (exact match to "PvP" tile in Screenshots 2 & 3)
                ctx.fill(x + 3, y + 1, x + 13, y + 2, color);
                ctx.fill(x + 2, y + 2, x + 3, y + 9, color);
                ctx.fill(x + 13, y + 2, x + 14, y + 9, color);
                ctx.fill(x + 3, y + 9, x + 5, y + 12, color);
                ctx.fill(x + 11, y + 9, x + 13, y + 12, color);
                ctx.fill(x + 5, y + 12, x + 8, y + 14, color);
                ctx.fill(x + 8, y + 12, x + 11, y + 14, color);
                // Inner check
                ctx.fill(x + 5, y + 7, x + 7, y + 9, color);
                ctx.fill(x + 7, y + 8, x + 9, y + 10, color);
                ctx.fill(x + 9, y + 5, x + 11, y + 8, color);
            }
            case INTERACTION -> {
                // Double Fast-Forward Chevrons ">>" (exact match to "Player" tile in Screenshots 2 & 3)
                for (int i = 0; i < 4; i++) {
                    ctx.fill(x + 3 + i, y + 4 + i, x + 5 + i, y + 5 + i, color);
                    ctx.fill(x + 3 + i, y + 11 - i, x + 5 + i, y + 12 - i, color);
                    ctx.fill(x + 8 + i, y + 4 + i, x + 10 + i, y + 5 + i, color);
                    ctx.fill(x + 8 + i, y + 11 - i, x + 10 + i, y + 12 - i, color);
                }
            }
            case PERFORMANCE -> {
                // Magic Wand + Sparkles (exact match to "Visuals" tile in Screenshots 2 & 3)
                for (int i = 0; i < 9; i++) {
                    ctx.fill(x + 3 + i, y + 12 - i, x + 4 + i, y + 13 - i, color);
                }
                ctx.fill(x + 6, y + 3, x + 7, y + 5, color);
                ctx.fill(x + 5, y + 4, x + 8, y + 4, color);
                ctx.fill(x + 12, y + 7, x + 14, y + 8, color);
                ctx.fill(x + 13, y + 6, x + 14, y + 9, color);
            }
            case NETWORK -> {
                // Rounded HUD / Monitor Box with lines (exact match to "HUD" tile in Screenshots 2 & 3)
                ctx.fill(x + 3, y + 3, x + 13, y + 4, color);
                ctx.fill(x + 3, y + 12, x + 13, y + 13, color);
                ctx.fill(x + 2, y + 4, x + 3, y + 12, color);
                ctx.fill(x + 13, y + 4, x + 14, y + 12, color);
                ctx.fill(x + 5, y + 6, x + 11, y + 7, color);
                ctx.fill(x + 5, y + 9, x + 9, y + 10, color);
            }
            case SERVER -> {
                // Three Horizontal Sliders / Bars (exact match to "Misc" tile in Screenshots 2 & 3)
                ctx.fill(x + 3, y + 4, x + 13, y + 5, color);
                ctx.fill(x + 9, y + 3, x + 11, y + 6, color);

                ctx.fill(x + 3, y + 8, x + 13, y + 9, color);
                ctx.fill(x + 5, y + 7, x + 7, y + 10, color);

                ctx.fill(x + 3, y + 12, x + 13, y + 13, color);
                ctx.fill(x + 8, y + 11, x + 10, y + 14, color);
            }
        }
    }

    /**
     * Draws a compact 10x10 monochrome module row icon (Screenshot 4 style).
     */
    public static void drawMiniModuleIcon(DrawContext ctx, String moduleId, int x, int y, int color) {
        switch (moduleId) {
            case ModuleId.CRYSTAL_OPTIMIZER -> {
                ctx.fill(x + 4, y + 1, x + 6, y + 3, color);
                ctx.fill(x + 2, y + 3, x + 8, y + 7, color);
                ctx.fill(x + 3, y + 4, x + 7, y + 6, 0xFF14161A);
                ctx.fill(x + 2, y + 8, x + 8, y + 9, color);
            }
            case ModuleId.ANCHOR_OPTIMIZER -> {
                ctx.fill(x + 2, y + 2, x + 8, y + 8, color);
                ctx.fill(x + 3, y + 3, x + 7, y + 6, 0xFF14161A);
                ctx.fill(x + 4, y + 4, x + 6, y + 5, color);
            }
            case ModuleId.GLOWSTONE_OPTIMIZER -> {
                ctx.fill(x + 4, y + 4, x + 6, y + 6, color);
                ctx.fill(x + 4, y + 1, x + 6, y + 3, color);
                ctx.fill(x + 4, y + 7, x + 6, y + 9, color);
                ctx.fill(x + 1, y + 4, x + 3, y + 6, color);
                ctx.fill(x + 7, y + 4, x + 9, y + 6, color);
            }
            case ModuleId.TOTEM_OPTIMIZER -> {
                ctx.fill(x + 2, y + 2, x + 8, y + 4, color);
                ctx.fill(x + 3, y + 4, x + 7, y + 8, color);
                ctx.fill(x + 4, y + 8, x + 6, y + 9, color);
            }
            case ModuleId.XP_OPTIMIZER -> {
                ctx.fill(x + 4, y + 1, x + 6, y + 3, color);
                ctx.fill(x + 2, y + 3, x + 8, y + 8, color);
                ctx.fill(x + 3, y + 4, x + 7, y + 7, 0xFF14161A);
            }
            case ModuleId.BLOCK_PLACEMENT_OPTIMIZER -> {
                ctx.fill(x + 1, y + 1, x + 4, y + 2, color);
                ctx.fill(x + 1, y + 1, x + 2, y + 4, color);
                ctx.fill(x + 6, y + 1, x + 9, y + 2, color);
                ctx.fill(x + 8, y + 1, x + 9, y + 4, color);
                ctx.fill(x + 1, y + 8, x + 4, y + 9, color);
                ctx.fill(x + 6, y + 8, x + 9, y + 9, color);
                ctx.fill(x + 4, y + 4, x + 6, y + 6, color);
            }
            case ModuleId.HOTBAR_OPTIMIZER, ModuleId.INPUT_OPTIMIZER -> {
                ctx.fill(x + 1, y + 2, x + 9, y + 8, color);
                ctx.fill(x + 2, y + 3, x + 8, y + 7, 0xFF14161A);
                ctx.fill(x + 3, y + 4, x + 4, y + 6, color);
                ctx.fill(x + 6, y + 4, x + 7, y + 6, color);
            }
            case ModuleId.LEFT_CLICK_OPTIMIZER, ModuleId.RIGHT_CLICK_OPTIMIZER -> {
                ctx.fill(x + 3, y + 1, x + 7, y + 9, color);
                ctx.fill(x + 4, y + 2, x + 6, y + 8, 0xFF14161A);
                ctx.fill(x + 4, y + 2, x + 6, y + 4, color);
            }
            case ModuleId.FPS_OPTIMIZER, ModuleId.RENDERING_OPTIMIZER -> {
                ctx.fill(x + 2, y + 2, x + 8, y + 8, color);
                ctx.fill(x + 3, y + 3, x + 7, y + 7, 0xFF14161A);
                ctx.fill(x + 5, y + 4, x + 7, y + 5, color);
            }
            default -> {
                // Signal / Network bars
                ctx.fill(x + 1, y + 6, x + 3, y + 9, color);
                ctx.fill(x + 4, y + 4, x + 6, y + 9, color);
                ctx.fill(x + 7, y + 2, x + 9, y + 9, color);
            }
        }
    }

    /**
     * Draws 10x10 Bottom Dock Icons (Hub, Search, Telemetry, Profiles).
     */
    public static void drawDockIcon(DrawContext ctx, int dockIndex, int x, int y, int color) {
        switch (dockIndex) {
            case 0 -> {
                // Hub / Person outline icon
                ctx.fill(x + 3, y + 1, x + 7, y + 4, color);
                ctx.fill(x + 4, y + 2, x + 6, y + 3, 0xFF14161A);
                ctx.fill(x + 2, y + 5, x + 8, y + 8, color);
                ctx.fill(x + 3, y + 6, x + 7, y + 8, 0xFF14161A);
            }
            case 1 -> {
                // Search Magnifier icon
                ctx.fill(x + 2, y + 1, x + 7, y + 6, color);
                ctx.fill(x + 3, y + 2, x + 6, y + 5, 0xFF14161A);
                ctx.fill(x + 6, y + 6, x + 9, y + 8, color);
            }
            case 2 -> {
                // Telemetry / Stats icon
                ctx.fill(x + 1, y + 5, x + 3, y + 8, color);
                ctx.fill(x + 4, y + 2, x + 6, y + 8, color);
                ctx.fill(x + 7, y + 4, x + 9, y + 8, color);
            }
            default -> {
                // Profiles / Stacked diamond layers icon
                ctx.fill(x + 3, y + 1, x + 7, y + 2, color);
                ctx.fill(x + 2, y + 2, x + 8, y + 4, color);
                ctx.fill(x + 3, y + 4, x + 7, y + 5, color);
                ctx.fill(x + 2, y + 6, x + 8, y + 7, color);
                ctx.fill(x + 3, y + 7, x + 7, y + 8, color);
            }
        }
    }
}
