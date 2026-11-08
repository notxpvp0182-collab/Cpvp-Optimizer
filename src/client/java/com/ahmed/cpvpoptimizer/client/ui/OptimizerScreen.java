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
 * Clean Auto-Adaptive ClickGUI Dashboard:
 * - No P logo (clean "CPVP_OPTIMIZER" title only).
 * - No "CONNECTED", "STATUS", or "VERSION" texts anywhere.
 * - No manual resize system; automatically detects phone/PC screen resolution & GUI scale and fits cleanly.
 */
public final class OptimizerScreen extends Screen {
    private static final int HEADER_H = 40;
    private static final int SIDEBAR_W = 104;

    private final Screen parent;
    private ModuleCategory selectedCategory = null;
    private boolean profileTabActive = false;
    private OptimizerModule activeSettingsModule = null;
    private ScrollableModuleList moduleList;
    private double savedScrollOffset = 0.0;

    // Live Search State
    private String searchQuery = "";
    private boolean searchFocused = false;

    // Auto-Adapted Window Bounds
    private int winX;
    private int winY;
    private int winW;
    private int winH;

    // Settings Drawer Controls
    private ModuleSetting.IntSliderSetting activeDraggingSlider = null;
    private ModuleSetting.TextSetting focusedTextSetting = null;

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
        computeAutoScreenBounds();
        rebuildModuleList();
    }

    /**
     * Automatically detects the current phone/PC scaled screen dimensions
     * and sizes + centers the ClickGUI proportionally.
     */
    private void computeAutoScreenBounds() {
        int marginX = Math.max(8, Math.round(this.width * 0.035f));
        int marginY = Math.max(8, Math.round(this.height * 0.045f));

        this.winW = Math.min(720, Math.max(300, this.width - (marginX * 2)));
        this.winH = Math.min(420, Math.max(190, this.height - (marginY * 2)));

        this.winX = (this.width - this.winW) / 2;
        this.winY = (this.height - this.winH) / 2;
    }

    private void rebuildModuleList() {
        List<OptimizerModule> source = selectedCategory == null
                ? ModuleRegistry.getInstance().getAllModules()
                : ModuleRegistry.getInstance().getByCategory(selectedCategory);

        List<OptimizerModule> filtered = new ArrayList<>();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (OptimizerModule m : source) {
            if (q.isEmpty()
                    || m.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(m);
            }
        }

        this.moduleList = new ScrollableModuleList(filtered, module -> {
            this.activeSettingsModule = (this.activeSettingsModule == module) ? null : module;
            this.focusedTextSetting = null;
            this.moduleList.setSelectedModule(this.activeSettingsModule);
        });
        this.moduleList.setSelectedModule(this.activeSettingsModule);
        updateGridBounds();
        this.moduleList.setScrollAmountImmediate(savedScrollOffset);
    }

    private void updateGridBounds() {
        if (this.moduleList == null) return;
        int gridX = winX + SIDEBAR_W + 2;
        int gridY = winY + HEADER_H + 2;
        int gridH = Math.max(60, winH - HEADER_H - 4);
        int fullGridW = winW - SIDEBAR_W - 4;
        int gridW = (activeSettingsModule != null) ? Math.max(150, fullGridW - 185) : fullGridW;
        this.moduleList.setDimensions(gridX, gridY, gridW, gridH);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Transparent in-game background
        context.fill(0, 0, this.width, this.height, 0x26000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float deltaSeconds = Math.min(0.1f, delta / 20.0f);
        computeAutoScreenBounds();
        updateGridBounds();
        this.renderBackground(ctx, mouseX, mouseY, delta);

        // 1. Outer Soft Shadow + Main Obsidian Frame (#0E1015)
        ctx.fill(winX - 3, winY - 3, winX + winW + 3, winY + winH + 3, 0x66000000);
        ctx.fill(winX - 1, winY - 1, winX + winW + 1, winY + winH + 1, 0xFF22252E);
        ctx.fill(winX, winY, winX + winW, winY + winH, 0xF60E1015);

        // 2. Top Header Bar (Clean Title Only - No P Logo)
        renderTopHeader(ctx, mouseX, mouseY);

        // 3. Left Sidebar (No Connected/Version text)
        renderLeftSidebar(ctx, mouseX, mouseY);

        // 4. Center Content Area
        if (profileTabActive) {
            renderProfileView(ctx, mouseX, mouseY);
        } else {
            if (this.moduleList != null) {
                this.moduleList.render(ctx, this.textRenderer, mouseX, mouseY, deltaSeconds);
            }
            if (this.activeSettingsModule != null) {
                renderRightSettingsPanel(ctx, mouseX, mouseY);
            }
        }
    }

    private void renderTopHeader(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(winX, winY, winX + winW, winY + HEADER_H, 0xFF0C0E12);
        ctx.fill(winX, winY + HEADER_H, winX + winW, winY + HEADER_H + 1, 0xFF20232B);

        // Clean Brand Title on the Left (No Logo)
        int titleX = winX + 12;
        int titleY = winY + 9;
        ctx.drawTextWithShadow(this.textRenderer, "CPVP", titleX, titleY, 0xFFFFFFFF);
        int cpvpW = this.textRenderer.getWidth("CPVP");
        ctx.drawTextWithShadow(this.textRenderer, "_OPTIMIZER", titleX + cpvpW, titleY, 0xFFFF3B1D);
        ctx.drawText(this.textRenderer, "MODS & HUD MANAGER", titleX, titleY + 12, 0xFF8E96A8, false);

        // Right-side Controls: [ SETTINGS ] Button + [ Search modules... 🔍 ]
        int rightEdge = winX + winW - 12;

        // Search Box ("Search modules...")
        int searchW = Math.min(155, Math.max(90, winW / 4));
        int searchX = rightEdge - searchW;
        int searchY = winY + 9;
        int searchH = 22;

        int searchBorder = searchFocused ? 0xFFFF4B2B : 0xFF252832;
        ctx.fill(searchX, searchY, searchX + searchW, searchY + searchH, searchBorder);
        ctx.fill(searchX + 1, searchY + 1, searchX + searchW - 1, searchY + searchH - 1, 0xFF12141A);

        String cursor = (searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) ? "|" : "";
        String searchDisplay = (searchQuery.isEmpty() && !searchFocused)
                ? "Search modules..."
                : (searchQuery + cursor);
        int searchTextColor = (searchQuery.isEmpty() && !searchFocused) ? 0xFF687082 : 0xFFFFFFFF;
        ctx.drawText(this.textRenderer,
                this.textRenderer.trimToWidth(searchDisplay, searchW - 24),
                searchX + 8, searchY + 7, searchTextColor, false);

        // Magnifying Glass Icon inside Search Box
        int magX = searchX + searchW - 15;
        int magY = searchY + 6;
        ctx.fill(magX, magY, magX + 6, magY + 6, 0xFF8E96A8);
        ctx.fill(magX + 1, magY + 1, magX + 5, magY + 5, 0xFF12141A);
        ctx.fill(magX + 5, magY + 5, magX + 9, magY + 9, 0xFF8E96A8);

        // [ SETTINGS ] Button to the left of Search Box
        int setBtnW = 76;
        int setBtnX = searchX - setBtnW - 8;
        int setBtnY = winY + 9;
        boolean setHover = ModuleButton.isMouseOver(mouseX, mouseY, setBtnX, setBtnY, setBtnW, 22);

        ctx.fill(setBtnX, setBtnY, setBtnX + setBtnW, setBtnY + 22, setHover ? 0xFFFF4B2B : 0xFF282C37);
        ctx.fill(setBtnX + 1, setBtnY + 1, setBtnX + setBtnW - 1, setBtnY + 21, 0xFF14161D);

        // Red Target Reticle Icon inside SETTINGS Button
        int retX = setBtnX + 8;
        int retY = setBtnY + 6;
        ctx.fill(retX + 2, retY + 2, retX + 8, retY + 8, 0xFFFF3B1D);
        ctx.fill(retX + 3, retY + 3, retX + 7, retY + 7, 0xFF14161D);
        ctx.fill(retX + 4, retY, retX + 6, retY + 2, 0xFFFF3B1D);
        ctx.fill(retX + 4, retY + 8, retX + 6, retY + 10, 0xFFFF3B1D);
        ctx.fill(retX, retY + 4, retX + 2, retY + 6, 0xFFFF3B1D);
        ctx.fill(retX + 8, retY + 4, retX + 10, retY + 6, 0xFFFF3B1D);

        ctx.drawTextWithShadow(this.textRenderer, "SETTINGS", setBtnX + 22, setBtnY + 7, 0xFFFFFFFF);
    }

    private void renderLeftSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int sideTop = winY + HEADER_H + 1;
        int sideBottom = winY + winH;

        ctx.fill(winX, sideTop, winX + SIDEBAR_W, sideBottom, 0xFF0C0E12);
        ctx.fill(winX + SIDEBAR_W, sideTop, winX + SIDEBAR_W + 1, sideBottom, 0xFF20232B);

        int btnX = winX + 7;
        int btnW = SIDEBAR_W - 14;

        // 1. DASHBOARD Tab
        int dashY = sideTop + 10;
        if (!profileTabActive && selectedCategory == null) {
            ModuleButton.drawHorizontalGradient(ctx, btnX, dashY, btnW, 24, 0xFF991B1B, 0xFFDC2626);
            ctx.fill(btnX, dashY, btnX + btnW, dashY + 1, 0x55FFFFFF);
        } else if (ModuleButton.isMouseOver(mouseX, mouseY, btnX, dashY, btnW, 24)) {
            ctx.fill(btnX, dashY, btnX + btnW, dashY + 24, 0xFF181B22);
        }
        drawFourSquareIcon(ctx, btnX + 6, dashY + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "DASHBOARD", btnX + 21, dashY + 8, 0xFFFFFFFF);

        // 2. PROFILE Tab
        int profY = dashY + 30;
        if (profileTabActive) {
            ModuleButton.drawHorizontalGradient(ctx, btnX, profY, btnW, 24, 0xFF991B1B, 0xFFDC2626);
        } else if (ModuleButton.isMouseOver(mouseX, mouseY, btnX, profY, btnW, 24)) {
            ctx.fill(btnX, profY, btnX + btnW, profY + 24, 0xFF181B22);
        }
        ctx.fill(btnX + 8, profY + 5, btnX + 14, profY + 11, 0xFFCBD5E1);
        ctx.fill(btnX + 6, profY + 13, btnX + 16, profY + 17, 0xFFCBD5E1);
        ctx.drawText(this.textRenderer, "PROFILE", btnX + 21, profY + 8,
                profileTabActive ? 0xFFFFFFFF : 0xFF94A3B8, false);
        ctx.fill(btnX + 4, profY + 22, btnX + btnW - 4, profY + 23, 0xFF20232B);

        // 3. Category Filters
        int catY = profY + 30;
        for (ModuleCategory cat : ModuleCategory.values()) {
            if (catY + 16 > sideBottom - 8) break;
            boolean isCatSelected = (!profileTabActive && selectedCategory == cat);
            boolean hovered = ModuleButton.isMouseOver(mouseX, mouseY, btnX, catY, btnW, 15);
            if (isCatSelected) {
                ctx.fill(btnX, catY, btnX + 2, catY + 14, 0xFFFF3B1D);
                ctx.fill(btnX + 2, catY, btnX + btnW, catY + 14, 0x44DC2626);
            } else if (hovered) {
                ctx.fill(btnX, catY, btnX + btnW, catY + 14, 0xFF151820);
            }
            ctx.drawText(this.textRenderer, cat.getDisplayName().toUpperCase(Locale.ROOT),
                    btnX + 8, catY + 3, isCatSelected ? 0xFFFFFFFF : 0xFF788296, false);
            catY += 17;
        }
    }

    private void drawFourSquareIcon(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 5, y + 5, color);
        ctx.fill(x + 7, y, x + 12, y + 5, color);
        ctx.fill(x, y + 7, x + 5, y + 12, color);
        ctx.fill(x + 7, y + 7, x + 12, y + 12, color);
    }

    private void renderProfileView(DrawContext ctx, int mouseX, int mouseY) {
        int areaX = winX + SIDEBAR_W + 16;
        int areaY = winY + HEADER_H + 16;
        OptimizerConfig cfg = OptimizerConfig.getInstance();

        ctx.drawTextWithShadow(this.textRenderer, "OPTIMIZATION PROFILES & GLOBAL CONTROLS", areaX, areaY, 0xFFFFFFFF);

        int cardW = Math.min(210, (winW - SIDEBAR_W - 44) / 2);
        drawProfileActionCard(ctx, areaX, areaY + 20, cardW, 44,
                "Maximum Responsiveness", "Zero-delay Block, Anchor, Glowstone, Hotbar & XP",
                !cfg.isSafeMode() && cfg.isMasterEnabled(), mouseX, mouseY);

        drawProfileActionCard(ctx, areaX + cardW + 12, areaY + 20, cardW, 44,
                "Safe Mode (Fallback)", "Disables all active hooks safely",
                cfg.isSafeMode(), mouseX, mouseY);

        drawProfileActionCard(ctx, areaX, areaY + 74, cardW, 44,
                "UI Smooth Animations", "Toggle smooth card & scroll transitions",
                cfg.isUiAnimationsEnabled(), mouseX, mouseY);

        drawProfileActionCard(ctx, areaX + cardW + 12, areaY + 74, cardW, 44,
                "Reset All Defaults", "Restore all 15 modules to factory settings",
                false, mouseX, mouseY);
    }

    private void drawProfileActionCard(DrawContext ctx, int x, int y, int w, int h, String title, String sub, boolean active, int mx, int my) {
        boolean hover = ModuleButton.isMouseOver(mx, my, x, y, w, h);
        ctx.fill(x, y, x + w, y + h, hover ? 0xFFFF4B2B : 0xFF252832);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF14161C);
        ctx.fill(x, y, x + 10, y + 2, 0xFFFF3B1D);
        ctx.fill(x, y, x + 2, y + 10, 0xFFFF3B1D);

        ctx.drawTextWithShadow(this.textRenderer, this.textRenderer.trimToWidth(title, w - 16), x + 10, y + 8, 0xFFFFFFFF);
        ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(sub, w - 20), x + 10, y + 20, 0xFF8E96A8, false);

        String badge = active ? "ACTIVE" : "APPLY";
        int bw = 48;
        int bx = x + w - bw - 8;
        int by = y + h - 16;
        if (active) {
            ModuleButton.drawHorizontalGradient(ctx, bx, by, bw, 12, 0xFFFF9A1F, 0xFFFF3B1D);
        } else {
            ctx.fill(bx, by, bx + bw, by + 12, 0xFF252832);
        }
        ctx.drawTextWithShadow(this.textRenderer, badge, bx + (bw - this.textRenderer.getWidth(badge)) / 2, by + 2, 0xFFFFFFFF);
    }

    private void renderRightSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        int panelW = 180;
        int panelX = winX + winW - panelW - 4;
        int panelY = winY + HEADER_H + 4;
        int panelH = Math.max(60, winH - HEADER_H - 8);

        ctx.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFFFF3B1D);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF812141A);

        ctx.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);

        String title = this.textRenderer.trimToWidth(activeSettingsModule.getDisplayName(), panelW - 28);
        ctx.drawTextWithShadow(this.textRenderer, title, panelX + 8, panelY + 8, 0xFFFFFFFF);

        int closeX = panelX + panelW - 18;
        ctx.fill(closeX, panelY + 6, closeX + 12, panelY + 18, 0xFF252832);
        ctx.drawTextWithShadow(this.textRenderer, "×", closeX + 3, panelY + 8, 0xFFFF4B2B);

        int rowY = panelY + 26;
        int ctrlX = panelX + 8;
        int ctrlW = panelW - 16;

        for (ModuleSetting<?> setting : activeSettingsModule.getSettings()) {
            if (rowY + 22 > panelY + panelH - 24) break;

            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                boolean hover = ModuleButton.isMouseOver(mouseX, mouseY, ctrlX, rowY, ctrlW, 18);
                ctx.fill(ctrlX, rowY, ctrlX + ctrlW, rowY + 18, hover ? 0xFF1F232D : 0xFF171A22);
                String name = this.textRenderer.trimToWidth(boolSetting.getDisplayName(), ctrlW - 38);
                ctx.drawText(this.textRenderer, name, ctrlX + 5, rowY + 5, 0xFFE2E8F0, false);

                if (boolSetting.getValue()) {
                    ModuleButton.drawHorizontalGradient(ctx, ctrlX + ctrlW - 30, rowY + 3, 26, 12, 0xFFFF9A1F, 0xFFFF3B1D);
                    ctx.drawTextWithShadow(this.textRenderer, "ON", ctrlX + ctrlW - 23, rowY + 5, 0xFFFFFFFF);
                } else {
                    ctx.fill(ctrlX + ctrlW - 30, rowY + 3, ctrlX + ctrlW - 4, rowY + 15, 0xFF252832);
                    ctx.drawText(this.textRenderer, "OFF", ctrlX + ctrlW - 25, rowY + 5, 0xFF8E96A8, false);
                }
                rowY += 21;

            } else if (setting instanceof ModuleSetting.IntSliderSetting slider) {
                ctx.fill(ctrlX, rowY, ctrlX + ctrlW, rowY + 22, 0xFF171A22);
                String label = slider.getDisplayName() + ": " + slider.formatValue();
                ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(label, ctrlW - 8), ctrlX + 5, rowY + 3, 0xFFE2E8F0, false);

                int barX = ctrlX + 5;
                int barY = rowY + 15;
                int barW = ctrlW - 10;
                double ratio = (double) (slider.getValue() - slider.getMin()) / (double) Math.max(1, slider.getMax() - slider.getMin());
                int fillW = (int) Math.round(ratio * barW);

                ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF262A34);
                if (fillW > 0) {
                    ModuleButton.drawHorizontalGradient(ctx, barX, barY, fillW, 3, 0xFFFF9A1F, 0xFFFF3B1D);
                }
                ctx.fill(barX + fillW - 2, barY - 1, barX + fillW + 2, barY + 4, 0xFFFFFFFF);
                rowY += 25;

            } else if (setting instanceof ModuleSetting.CycleSetting cycle) {
                ctx.fill(ctrlX, rowY, ctrlX + ctrlW, rowY + 18, 0xFF171A22);
                String text = cycle.getDisplayName() + ": " + cycle.formatValue();
                ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(text, ctrlW - 10), ctrlX + 5, rowY + 5, 0xFFE2E8F0, false);
                rowY += 21;

            } else if (setting instanceof ModuleSetting.TextSetting textSetting) {
                boolean focused = (this.focusedTextSetting == textSetting);
                ctx.fill(ctrlX, rowY, ctrlX + ctrlW, rowY + 20, focused ? 0xFFFF4B2B : 0xFF282C37);
                ctx.fill(ctrlX + 1, rowY + 1, ctrlX + ctrlW - 1, rowY + 19, 0xFF0E1015);
                String cursor = (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) ? "_" : "";
                String txt = textSetting.getDisplayName() + ": " + textSetting.getValue() + cursor;
                ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(txt, ctrlW - 10), ctrlX + 5, rowY + 6, 0xFFFFFFFF, false);
                rowY += 23;
            }
        }

        int resetY = panelY + panelH - 20;
        ctx.fill(ctrlX, resetY, ctrlX + ctrlW, resetY + 15, 0xFF252832);
        String rLabel = "RESET DEFAULTS";
        ctx.drawText(this.textRenderer, rLabel, ctrlX + (ctrlW - this.textRenderer.getWidth(rLabel)) / 2, resetY + 4, 0xFFCBD5E1, false);

        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        this.searchFocused = false;
        this.focusedTextSetting = null;

        // 1. Top Header Interactions (Search Box & SETTINGS button)
        if (mx >= winX && mx <= winX + winW && my >= winY && my <= winY + HEADER_H) {
            int rightEdge = winX + winW - 12;
            int searchW = Math.min(155, Math.max(90, winW / 4));
            int searchX = rightEdge - searchW;
            int searchY = winY + 9;

            if (ModuleButton.isMouseOver(mx, my, searchX, searchY, searchW, 22)) {
                playClickSound();
                if (button == 1) {
                    this.searchQuery = "";
                    rebuildModuleList();
                }
                this.searchFocused = true;
                return true;
            }

            int setBtnW = 76;
            int setBtnX = searchX - setBtnW - 8;
            if (ModuleButton.isMouseOver(mx, my, setBtnX, searchY, setBtnW, 22)) {
                playClickSound();
                this.profileTabActive = !this.profileTabActive;
                return true;
            }
            return true;
        }

        // 2. Left Sidebar Clicks (DASHBOARD, PROFILE, Category Filters)
        int sideTop = winY + HEADER_H + 1;
        int sideBottom = winY + winH;
        if (mx >= winX && mx <= winX + SIDEBAR_W && my >= sideTop && my <= sideBottom) {
            int btnX = winX + 7;
            int btnW = SIDEBAR_W - 14;
            int dashY = sideTop + 10;
            int profY = dashY + 30;

            if (ModuleButton.isMouseOver(mx, my, btnX, dashY, btnW, 24)) {
                playClickSound();
                this.profileTabActive = false;
                this.selectedCategory = null;
                this.savedScrollOffset = 0.0;
                rebuildModuleList();
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, btnX, profY, btnW, 24)) {
                playClickSound();
                this.profileTabActive = true;
                return true;
            }

            int catY = profY + 30;
            for (ModuleCategory cat : ModuleCategory.values()) {
                if (catY + 16 > sideBottom - 8) break;
                if (ModuleButton.isMouseOver(mx, my, btnX, catY, btnW, 15)) {
                    playClickSound();
                    this.profileTabActive = false;
                    this.selectedCategory = cat;
                    this.savedScrollOffset = 0.0;
                    rebuildModuleList();
                    return true;
                }
                catY += 17;
            }
            return true;
        }

        // 3. Profile View Clicks
        if (profileTabActive) {
            int areaX = winX + SIDEBAR_W + 16;
            int areaY = winY + HEADER_H + 16;
            int cardW = Math.min(210, (winW - SIDEBAR_W - 44) / 2);
            OptimizerConfig cfg = OptimizerConfig.getInstance();

            if (ModuleButton.isMouseOver(mx, my, areaX, areaY + 20, cardW, 44)) {
                playClickSound();
                cfg.applyMaximumResponsivenessProfile();
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, areaX + cardW + 12, areaY + 20, cardW, 44)) {
                playClickSound();
                cfg.setSafeMode(!cfg.isSafeMode());
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, areaX, areaY + 74, cardW, 44)) {
                playClickSound();
                cfg.setUiAnimationsEnabled(!cfg.isUiAnimationsEnabled());
                return true;
            }
            if (ModuleButton.isMouseOver(mx, my, areaX + cardW + 12, areaY + 74, cardW, 44)) {
                playClickSound();
                cfg.resetAllToDefaults();
                return true;
            }
            return true;
        }

        // 4. Right Settings Panel Clicks
        if (this.activeSettingsModule != null && handleRightSettingsClick(mx, my, button)) {
            return true;
        }

        // 5. Module Card Grid Clicks
        if (this.moduleList != null && this.moduleList.mouseClicked(mx, my, button)) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean handleRightSettingsClick(double mx, double my, int button) {
        int panelW = 180;
        int panelX = winX + winW - panelW - 4;
        int panelY = winY + HEADER_H + 4;
        int panelH = Math.max(60, winH - HEADER_H - 8);

        if (!ModuleButton.isMouseOver(mx, my, panelX, panelY, panelW, panelH)) {
            return false;
        }

        int closeX = panelX + panelW - 18;
        if (ModuleButton.isMouseOver(mx, my, closeX, panelY + 6, 12, 12)) {
            playClickSound();
            this.activeSettingsModule = null;
            this.moduleList.setSelectedModule(null);
            return true;
        }

        int ctrlX = panelX + 8;
        int ctrlW = panelW - 16;
        int resetY = panelY + panelH - 20;
        if (ModuleButton.isMouseOver(mx, my, ctrlX, resetY, ctrlW, 15)) {
            playClickSound();
            activeSettingsModule.resetDefaults();
            OptimizerConfig.getInstance().save();
            return true;
        }

        int rowY = panelY + 26;
        for (ModuleSetting<?> setting : activeSettingsModule.getSettings()) {
            if (rowY + 22 > panelY + panelH - 24) break;

            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                if (ModuleButton.isMouseOver(mx, my, ctrlX, rowY, ctrlW, 18)) {
                    playClickSound();
                    boolSetting.toggle();
                    OptimizerConfig.getInstance().save();
                    return true;
                }
                rowY += 21;
            } else if (setting instanceof ModuleSetting.IntSliderSetting slider) {
                if (ModuleButton.isMouseOver(mx, my, ctrlX, rowY, ctrlW, 22)) {
                    this.activeDraggingSlider = slider;
                    updateSliderFromMouse(slider, mx, ctrlX + 5, ctrlW - 10);
                    return true;
                }
                rowY += 25;
            } else if (setting instanceof ModuleSetting.CycleSetting cycle) {
                if (ModuleButton.isMouseOver(mx, my, ctrlX, rowY, ctrlW, 18)) {
                    playClickSound();
                    cycle.cycleNext();
                    OptimizerConfig.getInstance().save();
                    return true;
                }
                rowY += 21;
            } else if (setting instanceof ModuleSetting.TextSetting textSetting) {
                if (ModuleButton.isMouseOver(mx, my, ctrlX, rowY, ctrlW, 20)) {
                    playClickSound();
                    if (button == 1) textSetting.setValue("");
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

        if (this.activeDraggingSlider != null && this.activeSettingsModule != null) {
            int panelW = 180;
            int panelX = winX + winW - panelW - 4;
            updateSliderFromMouse(this.activeDraggingSlider, mx, panelX + 13, panelW - 26);
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
        this.activeDraggingSlider = null;
        if (this.moduleList != null && this.moduleList.mouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!profileTabActive && this.moduleList != null && this.moduleList.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();

        if (this.searchFocused) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.searchQuery.isEmpty()) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                    rebuildModuleList();
                }
                return true;
            }
            return true;
        }

        if (this.focusedTextSetting != null) {
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
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        String typed = input.asString();
        if (typed.isEmpty()) return super.charTyped(input);

        if (this.searchFocused) {
            if (this.searchQuery.length() < 24) {
                this.searchQuery += typed;
                rebuildModuleList();
            }
            return true;
        }

        if (this.focusedTextSetting != null) {
            String current = this.focusedTextSetting.getValue();
            if (current.length() < this.focusedTextSetting.getMaxLength()) {
                this.focusedTextSetting.setValue(current + typed);
                OptimizerConfig.getInstance().save();
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
