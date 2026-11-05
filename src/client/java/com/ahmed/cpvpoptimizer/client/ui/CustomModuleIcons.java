package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.common.ModuleId;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws crisp, custom geometric vector/pixel-art icons matching the PYES_CLIENT dashboard aesthetic.
 */
public final class CustomModuleIcons {
    private CustomModuleIcons() {}

    public static void drawModuleIcon(DrawContext ctx, String moduleId, int x, int y, boolean enabled) {
        int white = 0xFFF1F5F9;
        int accent = 0xFFFF4B2B;
        int dim = 0xFF94A3B8;
        int primary = enabled ? white : dim;

        // Subtle icon box frame (24x24 area)
        switch (moduleId) {
            case ModuleId.CRYSTAL_OPTIMIZER -> {
                // End Crystal Diamond + Base
                ctx.fill(x + 10, y + 2, x + 14, y + 4, accent);
                ctx.fill(x + 7, y + 4, x + 17, y + 7, primary);
                ctx.fill(x + 5, y + 7, x + 19, y + 13, primary);
                ctx.fill(x + 8, y + 8, x + 16, y + 12, accent);
                ctx.fill(x + 7, y + 13, x + 17, y + 16, primary);
                ctx.fill(x + 10, y + 16, x + 14, y + 18, accent);
                ctx.fill(x + 4, y + 19, x + 20, y + 22, dim);
            }
            case ModuleId.ANCHOR_OPTIMIZER -> {
                // Respawn Anchor Box + Glowing Portal Core
                ctx.fill(x + 4, y + 3, x + 20, y + 21, primary);
                ctx.fill(x + 6, y + 5, x + 18, y + 19, 0xFF14161B);
                ctx.fill(x + 8, y + 7, x + 16, y + 13, accent);
                ctx.fill(x + 6, y + 15, x + 18, y + 17, primary);
            }
            case ModuleId.GLOWSTONE_OPTIMIZER -> {
                // Sun / Brightness Burst Icon (matches Fullbright/Glow icon in screenshot)
                ctx.fill(x + 9, y + 9, x + 15, y + 15, accent);
                ctx.fill(x + 10, y + 10, x + 14, y + 14, 0xFF14161B);
                // Rays
                ctx.fill(x + 11, y + 3, x + 13, y + 7, accent);
                ctx.fill(x + 11, y + 17, x + 13, y + 21, accent);
                ctx.fill(x + 3, y + 11, x + 7, y + 13, accent);
                ctx.fill(x + 17, y + 11, x + 21, y + 13, accent);
                ctx.fill(x + 5, y + 5, x + 8, y + 8, primary);
                ctx.fill(x + 16, y + 5, x + 19, y + 8, primary);
                ctx.fill(x + 5, y + 16, x + 8, y + 19, primary);
                ctx.fill(x + 16, y + 16, x + 19, y + 19, primary);
            }
            case ModuleId.TOTEM_OPTIMIZER -> {
                // Shield / Armor HUD Emblem (matches Armor HUD shield in screenshot)
                ctx.fill(x + 5, y + 3, x + 19, y + 5, primary);
                ctx.fill(x + 4, y + 5, x + 20, y + 13, primary);
                ctx.fill(x + 6, y + 13, x + 18, y + 17, primary);
                ctx.fill(x + 9, y + 17, x + 15, y + 20, primary);
                ctx.fill(x + 11, y + 20, x + 13, y + 22, primary);
                // Inner core
                ctx.fill(x + 7, y + 6, x + 17, y + 12, 0xFF14161B);
                ctx.fill(x + 9, y + 7, x + 15, y + 14, accent);
            }
            case ModuleId.XP_OPTIMIZER -> {
                // Hourglass / Potion Timer Icon (matches Effect Timer in screenshot)
                ctx.fill(x + 5, y + 3, x + 19, y + 5, primary);
                ctx.fill(x + 7, y + 5, x + 17, y + 9, primary);
                ctx.fill(x + 9, y + 7, x + 15, y + 9, accent);
                ctx.fill(x + 10, y + 9, x + 14, y + 14, primary);
                ctx.fill(x + 7, y + 14, x + 17, y + 19, primary);
                ctx.fill(x + 9, y + 16, x + 15, y + 19, accent);
                ctx.fill(x + 5, y + 19, x + 19, y + 21, primary);
            }
            case ModuleId.BLOCK_PLACEMENT_OPTIMIZER -> {
                // Crosshair Target / Block Framing Icon
                ctx.fill(x + 4, y + 4, x + 9, y + 6, primary);
                ctx.fill(x + 4, y + 4, x + 6, y + 9, primary);
                ctx.fill(x + 15, y + 4, x + 20, y + 6, primary);
                ctx.fill(x + 18, y + 4, x + 20, y + 9, primary);
                ctx.fill(x + 4, y + 18, x + 9, y + 20, primary);
                ctx.fill(x + 4, y + 15, x + 6, y + 20, primary);
                ctx.fill(x + 15, y + 18, x + 20, y + 20, primary);
                ctx.fill(x + 18, y + 15, x + 20, y + 20, primary);
                ctx.fill(x + 9, y + 9, x + 15, y + 15, accent);
            }
            case ModuleId.HOTBAR_OPTIMIZER -> {
                // Inventory / Hotbar Grid Card (matches PYES Inventory in screenshot)
                ctx.fill(x + 3, y + 4, x + 21, y + 20, primary);
                ctx.fill(x + 5, y + 6, x + 19, y + 18, 0xFF14161B);
                ctx.fill(x + 6, y + 7, x + 18, y + 10, accent);
                ctx.fill(x + 6, y + 12, x + 9, y + 16, primary);
                ctx.fill(x + 11, y + 12, x + 14, y + 16, primary);
                ctx.fill(x + 15, y + 12, x + 18, y + 16, accent);
            }
            case ModuleId.LEFT_CLICK_OPTIMIZER -> {
                // Cursor Arrow + Click Burst (matches CPS Counter in screenshot)
                ctx.fill(x + 5, y + 3, x + 8, y + 6, accent);
                ctx.fill(x + 9, y + 7, x + 12, y + 18, primary);
                ctx.fill(x + 12, y + 10, x + 15, y + 16, primary);
                ctx.fill(x + 15, y + 12, x + 18, y + 15, primary);
                ctx.fill(x + 13, y + 16, x + 17, y + 20, accent);
            }
            case ModuleId.RIGHT_CLICK_OPTIMIZER -> {
                // Mouse Body Icon (matches Mouse Tweaks in screenshot)
                ctx.fill(x + 6, y + 3, x + 18, y + 21, primary);
                ctx.fill(x + 8, y + 5, x + 16, y + 19, 0xFF14161B);
                ctx.fill(x + 11, y + 5, x + 13, y + 11, primary);
                ctx.fill(x + 13, y + 5, x + 16, y + 10, accent);
                ctx.fill(x + 8, y + 11, x + 16, y + 12, primary);
            }
            case ModuleId.INPUT_OPTIMIZER -> {
                // Keystrokes / Keyboard Grid Icon (matches Keystrokes in screenshot)
                ctx.fill(x + 3, y + 6, x + 21, y + 18, primary);
                ctx.fill(x + 5, y + 8, x + 19, y + 16, 0xFF14161B);
                ctx.fill(x + 6, y + 9, x + 9, y + 11, accent);
                ctx.fill(x + 11, y + 9, x + 14, y + 11, primary);
                ctx.fill(x + 15, y + 9, x + 18, y + 11, primary);
                ctx.fill(x + 7, y + 13, x + 17, y + 15, accent);
            }
            case ModuleId.FPS_OPTIMIZER -> {
                // Monitor with Rising Graph (matches FPS Display in screenshot)
                ctx.fill(x + 3, y + 4, x + 21, y + 17, primary);
                ctx.fill(x + 5, y + 6, x + 19, y + 15, 0xFF14161B);
                ctx.fill(x + 7, y + 12, x + 10, y + 14, accent);
                ctx.fill(x + 10, y + 10, x + 14, y + 12, primary);
                ctx.fill(x + 14, y + 7, x + 17, y + 10, accent);
                ctx.fill(x + 8, y + 18, x + 16, y + 20, primary);
            }
            case ModuleId.RENDERING_OPTIMIZER -> {
                // Freelook Eye / Lens Icon (matches PYES Freelook in screenshot)
                ctx.fill(x + 4, y + 8, x + 20, y + 16, primary);
                ctx.fill(x + 6, y + 10, x + 18, y + 14, 0xFF14161B);
                ctx.fill(x + 10, y + 9, x + 14, y + 15, accent);
                ctx.fill(x + 2, y + 11, x + 4, y + 13, accent);
                ctx.fill(x + 20, y + 11, x + 22, y + 13, accent);
            }
            case ModuleId.LATENCY_OPTIMIZER -> {
                // Compass / Coordinates Radar Icon (matches Coordinates in screenshot)
                ctx.fill(x + 6, y + 6, x + 18, y + 18, primary);
                ctx.fill(x + 8, y + 8, x + 16, y + 16, 0xFF14161B);
                ctx.fill(x + 10, y + 10, x + 14, y + 14, accent);
                ctx.fill(x + 11, y + 2, x + 13, y + 6, primary);
                ctx.fill(x + 11, y + 18, x + 13, y + 22, primary);
                ctx.fill(x + 2, y + 11, x + 6, y + 13, primary);
                ctx.fill(x + 18, y + 11, x + 22, y + 13, primary);
            }
            default -> {
                // Circular Ring / Server Indicator (matches Loading Indicator in screenshot)
                ctx.fill(x + 5, y + 4, x + 19, y + 20, primary);
                ctx.fill(x + 8, y + 7, x + 16, y + 17, 0xFF14161B);
                ctx.fill(x + 12, y + 4, x + 19, y + 11, accent);
            }
        }
    }
}
