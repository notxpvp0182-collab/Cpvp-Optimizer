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

/**
 * Narrowly-scoped, high-responsiveness Mixin into MinecraftClient:
 * 1. Eliminates the 4-tick itemUseCooldown bottleneck for Block Placement, Respawn Anchors,
 *    Glowstone charging, End Crystals, and XP Bottles when the corresponding modules are enabled.
 * 2. Immediately synchronizes hotbar slot switches before item-use or attack dispatch.
 * 3. Clears the 10-tick client miss-swing attack lock when LeftClickOptimizer is enabled.
 */
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

        // Instant Hotbar Slot Sync + Clear stale use cooldown on slot switch
        int currentSlot = this.player.getInventory().getSelectedSlot();
        if (mod.getHotbarOptimizer().checkSlotChanged(currentSlot)) {
            if (mod.getHotbarOptimizer().isInstantSlotSyncEnabled()) {
                this.interactionManager.syncSelectedSlot();
            }
            if (mod.getHotbarOptimizer().shouldResetUseDelayOnSwitch() && this.itemUseCooldown > 0) {
                this.itemUseCooldown = 0;
            }
        }

        // Clear left-click miss penalty so crystal/target attacks respond immediately
        if (this.attackCooldown > 0 && mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
            this.attackCooldown = 0;
            mod.getLeftClickOptimizer().recordClearedMissPenalty();
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
            // Ensure server knows the exact hotbar slot BEFORE sending any block-place / anchor packet
            this.interactionManager.syncSelectedSlot();
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

        // 1. Respawn Anchor Target Interaction (Charging or Detonating)
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

        // 2. End Crystal Placement
        if (mainHand.isOf(Items.END_CRYSTAL) && mod.getCrystalOptimizer().isZeroCrystalPlaceDelayEnabled()) {
            this.itemUseCooldown = 0;
            mod.getCrystalOptimizer().recordFastCrystalPlace();
            return;
        }

        // 3. Experience Bottle Throw
        if (mainHand.isOf(Items.EXPERIENCE_BOTTLE) && mod.getXpOptimizer().isFastXpThrowEnabled()) {
            this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getXpOptimizer().getXpUseDelayTicks());
            mod.getXpOptimizer().recordFastThrow();
            return;
        }

        // 4. Block Placement (Obsidian, Respawn Anchor item, Glowstone block, etc.)
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

        // 5. General Right-Click Optimizer
        if (mod.getRightClickOptimizer().isImmediateUseEnabled()) {
            this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getRightClickOptimizer().getItemUseCooldownTicks());
            mod.getRightClickOptimizer().recordFastRightClick();
        }
    }
}
