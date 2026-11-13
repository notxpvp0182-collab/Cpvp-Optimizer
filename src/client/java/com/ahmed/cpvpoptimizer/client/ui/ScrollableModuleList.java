package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Compact vertical module row list inside the Center Frosted-Glass Panel (Screenshot 4).
 */
public final class ScrollableModuleList {
    private static final int ROW_H = PremiumTheme.ROW_HEIGHT;
    private static final int GAP = PremiumTheme.ROW_GAP;

    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Scrollbar scrollbar = new Scrollbar();
    private OptimizerModule selectedModule;

    private int x;
    private int y;
    private int width;
    private int height;

    private double scrollAmount = 0.0;
    private double targetScrollAmount = 0.0;

    public ScrollableModuleList(List<OptimizerModule> modules, Consumer<OptimizerModule> onOpenSettings) {
        for (OptimizerModule module : modules) {
            buttons.add(new ModuleButton(module, onOpenSettings));
        }
    }

    public void setSelectedModule(OptimizerModule selectedModule) {
        this.selectedModule = selectedModule;
    }

    public void setDimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scrollbar.setBounds(x + width - 4, y + 2, 2, Math.max(0, height - 4));
        clampScroll();
    }

    public int getContentHeight() {
        if (buttons.isEmpty()) return 0;
        return buttons.size() * (ROW_H + GAP) + 4;
    }

    public int getMaxScroll() {
        return Math.max(0, getContentHeight() - height);
    }

    public double getScrollAmount() {
        return targetScrollAmount;
    }

    public void setScrollAmountImmediate(double scroll) {
        this.targetScrollAmount = MathHelper.clamp(scroll, 0.0, getMaxScroll());
        this.scrollAmount = this.targetScrollAmount;
    }

    private void clampScroll() {
        int max = getMaxScroll();
        this.targetScrollAmount = MathHelper.clamp(this.targetScrollAmount, 0.0, max);
        this.scrollAmount = MathHelper.clamp(this.scrollAmount, 0.0, max);
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float deltaSeconds) {
        clampScroll();
        this.scrollAmount = UIAnimations.smoothScroll(this.scrollAmount, this.targetScrollAmount, deltaSeconds);

        int rowW = width - (scrollbar.isVisible(getContentHeight()) ? 8 : 2);
        int rowX = x + 1;
        int curY = y + 2 - (int) Math.round(scrollAmount);
        boolean insideViewport = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        context.enableScissor(x, y, x + width, y + height);
        for (ModuleButton btn : buttons) {
            if (curY + ROW_H >= y && curY <= y + height) {
                btn.renderRow(
                        context,
                        textRenderer,
                        rowX,
                        curY,
                        rowW,
                        ROW_H,
                        mouseX,
                        mouseY,
                        insideViewport && !scrollbar.isDragging(),
                        btn.getModule() == selectedModule,
                        deltaSeconds
                );
            }
            curY += ROW_H + GAP;
        }
        context.disableScissor();

        scrollbar.render(context, mouseX, mouseY, scrollAmount, getMaxScroll(), getContentHeight());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        if (scrollbar.mouseClicked(mouseX, mouseY, button, scrollAmount, getMaxScroll(), getContentHeight(), this::setScrollAmountImmediate)) {
            return true;
        }

        int rowW = width - (scrollbar.isVisible(getContentHeight()) ? 8 : 2);
        int rowX = x + 1;
        int curY = y + 2 - (int) Math.round(scrollAmount);

        for (ModuleButton btn : buttons) {
            if (curY + ROW_H >= y && curY <= y + height) {
                if (btn.mouseClicked(mouseX, mouseY, button, rowX, curY, rowW, ROW_H)) {
                    return true;
                }
            }
            curY += ROW_H + GAP;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return scrollbar.mouseDragged(mouseY, button, getMaxScroll(), getContentHeight(), this::setScrollAmountImmediate);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.isDragging()) {
            scrollbar.mouseReleased();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        int step = ROW_H + GAP;
        this.targetScrollAmount = MathHelper.clamp(this.targetScrollAmount - verticalAmount * step, 0.0, getMaxScroll());
        return true;
    }
}
