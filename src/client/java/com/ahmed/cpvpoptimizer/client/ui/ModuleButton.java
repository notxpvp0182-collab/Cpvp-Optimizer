package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.ColorHelper;

import java.util.function.Consumer;

/**
 * Premium Dark Liquid Glass Module Row:
 * - Displays ONLY the clean Module Name & Animated Liquid Glass Pill Switch (Zero extra description clutter).
 * - Left-Click: Smoothly toggles ON/OFF with animated glowing pill & saves immediately.
 * - Right-Click: Opens the adjacent Liquid Glass Settings Drawer for this module.
 */
public final class ModuleButton {
    private final OptimizerModule module;
    private final Consumer<OptimizerModule> onOpenSettings;
    private float hoverAnim = 0.0f;
    private float toggleAnim;

    public ModuleButton(OptimizerModule module, Consumer<OptimizerModule> onOpenSettings) {
        this.module = module;
        this.onOpenSettings = onOpenSettings;
        this.toggleAnim = module.isEnabled() ? 1.0f : 0.0f;
    }

    public OptimizerModule getModule() {
        return module;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height,
                       int mouseX, int mouseY, boolean withinViewport, boolean isSelectedForSettings, float deltaSeconds) {
        boolean hovered = withinViewport && isMouseOver(mouseX, mouseY, x, y, width, height);
        boolean active = OptimizerConfig.getInstance().isMasterEnabled() && module.isEnabled();

        this.hoverAnim = UIAnimations.approach(hoverAnim, hovered ? 1.0f : 0.0f, deltaSeconds, 16.0f);
        this.toggleAnim = UIAnimations.approach(toggleAnim, active ? 1.0f : 0.0f, deltaSeconds, 14.0f);

        // Frosted Dark Liquid Glass Row Fill
        int baseGlass = ColorHelper.lerp(hoverAnim, 0x88121722, 0xB81B2436);
        int activeTint = ColorHelper.lerp(toggleAnim, baseGlass, 0xC8162C3D);
        context.fill(x, y, x + width, y + height, activeTint);

        // Specular Top Glass Edge Highlight & Subtle Bottom Border
        int topBorder = isSelectedForSettings
                ? 0xFF00E5FF
                : ColorHelper.lerp(toggleAnim, 0x35FFFFFF, 0x9938BDF8);
        context.fill(x, y, x + width, y + 1, topBorder);
        context.fill(x, y + height - 1, x + width, y + height, 0x5505070B);

        // Left Accent Glow Bar (expands smoothly when enabled)
        int accentColor = ColorHelper.lerp(toggleAnim, 0x44475569, 0xFF00E5FF);
        context.fill(x, y, x + 2, y + height, accentColor);

        // Clean Module Name (No cluttered description)
        int nameColor = ColorHelper.lerp(toggleAnim, 0xFFB0BAC9, 0xFFFFFFFF);
        int textY = y + (height - 8) / 2;
        context.drawTextWithShadow(textRenderer, module.getDisplayName(), x + 10, textY, nameColor);

        // Custom Tag / Gear Indicator + Animated Liquid Pill Switch on the right
        int pillWidth = 30;
        int pillHeight = 14;
        int pillX = x + width - pillWidth - 8;
        int pillY = y + (height - pillHeight) / 2;

        // Subtle "..." settings dots indicator to the left of the switch
        int dotsColor = isSelectedForSettings ? 0xFF00E5FF : (hovered ? 0xCCFFFFFF : 0x6694A3B8);
        context.drawTextWithShadow(textRenderer, "•••", pillX - 18, textY, dotsColor);

        // Pill Track
        int trackBorder = ColorHelper.lerp(toggleAnim, 0x5594A3B8, 0xFF00E5FF);
        int trackFill = ColorHelper.lerp(toggleAnim, 0xAA090D14, 0xCC0284C7);
        context.fill(pillX, pillY, pillX + pillWidth, pillY + pillHeight, trackBorder);
        context.fill(pillX + 1, pillY + 1, pillX + pillWidth - 1, pillY + pillHeight - 1, trackFill);

        // Sliding Pill Knob
        int knobSize = pillHeight - 4;
        int knobMinX = pillX + 2;
        int knobMaxX = pillX + pillWidth - knobSize - 2;
        int knobX = knobMinX + Math.round(toggleAnim * (knobMaxX - knobMinX));
        int knobY = pillY + 2;
        int knobColor = ColorHelper.lerp(toggleAnim, 0xFF94A3B8, 0xFFFFFFFF);
        context.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knobColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (!isMouseOver(mouseX, mouseY, x, y, width, height)) {
            return false;
        }
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
        if (button == 0) {
            if (!OptimizerConfig.getInstance().isSafeMode()) {
                module.toggle();
                OptimizerConfig.getInstance().save();
            }
            return true;
        } else if (button == 1) {
            onOpenSettings.accept(module);
            return true;
        }
        return false;
    }

    public static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
