package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ScrollableModuleList {
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_SPACING = 3;

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
        this.scrollbar.setBounds(x + width - 6, y + 2, 4, Math.max(0, height - 4));
        clampScroll();
    }

    public int getRowWidth() {
        return Math.max(100, width - 14);
    }

    public int getContentHeight() {
        if (buttons.isEmpty()) return 0;
        return buttons.size() * (ROW_HEIGHT + ROW_SPACING) + 4;
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

        int rowWidth = getRowWidth();
        int rowX = x + 4;
        boolean insideViewport = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        context.enableScissor(x, y, x + width, y + height);
        int currentY = y + 2 - (int) Math.round(scrollAmount);
        for (ModuleButton button : buttons) {
            if (currentY + ROW_HEIGHT >= y && currentY <= y + height) {
                button.render(
                        context,
                        textRenderer,
                        rowX,
                        currentY,
                        rowWidth,
                        ROW_HEIGHT,
                        mouseX,
                        mouseY,
                        insideViewport && !scrollbar.isDragging(),
                        button.getModule() == selectedModule,
                        deltaSeconds
                );
            }
            currentY += ROW_HEIGHT + ROW_SPACING;
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

        int rowWidth = getRowWidth();
        int rowX = x + 4;
        int currentY = y + 2 - (int) Math.round(scrollAmount);
        for (ModuleButton moduleButton : buttons) {
            if (currentY + ROW_HEIGHT >= y && currentY <= y + height) {
                if (moduleButton.mouseClicked(mouseX, mouseY, button, rowX, currentY, rowWidth, ROW_HEIGHT)) {
                    return true;
                }
            }
            currentY += ROW_HEIGHT + ROW_SPACING;
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
        int step = ROW_HEIGHT + ROW_SPACING;
        this.targetScrollAmount = MathHelper.clamp(this.targetScrollAmount - verticalAmount * step, 0.0, getMaxScroll());
        return true;
    }
}
