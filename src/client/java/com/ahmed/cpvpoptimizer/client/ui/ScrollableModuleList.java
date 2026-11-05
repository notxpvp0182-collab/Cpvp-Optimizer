package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Multi-Column Card Grid matching the PYES_CLIENT dashboard layout (up to 4 cards per row).
 */
public final class ScrollableModuleList {
    private static final int CARD_HEIGHT = 68;
    private static final int GAP = 8;

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
        this.scrollbar.setBounds(x + width - 5, y + 4, 3, Math.max(0, height - 8));
        clampScroll();
    }

    public int getColumnCount() {
        int usableW = width - 16;
        if (usableW >= 460) return 4;
        if (usableW >= 330) return 3;
        if (usableW >= 210) return 2;
        return 1;
    }

    public int getCardWidth(int cols) {
        int usableW = width - 16 - (cols - 1) * GAP;
        return Math.max(95, usableW / cols);
    }

    public int getContentHeight() {
        if (buttons.isEmpty()) return 0;
        int cols = getColumnCount();
        int rows = (buttons.size() + cols - 1) / cols;
        return rows * (CARD_HEIGHT + GAP) + 8;
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

        int cols = getColumnCount();
        int cardW = getCardWidth(cols);
        int startX = x + 6;
        int startY = y + 6 - (int) Math.round(scrollAmount);
        boolean insideViewport = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        context.enableScissor(x, y, x + width, y + height);
        for (int i = 0; i < buttons.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cardX = startX + col * (cardW + GAP);
            int cardY = startY + row * (CARD_HEIGHT + GAP);

            if (cardY + CARD_HEIGHT >= y && cardY <= y + height) {
                ModuleButton btn = buttons.get(i);
                btn.renderCard(
                        context,
                        textRenderer,
                        cardX,
                        cardY,
                        cardW,
                        CARD_HEIGHT,
                        mouseX,
                        mouseY,
                        insideViewport && !scrollbar.isDragging(),
                        btn.getModule() == selectedModule,
                        deltaSeconds
                );
            }
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

        int cols = getColumnCount();
        int cardW = getCardWidth(cols);
        int startX = x + 6;
        int startY = y + 6 - (int) Math.round(scrollAmount);

        for (int i = 0; i < buttons.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cardX = startX + col * (cardW + GAP);
            int cardY = startY + row * (CARD_HEIGHT + GAP);

            if (cardY + CARD_HEIGHT >= y && cardY <= y + height) {
                if (buttons.get(i).mouseClicked(mouseX, mouseY, button, cardX, cardY, cardW, CARD_HEIGHT)) {
                    return true;
                }
            }
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
        int step = (CARD_HEIGHT + GAP) / 2;
        this.targetScrollAmount = MathHelper.clamp(this.targetScrollAmount - verticalAmount * step, 0.0, getMaxScroll());
        return true;
    }
}
