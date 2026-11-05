package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * Renders an individual Module Card matching the exact PYES_CLIENT reference image:
 * - Dark carbon card surface (#15171C) with subtle slate border (#262932)
 * - Top-left red-orange glowing corner bracket (⌜)
 * - Custom vector icon on the left, bold white title & 2-line gray description on the right
 * - Full-width bottom button: Orange-Red horizontal gradient ("ENABLED") or Dark recessed ("DISABLED")
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

    public void renderCard(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int height,
                           int mouseX, int mouseY, boolean withinViewport, boolean isSelectedForSettings, float deltaSeconds) {
        boolean hovered = withinViewport && isMouseOver(mouseX, mouseY, x, y, width, height);
        boolean active = OptimizerConfig.getInstance().isMasterEnabled() && module.isEnabled();

        this.hoverAnim = UIAnimations.approach(hoverAnim, hovered ? 1.0f : 0.0f, deltaSeconds, 16.0f);
        this.toggleAnim = UIAnimations.approach(toggleAnim, active ? 1.0f : 0.0f, deltaSeconds, 14.0f);

        // 1. Card Outer Border & Dark Carbon Surface
        int borderColor = isSelectedForSettings
                ? 0xFFFF4B2B
                : ColorHelper.lerp(hoverAnim, 0xFF23262F, 0xFF383C4A);
        int cardBg = ColorHelper.lerp(hoverAnim, 0xF214161C, 0xF5191C24);

        ctx.fill(x, y, x + width, y + height, borderColor);
        ctx.fill(x + 1, y + 1, x + width - 1, y + height - 1, cardBg);

        // 2. Signature Top-Left Red-Orange Corner Bracket (⌜) exactly like the screenshot
        int bracketColor = active ? 0xFFFF4B2B : 0xCCDC2626;
        ctx.fill(x, y, x + 12, y + 2, bracketColor);
        ctx.fill(x, y, x + 2, y + 12, bracketColor);

        // 3. Left Custom Icon Symbol (24x24)
        int iconX = x + 10;
        int iconY = y + 12;
        CustomModuleIcons.drawModuleIcon(ctx, module.getId(), iconX, iconY, active);

        // 4. Right Title & 2-Line Description
        int textX = x + 40;
        int maxTextW = Math.max(40, width - 46);

        String rawTitle = module.getDisplayName().replace(" Optimizer", "");
        String title = textRenderer.trimToWidth(rawTitle, maxTextW);
        ctx.drawTextWithShadow(textRenderer, title, textX, y + 9, 0xFFFFFFFF);

        List<OrderedText> wrappedDesc = textRenderer.wrapLines(Text.literal(module.getDescription()), maxTextW);
        int descY = y + 21;
        for (int i = 0; i < Math.min(2, wrappedDesc.size()); i++) {
            ctx.drawText(textRenderer, wrappedDesc.get(i), textX, descY + (i * 9), 0xFF8E96A8, false);
        }

        // 5. Bottom Full-Width Action Button (ENABLED with Orange-Red Gradient / DISABLED Dark)
        int btnX = x + 8;
        int btnW = width - 16;
        int btnH = 16;
        int btnY = y + height - btnH - 7;

        if (active) {
            drawHorizontalGradient(ctx, btnX, btnY, btnW, btnH, 0xFFFF9A1F, 0xFFFF3B1D);
            // Subtle top highlight on the enabled button
            ctx.fill(btnX + 1, btnY, btnX + btnW - 1, btnY + 1, 0x66FFFFFF);
            String label = "ENABLED";
            int tw = textRenderer.getWidth(label);
            ctx.drawTextWithShadow(textRenderer, label, btnX + (btnW - tw) / 2, btnY + 4, 0xFFFFFFFF);
        } else {
            ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF252832);
            ctx.fill(btnX + 1, btnY + 1, btnX + btnW - 1, btnY + btnH - 1, 0xFF101217);
            String label = "DISABLED";
            int tw = textRenderer.getWidth(label);
            ctx.drawText(textRenderer, label, btnX + (btnW - tw) / 2, btnY + 4, 0xFF94A3B8, false);
        }
    }

    public static void drawHorizontalGradient(DrawContext ctx, int x, int y, int width, int height, int leftColor, int rightColor) {
        int steps = 12;
        for (int i = 0; i < steps; i++) {
            int sx = x + (i * width) / steps;
            int ex = x + ((i + 1) * width) / steps;
            float t = (float) i / (float) (steps - 1);
            int color = ColorHelper.lerp(t, leftColor, rightColor);
            ctx.fill(sx, y, ex, y + height, color);
        }
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
