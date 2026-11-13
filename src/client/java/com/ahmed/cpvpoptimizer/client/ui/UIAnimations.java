package com.ahmed.cpvpoptimizer.client.ui;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import net.minecraft.util.math.MathHelper;

public final class UIAnimations {
    private UIAnimations() {}

    public static float approach(float current, float target, float deltaSeconds, float speed) {
        if (!OptimizerConfig.getInstance().isUiAnimationsEnabled()) {
            return target;
        }
        float step = MathHelper.clamp(deltaSeconds * speed, 0.0f, 1.0f);
        // Smooth ease-out interpolation (~120-170ms response)
        float eased = 1.0f - (1.0f - step) * (1.0f - step);
        float next = MathHelper.lerp(eased, current, target);
        return Math.abs(target - next) < 0.002f ? target : next;
    }

    public static double smoothScroll(double current, double target, float deltaSeconds) {
        if (!OptimizerConfig.getInstance().isUiAnimationsEnabled()) {
            return target;
        }
        double factor = Math.min(1.0, deltaSeconds * 18.0);
        double next = current + (target - current) * factor;
        return Math.abs(target - next) < 0.2 ? target : next;
    }

    public static float easeOutCubic(float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - t;
        return 1.0f - (inv * inv * inv);
    }
}
