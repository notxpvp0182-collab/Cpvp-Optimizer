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
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Premium Dark Liquid-Glass / Frosted-Glass Interface (Reverse-Engineered from Screenshots 1-6):
 *
 * - ViewMode.HUB (Screenshots 2 & 3):
 *   Centered "CPVP Optimizer" title above 5 Frosted-Glass Category Tiles with hover/selected elevation
 *   and the 4-tile Bottom Glass Dock (Hub, Search, Telemetry, Profiles).
 *
 * - ViewMode.CATEGORY_PANEL (Screenshot 4 — Default on Open):
 *   Centered Frosted-Glass Module Panel for the active category (with compact rows & monochrome pill switches),
 *   flanked symmetrically on the Left (2 tiles) and Right (2 tiles) by the remaining 4 Category Tiles!
 *   Right-clicking any module transitions the center panel into that module's fixed-grid Settings Inspector.
 *
 * - ViewMode.TELEMETRY (Screenshot 6):
 *   Multi-panel glass window with top metric cards, frame-stability progress bar, active fast-path milestone bars,
 *   and a right-side live module diagnostics list.
 *
 * - ViewMode.PROFILES (Screenshot 5):
 *   3-column multi-panel glass window with left category navigation, center live Responsiveness Architecture Inspector,
 *   and right 2x2 Profile & Global Control cards.
 */
public final class OptimizerScreen extends Screen {

    private enum ViewMode {
        HUB,
        CATEGORY_PANEL,
        TELEMETRY,
        PROFILES
    }

    private final Screen parent;
    private ViewMode viewMode = ViewMode.CATEGORY_PANEL;
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private OptimizerModule activeSettingsModule = null;
    private ScrollableModuleList moduleList;
    private double savedScrollOffset = 0.0;

    // Search Bar State
    private boolean searchBarOpen = false;
    private String searchQuery = "";

    // Smooth Animation States
    private float screenOpenAnim = 0.0f;
    private float panelTransitionAnim = 1.0f;
    private final float[] categoryHoverAnims = new float[ModuleCategory.values().length];
    private final float[] dockHoverAnims = new float[4];

    // Settings Inspector Interaction States
    private ModuleSetting.IntSliderSetting activeDraggingSlider = null;
    private ModuleSetting.TextSetting focusedTextSetting = null;

    public OptimizerScreen(Screen parent) {
        super(Text.literal("CPVP Optimizer"));
        this.parent = parent;
    }

    public OptimizerScreen(Screen parent, ModuleCategory retainedCategory, double retainedScrollOffset) {
        this(parent);
        if (retainedCategory != null) {
            this.selectedCategory = retainedCategory;
            this.viewMode = ViewMode.CATEGORY_PANEL;
        }
        this.savedScrollOffset = retainedScrollOffset;
    }

    @Override
    protected void init() {
        super.init();
        rebuildModuleList();
    }

    private void rebuildModuleList() {
        List<OptimizerModule> source;
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (searchBarOpen && !q.isEmpty()) {
            source = ModuleRegistry.getInstance().getAllModules();
        } else {
            source = ModuleRegistry.getInstance().getByCategory(selectedCategory);
        }

        List<OptimizerModule> filtered = new ArrayList<>();
        for (OptimizerModule m : source) {
            if (q.isEmpty()
                    || m.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(m);
            }
        }

        this.moduleList = new ScrollableModuleList(filtered, module -> {
            this.activeSettingsModule = module;
            this.focusedTextSetting = null;
            this.panelTransitionAnim = 0.0f;
        });
        this.moduleList.setSelectedModule(this.activeSettingsModule);
        updateCenterListBounds();
        this.moduleList.setScrollAmountImmediate(savedScrollOffset);
    }

    private void updateCenterListBounds() {
        if (this.moduleList == null) return;
        int panelW = getCenterPanelWidth();
        int panelH = getCenterPanelHeight();
        int panelX = (this.width - panelW) / 2;
        int panelY = getCenterPanelY(panelH);

        int listX = panelX + 10;
        int listY = panelY + 54;
        int listW = panelW - 20;
        int listH = Math.max(40, panelH - 62);
        this.moduleList.setDimensions(listX, listY, listW, listH);
    }

    private int getCenterPanelWidth() {
        return Math.min(252, Math.max(196, (int) (this.width * 0.34f)));
    }

    private int getCenterPanelHeight() {
        return Math.min(216, Math.max(154, (int) (this.height * 0.56f)));
    }

    private int getCenterPanelY(int panelH) {
        return Math.max(32, (this.height - panelH) / 2 - 8);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Handled in render() via GlassRenderer.renderFrostedBackdrop
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float dt = Math.min(0.1f, delta / 20.0f);
        this.screenOpenAnim = UIAnimations.approach(this.screenOpenAnim, 1.0f, dt, 14.0f);
        this.panelTransitionAnim = UIAnimations.approach(this.panelTransitionAnim, 1.0f, dt, 14.0f);

        // 1. Frosted Blur + Atmospheric Dark Vignette Backdrop
        GlassRenderer.renderFrostedBackdrop(ctx, this.width, this.height, this.screenOpenAnim);

        if (this.viewMode == ViewMode.HUB) {
            renderCategoryHubView(ctx, mouseX, mouseY, dt);
            renderBottomDock(ctx, mouseX, mouseY, dt);
        } else if (this.viewMode == ViewMode.CATEGORY_PANEL) {
            renderExpandedCategoryStage(ctx, mouseX, mouseY, dt);
            renderBottomDock(ctx, mouseX, mouseY, dt);
        } else if (this.viewMode == ViewMode.TELEMETRY) {
            renderTelemetryWindow(ctx, mouseX, mouseY);
        } else if (this.viewMode == ViewMode.PROFILES) {
            renderProfilesWindow(ctx, mouseX, mouseY);
        }
    }

    /**
     * SCREENSHOTS 2 & 3: Centered 5-Tile Category Hub View.
     */
    private void renderCategoryHubView(DrawContext ctx, int mouseX, int mouseY, float dt) {
        ModuleCategory[] cats = ModuleCategory.values();
        int tileW = 64;
        int tileH = 74;
        int gap = 10;
        int totalW = cats.length * tileW + (cats.length - 1) * gap;
        int startX = (this.width - totalW) / 2;
        int baseY = (this.height - tileH) / 2 - 6;

        // Centered Clean Title above the Category Hub
        String brand = "CPVP Optimizer";
        ctx.drawTextWithShadow(this.textRenderer, brand,
                (this.width - this.textRenderer.getWidth(brand)) / 2, baseY - 26, PremiumTheme.TEXT_PRIMARY);

        for (int i = 0; i < cats.length; i++) {
            ModuleCategory cat = cats[i];
            int tx = startX + i * (tileW + gap);
            boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, tx, baseY, tileW, tileH);
            boolean selected = (cat == selectedCategory);

            categoryHoverAnims[i] = UIAnimations.approach(categoryHoverAnims[i], (hovered || selected) ? 1.0f : 0.0f, dt, 14.0f);
            float anim = categoryHoverAnims[i];

            int expandX = Math.round(anim * 3.0f);
            int expandY = Math.round(anim * 5.0f);
            int rx = tx - expandX;
            int ry = baseY - expandY;
            int rw = tileW + expandX * 2;
            int rh = tileH + expandY * 2;

            int fill = PremiumTheme.lerpColor(anim, PremiumTheme.GLASS_PRIMARY, PremiumTheme.GLASS_SELECTED);
            int border = PremiumTheme.lerpColor(anim, PremiumTheme.BORDER_SUBTLE, PremiumTheme.BORDER_SELECTED);
            GlassRenderer.drawGlassPanel(ctx, rx, ry, rw, rh, fill, border, true);

            int iconColor = PremiumTheme.lerpColor(anim, PremiumTheme.TEXT_SECONDARY, PremiumTheme.TEXT_PRIMARY);
            CustomModuleIcons.drawCategoryIcon(ctx, cat, rx + (rw - 16) / 2, ry + 18, iconColor);

            String label = cat.getDisplayName();
            int lw = this.textRenderer.getWidth(label);
            ctx.drawText(this.textRenderer, label, rx + (rw - lw) / 2, ry + 44, iconColor, false);

            // Subtle horizontal dash indicator under category title
            int dashW = Math.round(8 + anim * 6);
            int dashColor = PremiumTheme.lerpColor(anim, 0x33FFFFFF, 0xAAFFFFFF);
            ctx.fill(rx + (rw - dashW) / 2, ry + 56, rx + (rw + dashW) / 2, ry + 57, dashColor);
        }
    }

    /**
     * SCREENSHOT 4: Center Frosted-Glass Module/Settings Panel flanked by 2 Left & 2 Right Category Tiles.
     */
    private void renderExpandedCategoryStage(DrawContext ctx, int mouseX, int mouseY, float dt) {
        updateCenterListBounds();
        int panelW = getCenterPanelWidth();
        int panelH = getCenterPanelHeight();
        int panelX = (this.width - panelW) / 2;
        int panelY = getCenterPanelY(panelH);

        // Top Centered Brand Title ("CPVP Optimizer")
        String brand = "CPVP Optimizer";
        ctx.drawTextWithShadow(this.textRenderer, brand,
                (this.width - this.textRenderer.getWidth(brand)) / 2, panelY - 18, PremiumTheme.TEXT_PRIMARY);

        // 1. Render the 4 Flanking Category Tiles (2 on Left, 2 on Right) when screen width allows
        List<ModuleCategory> sideCategories = new ArrayList<>();
        for (ModuleCategory cat : ModuleCategory.values()) {
            if (cat != selectedCategory) {
                sideCategories.add(cat);
            }
        }

        int sideTileW = 54;
        int sideTileH = 62;
        int sideGap = 8;
        int sideY = panelY + (panelH - sideTileH) / 2;

        if (panelX - (sideTileW * 2 + sideGap * 2 + 8) >= 4) {
            for (int i = 0; i < sideCategories.size(); i++) {
                ModuleCategory sideCat = sideCategories.get(i);
                int tx;
                if (i < 2) {
                    // Left 2 tiles
                    tx = panelX - 12 - (2 - i) * sideTileW - (1 - i) * sideGap;
                } else {
                    // Right 2 tiles
                    int rIdx = i - 2;
                    tx = panelX + panelW + 12 + rIdx * (sideTileW + sideGap);
                }

                int catOrd = sideCat.ordinal();
                boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, tx, sideY, sideTileW, sideTileH);
                categoryHoverAnims[catOrd] = UIAnimations.approach(categoryHoverAnims[catOrd], hovered ? 1.0f : 0.0f, dt, 15.0f);
                float anim = categoryHoverAnims[catOrd];

                int fill = PremiumTheme.lerpColor(anim, PremiumTheme.GLASS_PRIMARY, PremiumTheme.GLASS_HOVER);
                int border = PremiumTheme.lerpColor(anim, PremiumTheme.BORDER_SUBTLE, PremiumTheme.BORDER_HOVER);
                GlassRenderer.drawGlassPanel(ctx, tx, sideY, sideTileW, sideTileH, fill, border, true);

                int iconColor = PremiumTheme.lerpColor(anim, PremiumTheme.TEXT_SECONDARY, PremiumTheme.TEXT_PRIMARY);
                CustomModuleIcons.drawCategoryIcon(ctx, sideCat, tx + (sideTileW - 16) / 2, sideY + 14, iconColor);

                String name = this.textRenderer.trimToWidth(sideCat.getDisplayName(), sideTileW - 6);
                ctx.drawText(this.textRenderer, name,
                        tx + (sideTileW - this.textRenderer.getWidth(name)) / 2, sideY + 38, iconColor, false);
                ctx.fill(tx + (sideTileW - 8) / 2, sideY + 49, tx + (sideTileW + 8) / 2, sideY + 50, 0x33FFFFFF);
            }
        }

        // 2. Render Center Frosted-Glass Panel
        GlassRenderer.drawGlassPanel(ctx, panelX, panelY, panelW, panelH,
                PremiumTheme.GLASS_PRIMARY, PremiumTheme.BORDER_SELECTED, true);

        // Panel Top Header: Category Icon + Title + Tiny Underline (Exactly like "HUD" in Screenshot 4)
        CustomModuleIcons.drawCategoryIcon(ctx, selectedCategory, panelX + (panelW - 16) / 2, panelY + 6, PremiumTheme.TEXT_PRIMARY);
        String headerTitle = activeSettingsModule != null
                ? activeSettingsModule.getDisplayName().replace(" Optimizer", "")
                : selectedCategory.getDisplayName().toUpperCase(Locale.ROOT);
        int htw = this.textRenderer.getWidth(headerTitle);
        ctx.drawText(this.textRenderer, headerTitle, panelX + (panelW - htw) / 2, panelY + 23, PremiumTheme.TEXT_PRIMARY, false);
        ctx.fill(panelX + (panelW - 12) / 2, panelY + 33, panelX + (panelW + 12) / 2, panelY + 34, 0x55FFFFFF);

        // Sub-Header Full-Width Pill Bar (Matches "Edit HUD" pill in Screenshot 4)
        int subBarX = panelX + 10;
        int subBarY = panelY + 37;
        int subBarW = panelW - 20;
        int subBarH = 13;
        boolean subHovered = ModuleButton.isMouseOver(mouseX, mouseY, subBarX, subBarY, subBarW, subBarH);

        GlassRenderer.drawGlassPanel(ctx, subBarX, subBarY, subBarW, subBarH,
                subHovered ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_SECONDARY,
                subHovered ? PremiumTheme.BORDER_HOVER : PremiumTheme.BORDER_SUBTLE, false);

        String subBarText;
        if (activeSettingsModule != null) {
            subBarText = "← Back to " + selectedCategory.getDisplayName();
        } else if (searchBarOpen) {
            String cursor = ((System.currentTimeMillis() / 500L) % 2L == 0L) ? "|" : "";
            subBarText = "Search: " + (searchQuery.isEmpty() ? "Type module name..." : searchQuery) + cursor;
        } else {
            subBarText = "Right-Click Any Module to Configure";
        }
        int stw = this.textRenderer.getWidth(subBarText);
        ctx.drawText(this.textRenderer, subBarText,
                subBarX + (subBarW - stw) / 2, subBarY + 3, PremiumTheme.TEXT_SECONDARY, false);

        // 3. Panel Body: Either Compact Module List OR Fixed-Grid Module Settings Inspector
        if (activeSettingsModule != null) {
            renderModuleSettingsInspector(ctx, panelX + 10, panelY + 54, panelW - 20, panelH - 60, mouseX, mouseY);
        } else if (this.moduleList != null) {
            this.moduleList.render(ctx, this.textRenderer, mouseX, mouseY, dt);
        }
    }

    /**
     * Fixed-Grid Settings Inspector inside the Center Frosted-Glass Panel (Sections 8 & 9).
     */
    private void renderModuleSettingsInspector(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        ctx.enableScissor(x, y, x + w, y + h);

        int curY = y + 2;
        // 1. Master Module Enable Row
        boolean enableHover = ModuleButton.isMouseOver(mouseX, mouseY, x, curY, w, 20);
        GlassRenderer.drawGlassPanel(ctx, x, curY, w, 20,
                enableHover ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_ELEVATED,
                PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Module Enabled", x + 8, curY + 6, PremiumTheme.TEXT_PRIMARY, false);
        GlassRenderer.drawFrostedSwitch(ctx, x + w - 28, curY + 4, 22, 11, activeSettingsModule.isEnabled() ? 1.0f : 0.0f);
        curY += 24;

        // 2. Dynamic Settings Rows (Fixed Grid Alignment)
        for (ModuleSetting<?> setting : activeSettingsModule.getSettings()) {
            if (curY + 20 > y + h - 20) break;
            boolean rowHover = ModuleButton.isMouseOver(mouseX, mouseY, x, curY, w, 20);

            GlassRenderer.drawGlassPanel(ctx, x, curY, w, 20,
                    rowHover ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_ELEVATED,
                    PremiumTheme.BORDER_SUBTLE, false);

            String label = this.textRenderer.trimToWidth(setting.getDisplayName(), Math.max(40, w - 88));
            ctx.drawText(this.textRenderer, label, x + 8, curY + 6, PremiumTheme.TEXT_SECONDARY, false);

            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                GlassRenderer.drawFrostedSwitch(ctx, x + w - 28, curY + 4, 22, 11, boolSetting.getValue() ? 1.0f : 0.0f);
            } else if (setting instanceof ModuleSetting.IntSliderSetting slider) {
                int barW = 54;
                int barX = x + w - barW - 8;
                int barY = curY + 12;
                String valStr = slider.formatValue();
                ctx.drawText(this.textRenderer, valStr,
                        barX + (barW - this.textRenderer.getWidth(valStr)) / 2, curY + 2, PremiumTheme.TEXT_PRIMARY, false);

                float ratio = (float) (slider.getValue() - slider.getMin()) / (float) Math.max(1, slider.getMax() - slider.getMin());
                ctx.fill(barX, barY, barX + barW, barY + 2, 0x44FFFFFF);
                int fillW = Math.round(ratio * barW);
                ctx.fill(barX, barY, barX + fillW, barY + 2, PremiumTheme.TEXT_PRIMARY);
                ctx.fill(barX + fillW - 2, barY - 2, barX + fillW + 2, barY + 4, PremiumTheme.ACCENT_WHITE);
            } else if (setting instanceof ModuleSetting.CycleSetting cycle) {
                String val = "[ " + cycle.formatValue() + " ]";
                ctx.drawText(this.textRenderer, val,
                        x + w - this.textRenderer.getWidth(val) - 6, curY + 6, PremiumTheme.TEXT_PRIMARY, false);
            } else if (setting instanceof ModuleSetting.TextSetting textSetting) {
                boolean focused = (this.focusedTextSetting == textSetting);
                String cursor = (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) ? "_" : "";
                String val = "[" + textSetting.getValue() + cursor + "]";
                ctx.drawText(this.textRenderer, val,
                        x + w - this.textRenderer.getWidth(val) - 6, curY + 6,
                        focused ? PremiumTheme.ACCENT_WHITE : PremiumTheme.TEXT_PRIMARY, false);
            }
            curY += 23;
        }

        // Bottom Reset Defaults Pill inside Inspector
        int resetY = y + h - 16;
        boolean resetHover = ModuleButton.isMouseOver(mouseX, mouseY, x, resetY, w, 15);
        GlassRenderer.drawGlassPanel(ctx, x, resetY, w, 15,
                resetHover ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_SECONDARY,
                PremiumTheme.BORDER_SUBTLE, false);
        String resetTxt = "Reset Module Defaults";
        ctx.drawText(this.textRenderer, resetTxt,
                x + (w - this.textRenderer.getWidth(resetTxt)) / 2, resetY + 4, PremiumTheme.TEXT_SECONDARY, false);

        ctx.disableScissor();
    }

    /**
     * Bottom 4-Tile Frosted-Glass Dock (Screenshots 2, 3, 4: Hub, Search, Telemetry, Profiles).
     */
    private void renderBottomDock(DrawContext ctx, int mouseX, int mouseY, float dt) {
        String[] labels = {"Hub", "Search", "Telemetry", "Profiles"};
        int btnW = 42;
        int btnH = 30;
        int gap = 6;
        int totalW = labels.length * btnW + (labels.length - 1) * gap;
        int startX = (this.width - totalW) / 2;
        int dockY = Math.min(this.height - btnH - 8, getCenterPanelY(getCenterPanelHeight()) + getCenterPanelHeight() + 10);

        for (int i = 0; i < labels.length; i++) {
            int bx = startX + i * (btnW + gap);
            boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, bx, dockY, btnW, btnH);
            boolean active = (i == 0 && viewMode == ViewMode.HUB)
                    || (i == 1 && searchBarOpen)
                    || (i == 2 && viewMode == ViewMode.TELEMETRY)
                    || (i == 3 && viewMode == ViewMode.PROFILES);

            dockHoverAnims[i] = UIAnimations.approach(dockHoverAnims[i], (hovered || active) ? 1.0f : 0.0f, dt, 15.0f);
            float anim = dockHoverAnims[i];

            int fill = PremiumTheme.lerpColor(anim, PremiumTheme.GLASS_PRIMARY, PremiumTheme.GLASS_SELECTED);
            int border = PremiumTheme.lerpColor(anim, PremiumTheme.BORDER_SUBTLE, PremiumTheme.BORDER_SELECTED);
            GlassRenderer.drawGlassPanel(ctx, bx, dockY, btnW, btnH, fill, border, true);

            int iconColor = PremiumTheme.lerpColor(anim, PremiumTheme.TEXT_SECONDARY, PremiumTheme.TEXT_PRIMARY);
            CustomModuleIcons.drawDockIcon(ctx, i, bx + (btnW - 10) / 2, dockY + 5, iconColor);

            String lbl = labels[i];
            ctx.drawText(this.textRenderer, lbl,
                    bx + (btnW - this.textRenderer.getWidth(lbl)) / 2, dockY + 18, iconColor, false);
        }
    }

    /**
     * SCREENSHOT 6: Dense Multi-Panel Telemetry & Performance Overview Window.
     */
    private void renderTelemetryWindow(DrawContext ctx, int mouseX, int mouseY) {
        int winW = Math.min(480, this.width - 24);
        int winH = Math.min(268, this.height - 24);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        GlassRenderer.drawGlassPanel(ctx, winX, winY, winW, winH,
                PremiumTheme.GLASS_PRIMARY, PremiumTheme.BORDER_SELECTED, true);

        renderMultiPanelTopBar(ctx, winX, winY, winW, mouseX, mouseY);

        int contentY = winY + 34;
        int leftW = (int) (winW * 0.58f) - 14;
        int leftX = winX + 10;
        int rightX = leftX + leftW + 8;
        int rightW = winX + winW - rightX - 10;
        int bodyH = winH - 44;

        // Left Top: Real-Time Metric Cards (Screenshot 6 Daily Streak Card Row style)
        GlassRenderer.drawGlassPanel(ctx, leftX, contentY, leftW, 66,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Live Performance Metrics", leftX + 8, contentY + 6, PremiumTheme.TEXT_PRIMARY, false);

        MinecraftClient mc = MinecraftClient.getInstance();
        int fps = mc != null ? mc.getCurrentFps() : 0;
        String[] metricTitles = {"FPS", "Frame", "Mode", "Modules", "Status"};
        String[] metricValues = {
                fps + "",
                String.format(Locale.ROOT, "%.1fms", fps > 0 ? 1000.0f / fps : 0.0f),
                OptimizerConfig.getInstance().isSafeMode() ? "Safe" : "Fast",
                getEnabledModuleCount() + "/15",
                "Active"
        };

        int cardGap = 4;
        int cardW = (leftW - 16 - cardGap * 4) / 5;
        for (int i = 0; i < 5; i++) {
            int cx = leftX + 8 + i * (cardW + cardGap);
            int cy = contentY + 18;
            GlassRenderer.drawGlassPanel(ctx, cx, cy, cardW, 42,
                    PremiumTheme.GLASS_ELEVATED, PremiumTheme.BORDER_SUBTLE, false);
            ctx.drawText(this.textRenderer, metricTitles[i],
                    cx + (cardW - this.textRenderer.getWidth(metricTitles[i])) / 2, cy + 6, PremiumTheme.TEXT_MUTED, false);
            ctx.drawText(this.textRenderer, metricValues[i],
                    cx + (cardW - this.textRenderer.getWidth(metricValues[i])) / 2, cy + 22, PremiumTheme.TEXT_PRIMARY, false);
        }

        // Left Middle: Frame Responsiveness Stability Bar (Screenshot 6 Playtime Progress Bar style)
        int midY = contentY + 72;
        GlassRenderer.drawGlassPanel(ctx, leftX, midY, leftW, 34,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Responsiveness Budget   0-Tick Fast-Path Active",
                leftX + 8, midY + 6, PremiumTheme.TEXT_SECONDARY, false);
        GlassRenderer.drawProgressBar(ctx, leftX + 8, midY + 20, leftW - 16, 6,
                OptimizerConfig.getInstance().isSafeMode() ? 0.25f : 1.0f);

        // Left Bottom: Category Activation Breakdown (Screenshot 6 Milestones style)
        int botY = midY + 42;
        int botH = Math.max(50, bodyH - 114);
        GlassRenderer.drawGlassPanel(ctx, leftX, botY, leftW, botH,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Category Optimization Coverage", leftX + 8, botY + 6, PremiumTheme.TEXT_PRIMARY, false);

        int rowY = botY + 20;
        for (ModuleCategory cat : ModuleCategory.values()) {
            if (rowY + 16 > botY + botH - 4) break;
            List<OptimizerModule> mods = ModuleRegistry.getInstance().getByCategory(cat);
            int activeCount = 0;
            for (OptimizerModule m : mods) if (m.isEnabled()) activeCount++;
            float ratio = mods.isEmpty() ? 0.0f : (float) activeCount / (float) mods.size();

            ctx.drawText(this.textRenderer, cat.getDisplayName(), leftX + 8, rowY, PremiumTheme.TEXT_SECONDARY, false);
            GlassRenderer.drawProgressBar(ctx, leftX + 78, rowY + 3, leftW - 130, 4, ratio);
            String stat = activeCount + " / " + mods.size() + " ON";
            ctx.drawText(this.textRenderer, stat, leftX + leftW - this.textRenderer.getWidth(stat) - 8, rowY, PremiumTheme.TEXT_MUTED, false);
            rowY += 16;
        }

        // Right Panel: Live Module Diagnostics History List (Screenshot 6 History Panel style)
        GlassRenderer.drawGlassPanel(ctx, rightX, contentY, rightW, bodyH,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Module Telemetry", rightX + 8, contentY + 6, PremiumTheme.TEXT_PRIMARY, false);

        int histY = contentY + 20;
        for (OptimizerModule mod : ModuleRegistry.getInstance().getAllModules()) {
            if (histY + 14 > contentY + bodyH - 4) break;
            String shortName = this.textRenderer.trimToWidth(mod.getDisplayName().replace(" Optimizer", ""), rightW - 38);
            ctx.drawText(this.textRenderer, shortName, rightX + 8, histY, PremiumTheme.TEXT_SECONDARY, false);

            String status = mod.isEnabled() ? "+ON" : "OFF";
            int stColor = mod.isEnabled() ? PremiumTheme.ACCENT_GREEN : PremiumTheme.TEXT_MUTED;
            ctx.drawText(this.textRenderer, status,
                    rightX + rightW - this.textRenderer.getWidth(status) - 8, histY, stColor, false);
            ctx.fill(rightX + 6, histY + 11, rightX + rightW - 6, histY + 12, 0x12FFFFFF);
            histY += 14;
        }
    }

    /**
     * SCREENSHOT 5: 3-Column Multi-Panel Profiles & Architecture Inspector Window.
     */
    private void renderProfilesWindow(DrawContext ctx, int mouseX, int mouseY) {
        int winW = Math.min(480, this.width - 24);
        int winH = Math.min(268, this.height - 24);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;

        GlassRenderer.drawGlassPanel(ctx, winX, winY, winW, winH,
                PremiumTheme.GLASS_PRIMARY, PremiumTheme.BORDER_SELECTED, true);

        renderMultiPanelTopBar(ctx, winX, winY, winW, mouseX, mouseY);

        int contentY = winY + 34;
        int bodyH = winH - 44;

        // Column 1: Left Category Sidebar (Screenshot 5 Left Sidebar style)
        int sideX = winX + 10;
        int sideW = 88;
        GlassRenderer.drawGlassPanel(ctx, sideX, contentY, sideW, bodyH,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Categories", sideX + 8, contentY + 8, PremiumTheme.TEXT_PRIMARY, false);

        int catY = contentY + 24;
        for (ModuleCategory cat : ModuleCategory.values()) {
            boolean selected = (cat == selectedCategory);
            boolean hover = ModuleButton.isMouseOver(mouseX, mouseY, sideX + 4, catY, sideW - 8, 18);
            if (selected || hover) {
                GlassRenderer.drawGlassPanel(ctx, sideX + 4, catY, sideW - 8, 18,
                        selected ? PremiumTheme.GLASS_SELECTED : PremiumTheme.GLASS_HOVER,
                        PremiumTheme.BORDER_SUBTLE, false);
            }
            List<OptimizerModule> catMods = ModuleRegistry.getInstance().getByCategory(cat);
            ctx.drawText(this.textRenderer, cat.getDisplayName(), sideX + 8, catY + 5,
                    selected ? PremiumTheme.TEXT_PRIMARY : PremiumTheme.TEXT_SECONDARY, false);
            String cnt = String.valueOf(catMods.size());
            ctx.drawText(this.textRenderer, cnt, sideX + sideW - this.textRenderer.getWidth(cnt) - 8, catY + 5,
                    PremiumTheme.TEXT_MUTED, false);
            catY += 20;
        }

        // Column 2: Center Architecture Inspector (Screenshot 5 Center Preview Panel style)
        int centerX = sideX + sideW + 8;
        int centerW = 166;
        GlassRenderer.drawGlassPanel(ctx, centerX, contentY, centerW, bodyH,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Fast-Path Pipeline", centerX + 8, contentY + 8, PremiumTheme.TEXT_PRIMARY, false);

        String[] pipelineSteps = {
                "• Block Place: 0-Tick Cooldown",
                "• Anchor Blast: Instant Detonate",
                "• Glowstone: 0-Tick Charge",
                "• Hotbar: Immediate Slot Sync",
                "• Crystal: Instant Place & Attack",
                "• XP Bottle: 0-Tick Fast Throw",
                "• Elytra/Armor: 1-Click Protected",
                "• Containers: Single-Click Guard"
        };
        int py = contentY + 26;
        for (String step : pipelineSteps) {
            if (py + 14 > contentY + bodyH - 22) break;
            ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(step, centerW - 14),
                    centerX + 8, py, PremiumTheme.TEXT_SECONDARY, false);
            py += 16;
        }

        // Column 3: Right 2x2 Profile Cards Grid (Screenshot 5 Right 2x2 Card Grid style)
        int rightX = centerX + centerW + 8;
        int rightW = winX + winW - rightX - 10;
        GlassRenderer.drawGlassPanel(ctx, rightX, contentY, rightW, bodyH,
                PremiumTheme.GLASS_SECONDARY, PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Select Profile", rightX + 8, contentY + 8, PremiumTheme.TEXT_PRIMARY, false);

        OptimizerConfig cfg = OptimizerConfig.getInstance();
        String[] profTitles = {"Max Fast", "Safe Mode", "Animations", "Reset All"};
        String[] profBadges = {
                (!cfg.isSafeMode() && cfg.isMasterEnabled()) ? "Active" : "Apply",
                cfg.isSafeMode() ? "ON" : "OFF",
                cfg.isUiAnimationsEnabled() ? "ON" : "OFF",
                "Default"
        };

        int gap = 6;
        int cardW = (rightW - 16 - gap) / 2;
        int cardH = (bodyH - 34 - gap) / 2;

        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = rightX + 8 + col * (cardW + gap);
            int cy = contentY + 24 + row * (cardH + gap);
            boolean hover = ModuleButton.isMouseOver(mouseX, mouseY, cx, cy, cardW, cardH);

            GlassRenderer.drawGlassPanel(ctx, cx, cy, cardW, cardH,
                    hover ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_ELEVATED,
                    hover ? PremiumTheme.BORDER_SELECTED : PremiumTheme.BORDER_SUBTLE, false);

            ctx.drawText(this.textRenderer, profBadges[i], cx + 5, cy + 5, PremiumTheme.TEXT_MUTED, false);
            String pt = this.textRenderer.trimToWidth(profTitles[i], cardW - 8);
            ctx.drawText(this.textRenderer, pt,
                    cx + (cardW - this.textRenderer.getWidth(pt)) / 2, cy + cardH - 14, PremiumTheme.TEXT_PRIMARY, false);
        }
    }

    private void renderMultiPanelTopBar(DrawContext ctx, int winX, int winY, int winW, int mouseX, int mouseY) {
        int barY = winY + 8;
        // Left: ← Back button + Tab pills
        boolean backHover = ModuleButton.isMouseOver(mouseX, mouseY, winX + 10, barY, 44, 18);
        GlassRenderer.drawGlassPanel(ctx, winX + 10, barY, 44, 18,
                backHover ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_SECONDARY,
                PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "← Back", winX + 16, barY + 5, PremiumTheme.TEXT_PRIMARY, false);

        // Tab 1: Telemetry
        boolean telActive = (viewMode == ViewMode.TELEMETRY);
        GlassRenderer.drawGlassPanel(ctx, winX + 60, barY, 64, 18,
                telActive ? PremiumTheme.GLASS_SELECTED : PremiumTheme.GLASS_SECONDARY,
                telActive ? PremiumTheme.BORDER_SELECTED : PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Telemetry", winX + 70, barY + 5, PremiumTheme.TEXT_PRIMARY, false);

        // Tab 2: Profiles
        boolean profActive = (viewMode == ViewMode.PROFILES);
        GlassRenderer.drawGlassPanel(ctx, winX + 130, barY, 56, 18,
                profActive ? PremiumTheme.GLASS_SELECTED : PremiumTheme.GLASS_SECONDARY,
                profActive ? PremiumTheme.BORDER_SELECTED : PremiumTheme.BORDER_SUBTLE, false);
        ctx.drawText(this.textRenderer, "Profiles", winX + 140, barY + 5, PremiumTheme.TEXT_PRIMARY, false);

        // Right: Quick Max Responsiveness Pill
        int maxBtnW = 88;
        int maxBtnX = winX + winW - maxBtnW - 10;
        boolean maxHover = ModuleButton.isMouseOver(mouseX, mouseY, maxBtnX, barY, maxBtnW, 18);
        GlassRenderer.drawGlassPanel(ctx, maxBtnX, barY, maxBtnW, 18,
                maxHover ? PremiumTheme.GLASS_HOVER : PremiumTheme.GLASS_SECONDARY,
                PremiumTheme.BORDER_HOVER, false);
        ctx.drawText(this.textRenderer, "⚡ Max Fast", maxBtnX + 14, barY + 5, PremiumTheme.TEXT_PRIMARY, false);
    }

    private int getEnabledModuleCount() {
        int count = 0;
        for (OptimizerModule m : ModuleRegistry.getInstance().getAllModules()) {
            if (m.isEnabled()) count++;
        }
        return count;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        this.focusedTextSetting = null;

        // 1. Check Bottom 4-Tile Dock on HUB and CATEGORY_PANEL views
        if (this.viewMode == ViewMode.HUB || this.viewMode == ViewMode.CATEGORY_PANEL) {
            int btnW = 42;
            int btnH = 30;
            int gap = 6;
            int totalW = 4 * btnW + 3 * gap;
            int startX = (this.width - totalW) / 2;
            int dockY = Math.min(this.height - btnH - 8, getCenterPanelY(getCenterPanelHeight()) + getCenterPanelHeight() + 10);

            for (int i = 0; i < 4; i++) {
                int bx = startX + i * (btnW + gap);
                if (ModuleButton.isMouseOver(mx, my, bx, dockY, btnW, btnH)) {
                    playClickSound();
                    if (i == 0) {
                        this.viewMode = (this.viewMode == ViewMode.HUB) ? ViewMode.CATEGORY_PANEL : ViewMode.HUB;
                        this.activeSettingsModule = null;
                    } else if (i == 1) {
                        this.viewMode = ViewMode.CATEGORY_PANEL;
                        this.activeSettingsModule = null;
                        this.searchBarOpen = !this.searchBarOpen;
                        if (!this.searchBarOpen) this.searchQuery = "";
                        rebuildModuleList();
                    } else if (i == 2) {
                        this.viewMode = ViewMode.TELEMETRY;
                    } else {
                        this.viewMode = ViewMode.PROFILES;
                    }
                    return true;
                }
            }
        }

        // 2. Handle HUB View Tile Clicks
        if (this.viewMode == ViewMode.HUB) {
            ModuleCategory[] cats = ModuleCategory.values();
            int tileW = 64;
            int tileH = 74;
            int gap = 10;
            int totalW = cats.length * tileW + (cats.length - 1) * gap;
            int startX = (this.width - totalW) / 2;
            int baseY = (this.height - tileH) / 2 - 6;

            for (int i = 0; i < cats.length; i++) {
                int tx = startX + i * (tileW + gap);
                if (ModuleButton.isMouseOver(mx, my, tx - 4, baseY - 6, tileW + 8, tileH + 12)) {
                    playClickSound();
                    this.selectedCategory = cats[i];
                    this.activeSettingsModule = null;
                    this.viewMode = ViewMode.CATEGORY_PANEL;
                    this.savedScrollOffset = 0.0;
                    rebuildModuleList();
                    return true;
                }
            }
            return true;
        }

        // 3. Handle Expanded CATEGORY_PANEL View Clicks
        if (this.viewMode == ViewMode.CATEGORY_PANEL) {
            int panelW = getCenterPanelWidth();
            int panelH = getCenterPanelHeight();
            int panelX = (this.width - panelW) / 2;
            int panelY = getCenterPanelY(panelH);

            // Check Flanking Category Tiles (2 Left, 2 Right)
            List<ModuleCategory> sideCategories = new ArrayList<>();
            for (ModuleCategory cat : ModuleCategory.values()) {
                if (cat != selectedCategory) sideCategories.add(cat);
            }
            int sideTileW = 54;
            int sideTileH = 62;
            int sideGap = 8;
            int sideY = panelY + (panelH - sideTileH) / 2;

            if (panelX - (sideTileW * 2 + sideGap * 2 + 8) >= 4) {
                for (int i = 0; i < sideCategories.size(); i++) {
                    int tx = (i < 2)
                            ? (panelX - 12 - (2 - i) * sideTileW - (1 - i) * sideGap)
                            : (panelX + panelW + 12 + (i - 2) * (sideTileW + sideGap));
                    if (ModuleButton.isMouseOver(mx, my, tx, sideY, sideTileW, sideTileH)) {
                        playClickSound();
                        this.selectedCategory = sideCategories.get(i);
                        this.activeSettingsModule = null;
                        this.searchBarOpen = false;
                        this.searchQuery = "";
                        this.savedScrollOffset = 0.0;
                        rebuildModuleList();
                        return true;
                    }
                }
            }

            // Check Center Panel Sub-Header Pill Bar
            int subBarX = panelX + 10;
            int subBarY = panelY + 37;
            int subBarW = panelW - 20;
            if (ModuleButton.isMouseOver(mx, my, subBarX, subBarY, subBarW, 13)) {
                playClickSound();
                if (this.activeSettingsModule != null) {
                    this.activeSettingsModule = null;
                    this.moduleList.setSelectedModule(null);
                } else {
                    this.searchBarOpen = !this.searchBarOpen;
                    if (!this.searchBarOpen) this.searchQuery = "";
                    rebuildModuleList();
                }
                return true;
            }

            // Check Active Settings Inspector or Module List
            if (this.activeSettingsModule != null) {
                if (handleInspectorClick(mx, my, button, panelX + 10, panelY + 54, panelW - 20, panelH - 60)) {
                    return true;
                }
            } else if (this.moduleList != null && this.moduleList.mouseClicked(mx, my, button)) {
                this.savedScrollOffset = this.moduleList.getScrollAmount();
                return true;
            }
            return true;
        }

        // 4. Handle TELEMETRY & PROFILES Multi-Panel Window Clicks
        if (this.viewMode == ViewMode.TELEMETRY || this.viewMode == ViewMode.PROFILES) {
            int winW = Math.min(480, this.width - 24);
            int winH = Math.min(268, this.height - 24);
            int winX = (this.width - winW) / 2;
            int winY = (this.height - winH) / 2;
            int barY = winY + 8;

            if (ModuleButton.isMouseOver(mx, my, winX + 10, barY, 44, 18)) {
                playClickSound();
                this.viewMode = ViewMode.CATEGORY_PANEL;
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, winX + 60, barY, 64, 18)) {
                playClickSound();
                this.viewMode = ViewMode.TELEMETRY;
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, winX + 130, barY, 56, 18)) {
                playClickSound();
                this.viewMode = ViewMode.PROFILES;
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, winX + winW - 98, barY, 88, 18)) {
                playClickSound();
                OptimizerConfig.getInstance().applyMaximumResponsivenessProfile();
                return true;
            }

            if (this.viewMode == ViewMode.PROFILES) {
                int contentY = winY + 34;
                int bodyH = winH - 44;
                int sideX = winX + 10;
                int sideW = 88;

                // Category clicks in left sidebar of Profiles view
                int catY = contentY + 24;
                for (ModuleCategory cat : ModuleCategory.values()) {
                    if (ModuleButton.isMouseOver(mx, my, sideX + 4, catY, sideW - 8, 18)) {
                        playClickSound();
                        this.selectedCategory = cat;
                        this.viewMode = ViewMode.CATEGORY_PANEL;
                        rebuildModuleList();
                        return true;
                    }
                    catY += 20;
                }

                // 2x2 Profile Cards on the right
                int rightX = sideX + sideW + 8 + 166 + 8;
                int rightW = winX + winW - rightX - 10;
                int gap = 6;
                int cardW = (rightW - 16 - gap) / 2;
                int cardH = (bodyH - 34 - gap) / 2;
                OptimizerConfig cfg = OptimizerConfig.getInstance();

                for (int i = 0; i < 4; i++) {
                    int col = i % 2;
                    int row = i / 2;
                    int cx = rightX + 8 + col * (cardW + gap);
                    int cy = contentY + 24 + row * (cardH + gap);
                    if (ModuleButton.isMouseOver(mx, my, cx, cy, cardW, cardH)) {
                        playClickSound();
                        if (i == 0) cfg.applyMaximumResponsivenessProfile();
                        else if (i == 1) cfg.setSafeMode(!cfg.isSafeMode());
                        else if (i == 2) cfg.setUiAnimationsEnabled(!cfg.isUiAnimationsEnabled());
                        else cfg.resetAllToDefaults();
                        return true;
                    }
                }
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean handleInspectorClick(double mx, double my, int button, int x, int y, int w, int h) {
        if (!ModuleButton.isMouseOver(mx, my, x, y, w, h)) return false;

        int curY = y + 2;
        if (ModuleButton.isMouseOver(mx, my, x, curY, w, 20)) {
            playClickSound();
            if (!OptimizerConfig.getInstance().isSafeMode()) {
                activeSettingsModule.toggle();
                OptimizerConfig.getInstance().save();
            }
            return true;
        }
        curY += 24;

        for (ModuleSetting<?> setting : activeSettingsModule.getSettings()) {
            if (curY + 20 > y + h - 20) break;
            if (ModuleButton.isMouseOver(mx, my, x, curY, w, 20)) {
                if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                    playClickSound();
                    boolSetting.toggle();
                    OptimizerConfig.getInstance().save();
                } else if (setting instanceof ModuleSetting.IntSliderSetting slider) {
                    this.activeDraggingSlider = slider;
                    updateSliderFromMouse(slider, mx, x + w - 62, 54);
                } else if (setting instanceof ModuleSetting.CycleSetting cycle) {
                    playClickSound();
                    cycle.cycleNext();
                    OptimizerConfig.getInstance().save();
                } else if (setting instanceof ModuleSetting.TextSetting textSetting) {
                    playClickSound();
                    if (button == 1) textSetting.setValue("");
                    this.focusedTextSetting = textSetting;
                }
                return true;
            }
            curY += 23;
        }

        int resetY = y + h - 16;
        if (ModuleButton.isMouseOver(mx, my, x, resetY, w, 15)) {
            playClickSound();
            activeSettingsModule.resetDefaults();
            OptimizerConfig.getInstance().save();
            return true;
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

        if (this.activeDraggingSlider != null && this.activeSettingsModule != null) {
            int panelW = getCenterPanelWidth();
            int panelX = (this.width - panelW) / 2;
            int inspX = panelX + 10;
            int inspW = panelW - 20;
            updateSliderFromMouse(this.activeDraggingSlider, mx, inspX + inspW - 62, 54);
            return true;
        }

        if (this.viewMode == ViewMode.CATEGORY_PANEL && this.activeSettingsModule == null && this.moduleList != null) {
            if (this.moduleList.mouseDragged(mx, my, click.button())) {
                this.savedScrollOffset = this.moduleList.getScrollAmount();
                return true;
            }
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.activeDraggingSlider = null;
        if (this.moduleList != null && this.moduleList.mouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.viewMode == ViewMode.CATEGORY_PANEL && this.activeSettingsModule == null
                && this.moduleList != null && this.moduleList.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (this.focusedTextSetting != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                this.focusedTextSetting = null;
                OptimizerConfig.getInstance().save();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String cur = this.focusedTextSetting.getValue();
                if (!cur.isEmpty()) {
                    this.focusedTextSetting.setValue(cur.substring(0, cur.length() - 1));
                    OptimizerConfig.getInstance().save();
                }
                return true;
            }
            return true;
        }

        if (this.searchBarOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                this.searchBarOpen = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.searchQuery.isEmpty()) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                    rebuildModuleList();
                }
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_ESCAPE && this.activeSettingsModule != null) {
            this.activeSettingsModule = null;
            if (this.moduleList != null) this.moduleList.setSelectedModule(null);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        String typed = input.asString();
        if (typed.isEmpty()) return super.charTyped(input);

        if (this.focusedTextSetting != null) {
            String cur = this.focusedTextSetting.getValue();
            if (cur.length() < this.focusedTextSetting.getMaxLength()) {
                this.focusedTextSetting.setValue(cur + typed);
                OptimizerConfig.getInstance().save();
            }
            return true;
        }

        if (this.searchBarOpen) {
            if (this.searchQuery.length() < 22) {
                this.searchQuery += typed;
                rebuildModuleList();
            }
            return true;
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
        OptimizerConfig.getInstance().save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
