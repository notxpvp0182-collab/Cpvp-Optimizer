package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleRegistry;
import com.ahmed.cpvpoptimizer.common.ModuleSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Premium Dark Liquid Glass Floating ClickGUI:
 * - Fully transparent in-game background (no vanilla dirt or blur).
 * - Draggable from the top Liquid Glass Header Bar.
 * - Resizable from the bottom-right corner handle, automatically adapting to all 4 GUI scales.
 * - Left-click module: Smooth animated ON/OFF toggle.
 * - Right-click module: Opens the integrated right-side Liquid Glass customization panel with sliders, toggles, and a custom text input box.
 */
public final class OptimizerScreen extends Screen {
    private static final int HEADER_HEIGHT = 26;
    private static final int CATEGORY_BAR_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 22;
    private static final int RESIZE_HANDLE_SIZE = 12;

    private final Screen parent;
    private ModuleCategory selectedCategory = null;
    private OptimizerModule activeSettingsModule = null;
    private ScrollableModuleList moduleList;
    private double savedScrollOffset = 0.0;

    // Floating Window Coordinates & Dimensions
    private int winX;
    private int winY;
    private int winW;
    private int winH;

    // Drag & Resize states
    private boolean draggingWindow = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean resizingWindow = false;
    private int resizeStartW = 0;
    private int resizeStartH = 0;
    private double resizeStartMouseX = 0;
    private double resizeStartMouseY = 0;

    // Side Settings Panel Interaction States
    private ModuleSetting.IntSliderSetting activeDraggingSlider = null;
    private ModuleSetting.TextSetting focusedTextSetting = null;
    private float openScaleAnim = 0.0f;
    private float drawerAnim = 0.0f;

    public OptimizerScreen(Screen parent) {
        super(Text.literal("CPVP Optimizer"));
        this.parent = parent;
    }

    public OptimizerScreen(Screen parent, ModuleCategory retainedCategory, double retainedScrollOffset) {
        this(parent);
        this.selectedCategory = retainedCategory;
        this.savedScrollOffset = retainedScrollOffset;
    }

    @Override
    protected void init() {
        super.init();
        OptimizerConfig cfg = OptimizerConfig.getInstance();

        int minW = Math.min(300, this.width - 16);
        int minH = Math.min(200, this.height - 16);
        int maxW = Math.max(minW, this.width - 16);
        int maxH = Math.max(minH, this.height - 16);

        this.winW = MathHelper.clamp(cfg.getWindowWidth(), minW, maxW);
        this.winH = MathHelper.clamp(cfg.getWindowHeight(), minH, maxH);

        if (cfg.getWindowX() < 0 || cfg.getWindowY() < 0) {
            this.winX = (this.width - this.winW) / 2;
            this.winY = (this.height - this.winH) / 2;
        } else {
            this.winX = MathHelper.clamp(cfg.getWindowX(), 4, Math.max(4, this.width - this.winW - 4));
            this.winY = MathHelper.clamp(cfg.getWindowY(), 4, Math.max(4, this.height - this.winH - 4));
        }

        rebuildModuleList();
    }

    private void rebuildModuleList() {
        List<OptimizerModule> filtered = selectedCategory == null
                ? ModuleRegistry.getInstance().getAllModules()
                : ModuleRegistry.getInstance().getByCategory(selectedCategory);

        this.moduleList = new ScrollableModuleList(filtered, module -> {
            if (this.activeSettingsModule == module) {
                this.activeSettingsModule = null;
                this.focusedTextSetting = null;
            } else {
                this.activeSettingsModule = module;
                this.focusedTextSetting = null;
            }
            this.moduleList.setSelectedModule(this.activeSettingsModule);
        });
        this.moduleList.setSelectedModule(this.activeSettingsModule);
        updateLayoutBounds();
        this.moduleList.setScrollAmountImmediate(savedScrollOffset);
    }

    private void updateLayoutBounds() {
        if (this.moduleList == null) return;
        int listX = winX + 6;
        int listY = winY + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 4;
        int listH = Math.max(40, winH - HEADER_HEIGHT - CATEGORY_BAR_HEIGHT - FOOTER_HEIGHT - 8);
        int listW = (activeSettingsModule != null)
                ? Math.max(120, (int) (winW * 0.50f) - 8)
                : (winW - 12);
        this.moduleList.setDimensions(listX, listY, listW, listH);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep game world 100% transparent behind the floating Liquid Glass window!
        context.fill(0, 0, this.width, this.height, 0x22020408);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaSeconds = Math.min(0.1f, delta / 20.0f);
        this.openScaleAnim = UIAnimations.approach(this.openScaleAnim, 1.0f, deltaSeconds, 14.0f);
        this.drawerAnim = UIAnimations.approach(this.drawerAnim, activeSettingsModule != null ? 1.0f : 0.0f, deltaSeconds, 14.0f);
        updateLayoutBounds();

        this.renderBackground(context, mouseX, mouseY, delta);

        // 1. Outer Ambient Drop Shadow
        context.fill(winX - 3, winY - 3, winX + winW + 3, winY + winH + 3, 0x55000000);
        context.fill(winX - 1, winY - 1, winX + winW + 1, winY + winH + 1, 0x99030712);

        // 2. Main Dark Liquid Glass Body
        context.fill(winX, winY, winX + winW, winY + winH, 0xD80B0F19);

        // 3. Liquid Glass Rim Border (Cyan/Ice Specular Top + Deep Slate Bottom)
        context.fill(winX, winY, winX + winW, winY + 1, 0xCC38BDF8);
        context.fill(winX, winY + winH - 1, winX + winW, winY + winH, 0x881E293B);
        context.fill(winX, winY, winX + 1, winY + winH, 0x7738BDF8);
        context.fill(winX + winW - 1, winY, winX + winW, winY + winH, 0x771E293B);

        // 4. Draggable Top Header Bar
        context.fill(winX + 1, winY + 1, winX + winW - 1, winY + HEADER_HEIGHT, 0xCC111827);
        context.fill(winX + 1, winY + HEADER_HEIGHT, winX + winW - 1, winY + HEADER_HEIGHT + 1, 0x5538BDF8);

        context.drawTextWithShadow(this.textRenderer, "CPVP OPTIMIZER", winX + 10, winY + 9, 0xFF00E5FF);

        // Header Quick Pill Buttons: [Max Fast] [Safe] [X]
        OptimizerConfig cfg = OptimizerConfig.getInstance();
        int closeBtnX = winX + winW - 22;
        int safeBtnX = closeBtnX - 48;
        int maxFastBtnX = safeBtnX - 62;
        int hdrBtnY = winY + 5;

        drawGlassPillButton(context, maxFastBtnX, hdrBtnY, 58, 16, "MAX FAST", false, mouseX, mouseY);
        drawGlassPillButton(context, safeBtnX, hdrBtnY, 44, 16, "SAFE", cfg.isSafeMode(), mouseX, mouseY);
        drawGlassPillButton(context, closeBtnX, hdrBtnY, 16, 16, "×", false, mouseX, mouseY);

        // 5. Category Filter Bar
        int catY = winY + HEADER_HEIGHT + 3;
        int catX = winX + 6;
        catX = drawCategoryTab(context, catX, catY, "ALL", selectedCategory == null, mouseX, mouseY);
        for (ModuleCategory cat : ModuleCategory.values()) {
            catX = drawCategoryTab(context, catX, catY, cat.getDisplayName().toUpperCase(), selectedCategory == cat, mouseX, mouseY);
        }

        // 6. Scrollable Module List
        if (this.moduleList != null) {
            this.moduleList.render(context, this.textRenderer, mouseX, mouseY, deltaSeconds);
        }

        // 7. Right-Click Side Customization Panel (Liquid Glass Drawer)
        if (this.activeSettingsModule != null) {
            renderSideSettingsDrawer(context, mouseX, mouseY);
        }

        // 8. Footer Status & Resize Corner Handle
        int footerY = winY + winH - FOOTER_HEIGHT;
        context.fill(winX + 1, footerY, winX + winW - 1, winY + winH - 1, 0xAA090D16);
        context.fill(winX + 1, footerY, winX + winW - 1, footerY + 1, 0x33FFFFFF);

        String hint = "L-Click: Toggle  |  R-Click: Customize  |  Drag Header to Move";
        context.drawTextWithShadow(this.textRenderer, hint, winX + 8, footerY + 7, 0xFF94A3B8);

        // Resize Grip Icon in Bottom-Right Corner
        context.drawTextWithShadow(this.textRenderer, "◢", winX + winW - 10, winY + winH - 11,
                resizingWindow ? 0xFF00E5FF : 0xAA38BDF8);
    }

    private void renderSideSettingsDrawer(DrawContext context, int mouseX, int mouseY) {
        int drawerX = winX + Math.max(126, (int) (winW * 0.50f));
        int drawerY = winY + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 4;
        int drawerW = winX + winW - drawerX - 6;
        int drawerH = Math.max(40, winH - HEADER_HEIGHT - CATEGORY_BAR_HEIGHT - FOOTER_HEIGHT - 8);

        // Frosted Glass Drawer Panel
        context.fill(drawerX, drawerY, drawerX + drawerW, drawerY + drawerH, 0xCC111827);
        context.fill(drawerX, drawerY, drawerX + 1, drawerY + drawerH, 0xAA00E5FF);
        context.fill(drawerX, drawerY, drawerX + drawerW, drawerY + 1, 0x6638BDF8);

        context.enableScissor(drawerX, drawerY, drawerX + drawerW, drawerY + drawerH);

        // Drawer Header: Module Name + Reset Button
        context.drawTextWithShadow(this.textRenderer, activeSettingsModule.getDisplayName(), drawerX + 8, drawerY + 6, 0xFF00E5FF);
        int resetW = 38;
        int resetX = drawerX + drawerW - resetW - 6;
        drawGlassPillButton(context, resetX, drawerY + 4, resetW, 14, "Reset", false, mouseX, mouseY);

        int rowY = drawerY + 24;
        int controlW = drawerW - 16;
        int controlX = drawerX + 8;

        for (ModuleSetting<?> setting : activeSettingsModule.getSettings()) {
            if (rowY + 22 > drawerY + drawerH) break;

            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, controlX, rowY, controlW, 18);
                context.fill(controlX, rowY, controlX + controlW, rowY + 18, hovered ? 0x991E293B : 0x660F172A);
                context.fill(controlX, rowY, controlX + controlW, rowY + 1, 0x33FFFFFF);
                String name = this.textRenderer.trimToWidth(boolSetting.getDisplayName(), Math.max(40, controlW - 36));
                context.drawTextWithShadow(this.textRenderer, name, controlX + 5, rowY + 5, 0xFFE2E8F0);

                String state = boolSetting.getValue() ? "ON" : "OFF";
                int stateColor = boolSetting.getValue() ? 0xFF00E5FF : 0xFF64748B;
                context.drawTextWithShadow(this.textRenderer, state,
                        controlX + controlW - this.textRenderer.getWidth(state) - 5, rowY + 5, stateColor);
                rowY += 21;

            } else if (setting instanceof ModuleSetting.IntSliderSetting slider) {
                context.fill(controlX, rowY, controlX + controlW, rowY + 22, 0x660F172A);
                String label = slider.getDisplayName() + ": " + slider.formatValue();
                context.drawTextWithShadow(this.textRenderer,
                        this.textRenderer.trimToWidth(label, controlW - 8), controlX + 4, rowY + 3, 0xFFE2E8F0);

                int barX = controlX + 4;
                int barY = rowY + 15;
                int barW = controlW - 8;
                double ratio = (double) (slider.getValue() - slider.getMin()) / (double) Math.max(1, slider.getMax() - slider.getMin());
                int fillW = (int) Math.round(ratio * barW);

                context.fill(barX, barY, barX + barW, barY + 3, 0xFF1E293B);
                context.fill(barX, barY, barX + fillW, barY + 3, 0xFF00E5FF);
                context.fill(barX + fillW - 2, barY - 1, barX + fillW + 2, barY + 4, 0xFFFFFFFF);
                rowY += 25;

            } else if (setting instanceof ModuleSetting.CycleSetting cycle) {
                boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, controlX, rowY, controlW, 18);
                context.fill(controlX, rowY, controlX + controlW, rowY + 18, hovered ? 0x991E293B : 0x660F172A);
                String text = cycle.getDisplayName() + ": " + cycle.formatValue();
                context.drawTextWithShadow(this.textRenderer,
                        this.textRenderer.trimToWidth(text, controlW - 10), controlX + 5, rowY + 5, 0xFFE2E8F0);
                rowY += 21;

            } else if (setting instanceof ModuleSetting.TextSetting textSetting) {
                boolean focused = (this.focusedTextSetting == textSetting);
                int borderColor = focused ? 0xFF00E5FF : 0x5594A3B8;
                context.fill(controlX, rowY, controlX + controlW, rowY + 20, borderColor);
                context.fill(controlX + 1, rowY + 1, controlX + controlW - 1, rowY + 19, 0xCC090D16);

                String cursor = (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) ? "_" : "";
                String display = textSetting.getDisplayName() + ": " + textSetting.getValue() + cursor;
                context.drawTextWithShadow(this.textRenderer,
                        this.textRenderer.trimToWidth(display, controlW - 10), controlX + 5, rowY + 6,
                        focused ? 0xFF00E5FF : 0xFFCBD5E1);
                rowY += 23;
            }
        }

        context.disableScissor();
    }

    private void drawGlassPillButton(DrawContext context, int x, int y, int w, int h, String label, boolean active, int mouseX, int mouseY) {
        boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, x, y, w, h);
        int border = active ? 0xFF00E5FF : (hovered ? 0xAA38BDF8 : 0x5564748B);
        int fill = active ? 0xCC0284C7 : (hovered ? 0xAA1E293B : 0x770F172A);
        context.fill(x, y, x + w, y + h, border);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        int tw = this.textRenderer.getWidth(label);
        context.drawTextWithShadow(this.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    private int drawCategoryTab(DrawContext context, int x, int y, String title, boolean selected, int mouseX, int mouseY) {
        int tabW = this.textRenderer.getWidth(title) + 12;
        int tabH = 16;
        boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, x, y, tabW, tabH);
        int bg = selected ? 0xCC0284C7 : (hovered ? 0x881E293B : 0x440F172A);
        int bottomLine = selected ? 0xFF00E5FF : 0x3364748B;
        context.fill(x, y, x + tabW, y + tabH, bg);
        context.fill(x, y + tabH - 1, x + tabW, y + tabH, bottomLine);
        context.drawTextWithShadow(this.textRenderer, title, x + 6, y + 4, selected ? 0xFFFFFFFF : 0xFF94A3B8);
        return x + tabW + 4;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        // 1. Check Bottom-Right Resize Handle
        if (button == 0 && mx >= winX + winW - RESIZE_HANDLE_SIZE && mx <= winX + winW + 2
                && my >= winY + winH - RESIZE_HANDLE_SIZE && my <= winY + winH + 2) {
            this.resizingWindow = true;
            this.resizeStartW = this.winW;
            this.resizeStartH = this.winH;
            this.resizeStartMouseX = mx;
            this.resizeStartMouseY = my;
            return true;
        }

        // 2. Check Top Header Buttons & Drag Bar
        if (mx >= winX && mx <= winX + winW && my >= winY && my <= winY + HEADER_HEIGHT) {
            int closeBtnX = winX + winW - 22;
            int safeBtnX = closeBtnX - 48;
            int maxFastBtnX = safeBtnX - 62;
            int hdrBtnY = winY + 5;

            if (ModuleButton.isMouseOver(mx, my, closeBtnX, hdrBtnY, 16, 16)) {
                playClickSound();
                close();
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, safeBtnX, hdrBtnY, 44, 16)) {
                playClickSound();
                OptimizerConfig cfg = OptimizerConfig.getInstance();
                cfg.setSafeMode(!cfg.isSafeMode());
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, maxFastBtnX, hdrBtnY, 58, 16)) {
                playClickSound();
                OptimizerConfig.getInstance().applyMaximumResponsivenessProfile();
                return true;
            }

            if (button == 0) {
                this.draggingWindow = true;
                this.dragOffsetX = (int) mx - this.winX;
                this.dragOffsetY = (int) my - this.winY;
                return true;
            }
        }

        // 3. Check Category Tabs
        int catY = winY + HEADER_HEIGHT + 3;
        int catX = winX + 6;
        int allW = this.textRenderer.getWidth("ALL") + 12;
        if (ModuleButton.isMouseOver(mx, my, catX, catY, allW, 16)) {
            playClickSound();
            this.selectedCategory = null;
            this.savedScrollOffset = 0.0;
            rebuildModuleList();
            return true;
        }
        catX += allW + 4;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int tw = this.textRenderer.getWidth(cat.getDisplayName().toUpperCase()) + 12;
            if (ModuleButton.isMouseOver(mx, my, catX, catY, tw, 16)) {
                playClickSound();
                this.selectedCategory = cat;
                this.savedScrollOffset = 0.0;
                rebuildModuleList();
                return true;
            }
            catX += tw + 4;
        }

        // 4. Check Side Settings Drawer Controls
        if (this.activeSettingsModule != null && handleSideDrawerClick(mx, my, button)) {
            return true;
        }

        // 5. Check Scrollable Module List
        if (this.moduleList != null && this.moduleList.mouseClicked(mx, my, button)) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean handleSideDrawerClick(double mx, double my, int button) {
        int drawerX = winX + Math.max(126, (int) (winW * 0.50f));
        int drawerY = winY + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 4;
        int drawerW = winX + winW - drawerX - 6;
        int drawerH = Math.max(40, winH - HEADER_HEIGHT - CATEGORY_BAR_HEIGHT - FOOTER_HEIGHT - 8);

        if (!ModuleButton.isMouseOver(mx, my, drawerX, drawerY, drawerW, drawerH)) {
            this.focusedTextSetting = null;
            return false;
        }

        int resetW = 38;
        int resetX = drawerX + drawerW - resetW - 6;
        if (ModuleButton.isMouseOver(mx, my, resetX, drawerY + 4, resetW, 14)) {
            playClickSound();
            activeSettingsModule.resetDefaults();
            OptimizerConfig.getInstance().save();
            return true;
        }

        int rowY = drawerY + 24;
        int controlW = drawerW - 16;
        int controlX = drawerX + 8;
        this.focusedTextSetting = null;

        for (ModuleSetting<?> setting : activeSettingsModule.getSettings()) {
            if (rowY + 22 > drawerY + drawerH) break;

            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                if (ModuleButton.isMouseOver(mx, my, controlX, rowY, controlW, 18)) {
                    playClickSound();
                    boolSetting.toggle();
                    OptimizerConfig.getInstance().save();
                    return true;
                }
                rowY += 21;
            } else if (setting instanceof ModuleSetting.IntSliderSetting slider) {
                if (ModuleButton.isMouseOver(mx, my, controlX, rowY, controlW, 22)) {
                    this.activeDraggingSlider = slider;
                    updateSliderFromMouse(slider, mx, controlX + 4, controlW - 8);
                    return true;
                }
                rowY += 25;
            } else if (setting instanceof ModuleSetting.CycleSetting cycle) {
                if (ModuleButton.isMouseOver(mx, my, controlX, rowY, controlW, 18)) {
                    playClickSound();
                    cycle.cycleNext();
                    OptimizerConfig.getInstance().save();
                    return true;
                }
                rowY += 21;
            } else if (setting instanceof ModuleSetting.TextSetting textSetting) {
                if (ModuleButton.isMouseOver(mx, my, controlX, rowY, controlW, 20)) {
                    playClickSound();
                    if (button == 1) {
                        textSetting.setValue("");
                    }
                    this.focusedTextSetting = textSetting;
                    return true;
                }
                rowY += 23;
            }
        }
        return true;
    }

    private void updateSliderFromMouse(ModuleSetting.IntSliderSetting slider, double mx, int barX, int barW) {
        double ratio = MathHelper.clamp((mx - barX) / (double) Math.max(1, barW), 0.0, 1.0);
        int newVal = slider.getMin() + (int) Math.round(ratio * (slider.getMax() - slider.getMin()));
        slider.setValue(newVal);
        OptimizerConfig.getInstance().save();
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mx = click.x();
        double my = click.y();

        if (this.draggingWindow) {
            this.winX = MathHelper.clamp((int) mx - this.dragOffsetX, 2, Math.max(2, this.width - this.winW - 2));
            this.winY = MathHelper.clamp((int) my - this.dragOffsetY, 2, Math.max(2, this.height - this.winH - 2));
            OptimizerConfig.getInstance().saveWindowBounds(this.winX, this.winY, this.winW, this.winH);
            updateLayoutBounds();
            return true;
        }

        if (this.resizingWindow) {
            int minW = Math.min(280, this.width - 12);
            int minH = Math.min(180, this.height - 12);
            int maxW = Math.max(minW, this.width - this.winX - 4);
            int maxH = Math.max(minH, this.height - this.winY - 4);

            this.winW = MathHelper.clamp(this.resizeStartW + (int) (mx - this.resizeStartMouseX), minW, maxW);
            this.winH = MathHelper.clamp(this.resizeStartH + (int) (my - this.resizeStartMouseY), minH, maxH);
            OptimizerConfig.getInstance().saveWindowBounds(this.winX, this.winY, this.winW, this.winH);
            updateLayoutBounds();
            return true;
        }

        if (this.activeDraggingSlider != null && this.activeSettingsModule != null) {
            int drawerX = winX + Math.max(126, (int) (winW * 0.50f));
            int drawerW = winX + winW - drawerX - 6;
            updateSliderFromMouse(this.activeDraggingSlider, mx, drawerX + 12, drawerW - 24);
            return true;
        }

        if (this.moduleList != null && this.moduleList.mouseDragged(mx, my, click.button())) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.draggingWindow = false;
        this.resizingWindow = false;
        this.activeDraggingSlider = null;
        if (this.moduleList != null && this.moduleList.mouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.moduleList != null && this.moduleList.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.focusedTextSetting != null) {
            int key = input.key();
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                this.focusedTextSetting = null;
                OptimizerConfig.getInstance().save();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String current = this.focusedTextSetting.getValue();
                if (!current.isEmpty()) {
                    this.focusedTextSetting.setValue(current.substring(0, current.length() - 1));
                    OptimizerConfig.getInstance().save();
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (this.focusedTextSetting != null) {
            String current = this.focusedTextSetting.getValue();
            String typed = input.asString();
            if (!typed.isEmpty() && current.length() < this.focusedTextSetting.getMaxLength()) {
                this.focusedTextSetting.setValue(current + typed);
                OptimizerConfig.getInstance().save();
                return true;
            }
        }
        return super.charTyped(input);
    }

    private static void playClickSound() {
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        OptimizerConfig.getInstance().saveWindowBounds(this.winX, this.winY, this.winW, this.winH);
        OptimizerConfig.getInstance().save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
