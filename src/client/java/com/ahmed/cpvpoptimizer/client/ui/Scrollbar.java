package com.ahmed.cpvpoptimizer.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class Scrollbar {
    private int x;
    private int y;
    private int width = 2;
    private int height;
    private boolean dragging = false;
    private double dragGrabOffset = 0.0;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isVisible(int contentHeight) {
        return contentHeight > height && height > 0;
    }

    public int getThumbHeight(int contentHeight) {
        if (contentHeight <= height || contentHeight <= 0) return height;
        int thumb = (int) ((float) (height * height) / (float) contentHeight);
        return MathHelper.clamp(thumb, 18, Math.max(18, height - 4));
    }

    public int getThumbY(double scrollAmount, int maxScroll, int contentHeight) {
        if (maxScroll <= 0) return y;
        int thumbHeight = getThumbHeight(contentHeight);
        int movableTrack = Math.max(1, height - thumbHeight);
        double ratio = MathHelper.clamp(scrollAmount / (double) maxScroll, 0.0, 1.0);
        return y + (int) Math.round(ratio * movableTrack);
    }

    public void render(DrawContext context, int mouseX, int mouseY, double scrollAmount, int maxScroll, int contentHeight) {
        if (!isVisible(contentHeight)) return;

        int thumbHeight = getThumbHeight(contentHeight);
        int thumbY = getThumbY(scrollAmount, maxScroll, contentHeight);
        boolean hovered = isMouseOverTrack(mouseX, mouseY);

        context.fill(x, y, x + width, y + height, 0x22FFFFFF);
        int thumbColor = (hovered || dragging) ? 0xAAFFFFFF : 0x55FFFFFF;
        context.fill(x, thumbY, x + width, thumbY + thumbHeight, thumbColor);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, double currentScroll, int maxScroll, int contentHeight, ScrollConsumer scrollUpdater) {
        if (button != 0 || !isVisible(contentHeight) || !isMouseOverTrack(mouseX, mouseY)) {
            return false;
        }
        int thumbHeight = getThumbHeight(contentHeight);
        int thumbY = getThumbY(currentScroll, maxScroll, contentHeight);

        if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
            this.dragging = true;
            this.dragGrabOffset = mouseY - thumbY;
        } else {
            this.dragging = true;
            this.dragGrabOffset = thumbHeight / 2.0;
            updateScrollFromMouse(mouseY, maxScroll, contentHeight, scrollUpdater);
        }
        return true;
    }

    public boolean mouseDragged(double mouseY, int button, int maxScroll, int contentHeight, ScrollConsumer scrollUpdater) {
        if (!dragging || button != 0 || !isVisible(contentHeight)) {
            return false;
        }
        updateScrollFromMouse(mouseY, maxScroll, contentHeight, scrollUpdater);
        return true;
    }

    public void mouseReleased() {
        this.dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isMouseOverTrack(double mouseX, double mouseY) {
        return mouseX >= x - 3 && mouseX <= x + width + 3 && mouseY >= y && mouseY <= y + height;
    }

    private void updateScrollFromMouse(double mouseY, int maxScroll, int contentHeight, ScrollConsumer scrollUpdater) {
        int thumbHeight = getThumbHeight(contentHeight);
        int movableTrack = Math.max(1, height - thumbHeight);
        double relativeY = (mouseY - y - dragGrabOffset) / (double) movableTrack;
        double targetScroll = MathHelper.clamp(relativeY, 0.0, 1.0) * maxScroll;
        scrollUpdater.accept(targetScroll);
    }

    @FunctionalInterface
    public interface ScrollConsumer {
        void accept(double newTargetScroll);
    }
}
