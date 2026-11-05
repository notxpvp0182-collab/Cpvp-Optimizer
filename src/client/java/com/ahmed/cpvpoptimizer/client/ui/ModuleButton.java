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

public final class ModuleButton {
    private final OptimizerModule module;
    private final Consumer<OptimizerModule> onOpenSettings;
    private float hoverProgress = 0.0f;
    private float toggleProgress;

    public ModuleButton(OptimizerModule module, Consumer<OptimizerModule> onOpenSettings) {
        this.module = module;
        this.onOpenSettings = onOpenSettings;
        this.toggleProgress = module.isEnabled() ? 1.0f : 0.0f;
    }

    public OptimizerModule getModule() {
        return module;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height, int mouseX, int mouseY, boolean withinViewport, float deltaSeconds) {
        boolean hovered = withinViewport && isMouseOver(mouseX, mouseY, x, y, width, height);
        this.hoverProgress = UIAnimations.approach(hoverProgress, hovered ? 1.0f : 0.0f, deltaSeconds, 12.0f);
        this.toggleProgress = UIAnimations.approach(toggleProgress, module.isEnabled() ? 1.0f : 0.0f, deltaSeconds, 10.0f);

        int outerBorder = hovered ? 0xFFFFFFFF : 0xFFA0A0A0;
        int baseBg = ColorHelper.lerp(hoverProgress, 0xFF2B2B2B, 0xFF3A3A3A);

        context.fill(x, y, x + width, y + height, outerBorder);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, baseBg);

        boolean masterActive = OptimizerConfig.getInstance().isMasterEnabled();
        int titleColor = masterActive ? 0xFFFFFFFF : 0xFF808080;
        String title = module.getDisplayName() + " [" + module.getCategory().getDisplayName() + "]";
        context.drawTextWithShadow(textRenderer, title, x + 8, y + 6, titleColor);

        String subText = module.isEnabled() ? module.getDiagnosticsSummary() : module.getDescription();
        int maxSubWidth = Math.max(40, width - 115);
        String trimmedSub = textRenderer.trimToWidth(subText, maxSubWidth);
        context.drawTextWithShadow(textRenderer, trimmedSub, x + 8, y + 19, 0xFFA0A0A0);

        boolean active = masterActive && module.isEnabled();
        String stateText = active ? "ON" : "OFF";
        int stateColor = active ? 0xFF55FF55 : 0xFFFF5555;
        int badgeWidth = 44;
        int badgeX = x + width - badgeWidth - 6;
        int badgeY = y + 5;
        int badgeHeight = height - 10;

        int badgeBg = ColorHelper.lerp(toggleProgress, 0xFF222222, 0xFF334433);
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, 0xFF000000);
        context.fill(badgeX + 1, badgeY + 1, badgeX + badgeWidth - 1, badgeY + badgeHeight - 1, badgeBg);

        int textW = textRenderer.getWidth(stateText);
        context.drawTextWithShadow(
                textRenderer,
                stateText,
                badgeX + (badgeWidth - textW) / 2,
                badgeY + (badgeHeight - 8) / 2,
                stateColor
        );
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
