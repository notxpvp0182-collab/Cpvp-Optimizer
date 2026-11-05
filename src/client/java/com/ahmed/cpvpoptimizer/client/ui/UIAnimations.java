package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import net.minecraft.util.math.MathHelper;

/**
 * Lightweight, non-blocking vanilla-style interpolation helpers.
 * Automatically snaps to target immediately when UI Animations are disabled.
 */
public final class UIAnimations {
    private UIAnimations() {}

    public static float approach(float current, float target, float deltaSeconds, float speed) {
        if (!OptimizerConfig.getInstance().isUiAnimationsEnabled()) {
            return target;
        }
        float step = MathHelper.clamp(deltaSeconds * speed, 0.0f, 1.0f);
        return MathHelper.lerp(step, current, target);
    }

    public static double smoothScroll(double current, double target, float deltaSeconds) {
        if (!OptimizerConfig.getInstance().isUiAnimationsEnabled()) {
            return target;
        }
        double factor = Math.min(1.0, deltaSeconds * 16.0);
        double next = current + (target - current) * factor;
        return Math.abs(target - next) < 0.25 ? target : next;
    }
}
