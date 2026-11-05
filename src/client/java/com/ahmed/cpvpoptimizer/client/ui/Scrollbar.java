package com.ahmed.cpvpoptimizer.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Classic Vanilla Minecraft Options-list style vertical scrollbar with proportional thumb,
 * draggable thumb interaction, and clickable track paging.
 */
public final class Scrollbar {
    private static final int TRACK_BG_COLOR = 0xFF000000;
    private static final int THUMB_OUTER_COLOR = 0xFF808080;
    private static final int THUMB_INNER_COLOR = 0xFFC0C0C0;
    private static final int THUMB_HOVER_COLOR = 0xFFD8D8D8;

    private int x;
    private int y;
    private int width = 6;
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
        return MathHelper.clamp(thumb, 32, Math.max(32, height - 8));
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

        // Vanilla recessed dark scrollbar track
        context.fill(x, y, x + width, y + height, TRACK_BG_COLOR);
        // Classic Minecraft 2-layer beveled scrollbar thumb
        context.fill(x, thumbY, x + width, thumbY + thumbHeight, THUMB_OUTER_COLOR);
        context.fill(x, thumbY, x + width - 1, thumbY + thumbHeight - 1,
                (hovered || dragging) ? THUMB_HOVER_COLOR : THUMB_INNER_COLOR);
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
            // Click on track jumps proportionally toward clicked position
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
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
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
