package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vanilla-styled scrollable module list with scissor clipping, smooth wheel scrolling,
 * draggable scrollbar thumb, clickable scrollbar track, and preserved scroll offset.
 */
public final class ScrollableModuleList {
    private static final int ROW_HEIGHT = 34;
    private static final int ROW_SPACING = 4;

    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Scrollbar scrollbar = new Scrollbar();

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

    public void setDimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        int rowAreaWidth = getRowWidth();
        int scrollbarX = x + (width - rowAreaWidth) / 2 + rowAreaWidth + 6;
        this.scrollbar.setBounds(scrollbarX, y + 2, 6, Math.max(0, height - 4));
        clampScroll();
    }

    public int getRowWidth() {
        return Math.min(340, Math.max(180, width - 28));
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

        // Vanilla recessed list container background & top/bottom borders
        context.fill(x, y, x + width, y + height, 0xA0101010);
        context.fill(x, y, x + width, y + 1, 0xFF000000);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF000000);

        int rowWidth = getRowWidth();
        int rowX = x + (width - rowWidth) / 2 - 4;
        boolean insideViewport = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        // Strict scissor clipping so module rows never overflow outside the list bounds
        context.enableScissor(x, y + 1, x + width, y + height - 1);
        int currentY = y + 4 - (int) Math.round(scrollAmount);
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
        // Check scrollbar first so clicking/dragging the scrollbar never toggles a module
        if (scrollbar.mouseClicked(mouseX, mouseY, button, scrollAmount, getMaxScroll(), getContentHeight(), this::setScrollAmountImmediate)) {
            return true;
        }

        int rowWidth = getRowWidth();
        int rowX = x + (width - rowWidth) / 2 - 4;
        int currentY = y + 4 - (int) Math.round(scrollAmount);
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
        int step = (ROW_HEIGHT + ROW_SPACING) / 2;
        this.targetScrollAmount = MathHelper.clamp(this.targetScrollAmount - verticalAmount * step, 0.0, getMaxScroll());
        return true;
    }
}
