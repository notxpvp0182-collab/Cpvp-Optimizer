package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Individual Module Settings Screen opened via Right-Click on any module in OptimizerScreen.
 * Includes Vanilla toggle buttons, sliders, cycling buttons, Reset Defaults, and Back navigation.
 */
public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final OptimizerModule module;
    private final ModuleCategory retainedCategory;
    private final double retainedScrollOffset;
    private float transitionProgress = 0.0f;

    public SettingsScreen(Screen parent, OptimizerModule module, ModuleCategory retainedCategory, double retainedScrollOffset) {
        super(Text.literal(module.getDisplayName() + " Settings"));
        this.parent = parent;
        this.module = module;
        this.retainedCategory = retainedCategory;
        this.retainedScrollOffset = retainedScrollOffset;
    }

    @Override
    protected void init() {
        super.init();
        rebuildControls();
    }

    private void rebuildControls() {
        this.clearChildren();

        int controlWidth = Math.min(310, this.width - 40);
        int centerX = (this.width - controlWidth) / 2;
        int currentY = 48;

        // Primary Module Enable/Disable Button
        String enableLabel = "Enable " + module.getDisplayName() + ": " + (module.isEnabled() ? "ON" : "OFF");
        this.addDrawableChild(ButtonWidget.builder(Text.literal(enableLabel), btn -> {
            module.toggle();
            OptimizerConfig.getInstance().save();
            rebuildControls();
        }).dimensions(centerX, currentY, controlWidth, 20).build());
        currentY += 26;

        // Dynamic Controls for Every Real Module Setting
        for (ModuleSetting<?> setting : module.getSettings()) {
            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                String label = boolSetting.getDisplayName() + ": " + boolSetting.formatValue();
                this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                    boolSetting.toggle();
                    OptimizerConfig.getInstance().save();
                    rebuildControls();
                }).dimensions(centerX, currentY, controlWidth, 20).build());
            } else if (setting instanceof ModuleSetting.CycleSetting cycleSetting) {
                String label = cycleSetting.getDisplayName() + ": " + cycleSetting.formatValue();
                this.addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                    cycleSetting.cycleNext();
                    OptimizerConfig.getInstance().save();
                    rebuildControls();
                }).dimensions(centerX, currentY, controlWidth, 20).build());
            } else if (setting instanceof ModuleSetting.IntSliderSetting sliderSetting) {
                this.addDrawableChild(new VanillaIntSlider(centerX, currentY, controlWidth, 20, sliderSetting));
            }
            currentY += 24;
        }

        // Bottom Action Buttons: Reset Defaults & Back
        int bottomY = this.height - 28;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset Defaults"), btn -> {
            module.resetDefaults();
            OptimizerConfig.getInstance().save();
            rebuildControls();
        }).dimensions(this.width / 2 - 154, bottomY, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> close())
                .dimensions(this.width / 2 + 4, bottomY, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaSeconds = Math.min(0.1f, delta / 20.0f);
        this.transitionProgress = UIAnimations.approach(this.transitionProgress, 1.0f, deltaSeconds, 12.0f);

        this.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(module.getDescription()),
                this.width / 2,
                26,
                0xFFA0A0A0
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Diagnostics: " + module.getDiagnosticsSummary()),
                this.width / 2,
                this.height - 46,
                0xFFCCCCCC
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        OptimizerConfig.getInstance().save();
        if (this.client != null) {
            if (this.parent instanceof OptimizerScreen) {
                this.client.setScreen(new OptimizerScreen(null, retainedCategory, retainedScrollOffset));
            } else {
                this.client.setScreen(this.parent);
            }
        }
    }

    private static final class VanillaIntSlider extends SliderWidget {
        private final ModuleSetting.IntSliderSetting setting;

        VanillaIntSlider(int x, int y, int width, int height, ModuleSetting.IntSliderSetting setting) {
            super(x, y, width, height, Text.empty(), toNormalized(setting));
            this.setting = setting;
            updateMessage();
        }

        private static double toNormalized(ModuleSetting.IntSliderSetting s) {
            return (double) (s.getValue() - s.getMin()) / (double) Math.max(1, s.getMax() - s.getMin());
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(setting.getDisplayName() + ": " + setting.formatValue()));
        }

        @Override
        protected void applyValue() {
            int raw = setting.getMin() + (int) Math.round(MathHelper.clamp(this.value, 0.0, 1.0) * (setting.getMax() - setting.getMin()));
            setting.setValue(raw);
            OptimizerConfig.getInstance().save();
        }
    }
}
