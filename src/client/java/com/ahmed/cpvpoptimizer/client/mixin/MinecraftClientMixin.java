package com.ahmed.cpvpoptimizer.client.mixin;

import com.ahmed.cpvpoptimizer.client.CPVPOptimizerClient;
import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    private int itemUseCooldown;

    @Shadow
    public int attackCooldown;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public ClientWorld world;

    @Shadow
    @Nullable
    public HitResult crosshairTarget;

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void cpvpOptimizer$onHandleInputEventsHead(CallbackInfo ci) {
        if (!OptimizerConfig.getInstance().isMasterEnabled() || this.player == null || this.interactionManager == null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod == null) return;

        ClientPlayerInteractionManagerAccessor imAccessor = (ClientPlayerInteractionManagerAccessor) this.interactionManager;

        // 1. Instant Hotbar Slot Sync + Clear stale use cooldown on slot switch
        int currentSlot = this.player.getInventory().getSelectedSlot();
        if (mod.getHotbarOptimizer().checkSlotChanged(currentSlot)) {
            if (mod.getHotbarOptimizer().isInstantSlotSyncEnabled()) {
                imAccessor.cpvp$syncSelectedSlot();
            }
            if (mod.getHotbarOptimizer().shouldResetUseDelayOnSwitch()) {
                this.itemUseCooldown = 0;
                this.attackCooldown = 0;
            }
        }

        // 2. Clear left-click miss lock & block breaking cooldown so crystal/block hits never stall
        if (mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
            if (this.attackCooldown > 0) {
                this.attackCooldown = 0;
                mod.getLeftClickOptimizer().recordClearedMissPenalty();
            }
            if (imAccessor.cpvp$getBlockBreakingCooldown() > 0) {
                imAccessor.cpvp$setBlockBreakingCooldown(0);
            }
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void cpvpOptimizer$onBeforeDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!OptimizerConfig.getInstance().isMasterEnabled() || this.interactionManager == null) return;
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod != null && mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
            this.attackCooldown = 0;
            ((ClientPlayerInteractionManagerAccessor) this.interactionManager).cpvp$syncSelectedSlot();
        }
    }

    @Inject(method = "doAttack", at = @At("RETURN"))
    private void cpvpOptimizer$onAfterDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!OptimizerConfig.getInstance().isMasterEnabled()) return;
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod != null && this.attackCooldown > 0 && mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
            this.attackCooldown = 0;
            mod.getLeftClickOptimizer().recordClearedMissPenalty();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void cpvpOptimizer$onBeforeDoItemUse(CallbackInfo ci) {
        if (!OptimizerConfig.getInstance().isMasterEnabled() || this.player == null || this.interactionManager == null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod != null && mod.getHotbarOptimizer().isInstantSlotSyncEnabled()) {
            ((ClientPlayerInteractionManagerAccessor) this.interactionManager).cpvp$syncSelectedSlot();
        }
    }

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void cpvpOptimizer$onAfterDoItemUse(CallbackInfo ci) {
        if (!OptimizerConfig.getInstance().isMasterEnabled() || this.player == null || this.world == null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod == null || this.itemUseCooldown <= 0) return;

        ItemStack mainHand = this.player.getMainHandStack();

        // 1. Respawn Anchor Target Interaction (Charging with Glowstone or Detonating with any item/hand)
        if (this.crosshairTarget instanceof BlockHitResult blockHit && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockState targetState = this.world.getBlockState(blockHit.getBlockPos());
            if (targetState.isOf(Blocks.RESPAWN_ANCHOR)) {
                if (mainHand.isOf(Items.GLOWSTONE) && mod.getGlowstoneOptimizer().isZeroGlowstoneDelayEnabled()) {
                    this.itemUseCooldown = 0;
                    mod.getGlowstoneOptimizer().recordFastCharge();
                    return;
                }
                if (mod.getAnchorOptimizer().isInstantAnchorInteractEnabled()) {
                    this.itemUseCooldown = 0;
                    mod.getAnchorOptimizer().recordAnchorAction(targetState.get(RespawnAnchorBlock.CHARGES));
                    return;
                }
            }
        }

        // 2. End Crystal Placement (Zero Delay)
        if (mainHand.isOf(Items.END_CRYSTAL) && mod.getCrystalOptimizer().isZeroCrystalPlaceDelayEnabled()) {
            this.itemUseCooldown = 0;
            mod.getCrystalOptimizer().recordFastCrystalPlace();
            return;
        }

        // 3. Experience Bottle Throw (Zero Delay)
        if (mainHand.isOf(Items.EXPERIENCE_BOTTLE) && mod.getXpOptimizer().isFastXpThrowEnabled()) {
            this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getXpOptimizer().getXpUseDelayTicks());
            mod.getXpOptimizer().recordFastThrow();
            return;
        }

        // 4. Any Block Placement (Obsidian, Respawn Anchor, Glowstone, Cobweb, etc.)
        if (mainHand.getItem() instanceof BlockItem) {
            if (mainHand.isOf(Items.RESPAWN_ANCHOR) && mod.getAnchorOptimizer().isInstantAnchorInteractEnabled()) {
                this.itemUseCooldown = 0;
                mod.getAnchorOptimizer().recordAnchorAction(0);
                return;
            }
            if (mod.getBlockPlacementOptimizer().isZeroPlaceDelayEnabled()) {
                this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getBlockPlacementOptimizer().getPlacementCooldownTicks());
                mod.getBlockPlacementOptimizer().recordFastPlacement();
                return;
            }
        }

        // 5. All Other Right-Click / Throwables (Pearls, Potions, Snowballs, Offhand items, etc.)
        if (mod.getRightClickOptimizer().isImmediateUseEnabled()) {
            this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getRightClickOptimizer().getItemUseCooldownTicks());
            mod.getRightClickOptimizer().recordFastRightClick();
        }
    }
}
