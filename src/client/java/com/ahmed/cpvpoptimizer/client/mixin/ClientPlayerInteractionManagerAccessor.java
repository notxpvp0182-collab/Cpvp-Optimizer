package com.ahmed.cpvpoptimizer.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {

    @Invoker("syncSelectedSlot")
    void cpvp$syncSelectedSlot();

    @Accessor("blockBreakingCooldown")
    int cpvp$getBlockBreakingCooldown();

    @Accessor("blockBreakingCooldown")
    void cpvp$setBlockBreakingCooldown(int cooldown);
}
