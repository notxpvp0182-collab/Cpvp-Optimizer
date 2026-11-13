package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.util.function.Consumer;

/**
 * Compact Frosted-Glass Module Row matching Screenshot 4:
 * - Left: Minimal 10x10 monochrome icon + Module Name.
 * - Right: Subtle settings indicator + Monochrome Frosted Pill Toggle Switch.
 * - Left-Click: Toggles module ON/OFF with smooth pill thumb animation.
 * - Right-Click (or clicking settings area): Opens the module's Settings Inspector.
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

    public void renderRow(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int height,
                          int mouseX, int mouseY, boolean withinViewport, boolean isSelectedForSettings, float deltaSeconds) {
        boolean hovered = withinViewport && isMouseOver(mouseX, mouseY, x, y, width, height);
        boolean active = OptimizerConfig.getInstance().isMasterEnabled() && module.isEnabled();

        this.hoverAnim = UIAnimations.approach(hoverAnim, hovered ? 1.0f : 0.0f, deltaSeconds, 16.0f);
        this.toggleAnim = UIAnimations.approach(toggleAnim, active ? 1.0f : 0.0f, deltaSeconds, 14.0f);

        int rowFill = PremiumTheme.lerpColor(hoverAnim, PremiumTheme.GLASS_ELEVATED, PremiumTheme.GLASS_HOVER);
        int border = isSelectedForSettings
                ? PremiumTheme.BORDER_SELECTED
                : PremiumTheme.lerpColor(hoverAnim, PremiumTheme.BORDER_SUBTLE, PremiumTheme.BORDER_HOVER);

        GlassRenderer.drawGlassPanel(ctx, x, y, width, height, rowFill, border, false);

        // 1. Minimal Monochrome Icon on the Left
        int iconColor = PremiumTheme.lerpColor(toggleAnim, PremiumTheme.TEXT_SECONDARY, PremiumTheme.TEXT_PRIMARY);
        CustomModuleIcons.drawMiniModuleIcon(ctx, module.getId(), x + 7, y + (height - 10) / 2, iconColor);

        // 2. Clean Module Name
        String cleanName = module.getDisplayName().replace(" Optimizer", "");
        int maxNameW = Math.max(40, width - 64);
        String trimmedName = textRenderer.trimToWidth(cleanName, maxNameW);
        int textColor = PremiumTheme.lerpColor(toggleAnim, PremiumTheme.TEXT_SECONDARY, PremiumTheme.TEXT_PRIMARY);
        ctx.drawText(textRenderer, trimmedName, x + 22, y + (height - 8) / 2, textColor, false);

        // 3. Subtle Settings Chevron + Monochrome Frosted Pill Switch on the Right
        int switchW = 22;
        int switchH = 11;
        int switchX = x + width - switchW - 6;
        int switchY = y + (height - switchH) / 2;

        if (hovered) {
            ctx.drawText(textRenderer, "›", switchX - 9, y + (height - 8) / 2, PremiumTheme.TEXT_MUTED, false);
        }

        GlassRenderer.drawFrostedSwitch(ctx, switchX, switchY, switchW, switchH, toggleAnim);
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
