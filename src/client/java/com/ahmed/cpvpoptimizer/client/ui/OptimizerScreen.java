package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleRegistry;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.server.ServerPerformanceManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class OptimizerScreen extends Screen {
    private final Screen parent;
    private ModuleCategory selectedCategory = null;
    private ScrollableModuleList moduleList;
    private double savedScrollOffset = 0.0;
    private float openFadeProgress = 0.0f;

    public OptimizerScreen(Screen parent) {
        super(Text.literal("CPVP Optimizer Options"));
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
        rebuildListAndWidgets();
    }

    private void rebuildListAndWidgets() {
        this.clearChildren();

        int topY = 24;
        int buttonW = 104;
        int startX = (this.width - (buttonW * 4 + 12)) / 2;

        String categoryLabel = "Category: " + (selectedCategory == null ? "All" : selectedCategory.getDisplayName());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(categoryLabel), btn -> {
            cycleCategory();
            savedScrollOffset = 0.0;
            rebuildListAndWidgets();
        }).dimensions(startX, topY, buttonW, 20).build());

        OptimizerConfig cfg = OptimizerConfig.getInstance();
        String masterLabel = "Master: " + (cfg.isMasterEnabled() ? "ON" : "OFF");
        this.addDrawableChild(ButtonWidget.builder(Text.literal(masterLabel), btn -> {
            cfg.setMasterEnabled(!cfg.isMasterEnabled());
            rebuildListAndWidgets();
        }).dimensions(startX + (buttonW + 4), topY, buttonW, 20).build());

        String safeModeLabel = "Safe Mode: " + (cfg.isSafeMode() ? "ON" : "OFF");
        this.addDrawableChild(ButtonWidget.builder(Text.literal(safeModeLabel), btn -> {
            cfg.setSafeMode(!cfg.isSafeMode());
            rebuildListAndWidgets();
        }).dimensions(startX + (buttonW + 4) * 2, topY, buttonW, 20).build());

        String animLabel = "Animations: " + (cfg.isUiAnimationsEnabled() ? "ON" : "OFF");
        this.addDrawableChild(ButtonWidget.builder(Text.literal(animLabel), btn -> {
            cfg.setUiAnimationsEnabled(!cfg.isUiAnimationsEnabled());
            rebuildListAndWidgets();
        }).dimensions(startX + (buttonW + 4) * 3, topY, buttonW, 20).build());

        List<OptimizerModule> filtered = selectedCategory == null
                ? ModuleRegistry.getInstance().getAllModules()
                : ModuleRegistry.getInstance().getByCategory(selectedCategory);

        this.moduleList = new ScrollableModuleList(filtered, this::openModuleSettings);
        int listTop = 50;
        int listBottom = this.height - 36;
        this.moduleList.setDimensions(16, listTop, this.width - 32, Math.max(60, listBottom - listTop));
        this.moduleList.setScrollAmountImmediate(savedScrollOffset);

        int bottomY = this.height - 26;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset All Defaults"), btn -> {
            OptimizerConfig.getInstance().resetAllToDefaults();
            rebuildListAndWidgets();
        }).dimensions(this.width / 2 - 154, bottomY, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(this.width / 2 + 4, bottomY, 150, 20).build());
    }

    private void cycleCategory() {
        ModuleCategory[] categories = ModuleCategory.values();
        if (selectedCategory == null) {
            selectedCategory = categories[0];
        } else {
            int next = selectedCategory.ordinal() + 1;
            selectedCategory = next < categories.length ? categories[next] : null;
        }
    }

    private void openModuleSettings(OptimizerModule module) {
        if (this.client == null) return;
        this.savedScrollOffset = this.moduleList != null ? this.moduleList.getScrollAmount() : 0.0;
        this.client.setScreen(new SettingsScreen(this, module, this.selectedCategory, this.savedScrollOffset));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaSeconds = Math.min(0.1f, delta / 20.0f);
        this.openFadeProgress = UIAnimations.approach(this.openFadeProgress, 1.0f, deltaSeconds, 10.0f);

        this.renderBackground(context, mouseX, mouseY, delta);

        if (this.moduleList != null) {
            this.moduleList.render(context, this.textRenderer, mouseX, mouseY, deltaSeconds);
        }

        boolean serverSynced = ServerPerformanceManager.getInstance().isServerComponentActive();
        String serverTag = serverSynced ? "Server Mod: Active" : "Server Mod: Not Installed (Client-Only Mode)";
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("CPVP Optimizer (1.21.11) - Left-Click: Toggle | Right-Click: Module Settings | " + serverTag),
                this.width / 2,
                8,
                0xFFFFFFFF
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.moduleList != null && this.moduleList.mouseClicked(click.x(), click.y(), click.button())) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (this.moduleList != null && this.moduleList.mouseDragged(click.x(), click.y(), click.button())) {
            this.savedScrollOffset = this.moduleList.getScrollAmount();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
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
    public void close() {
        OptimizerConfig.getInstance().save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
