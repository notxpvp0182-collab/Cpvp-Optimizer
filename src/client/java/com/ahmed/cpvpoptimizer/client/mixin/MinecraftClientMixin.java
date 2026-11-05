package com.ahmed.cpvpoptimizer.client.mixin;

import com.ahmed.cpvpoptimizer.client.CPVPOptimizerClient;
import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void cpvpOptimizer$onHandleInputEventsHead(CallbackInfo ci) {
        if (!OptimizerConfig.getInstance().isMasterEnabled()
                || this.player == null
                || this.interactionManager == null
                || this.currentScreen != null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod == null) return;

        ClientPlayerInteractionManagerAccessor imAccessor =
                (ClientPlayerInteractionManagerAccessor) this.interactionManager;

        // 1. Instant Hotbar Slot Sync + safe cooldown reset on slot switch
        int currentSlot = this.player.getInventory().getSelectedSlot();
        if (mod.getHotbarOptimizer().checkSlotChanged(currentSlot)) {
            if (mod.getHotbarOptimizer().isInstantSlotSyncEnabled()) {
                imAccessor.cpvp$syncSelectedSlot();
            }
            if (mod.getHotbarOptimizer().shouldResetUseDelayOnSwitch()) {
                ItemStack newlySelected = this.player.getMainHandStack();
                // Do NOT zero cooldown if switching to Elytra/Armor to prevent accidental double-swap
                if (!cpvpOptimizer$isEquippableGear(newlySelected)) {
                    this.itemUseCooldown = 0;
                }
                this.attackCooldown = 0;
            }
        }

        // 2. Clear left-click miss lock & block breaking cooldown
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
        if (!OptimizerConfig.getInstance().isMasterEnabled()
                || this.interactionManager == null
                || this.currentScreen != null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod != null && mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
            this.attackCooldown = 0;
            ((ClientPlayerInteractionManagerAccessor) this.interactionManager).cpvp$syncSelectedSlot();
        }
    }

    @Inject(method = "doAttack", at = @At("RETURN"))
    private void cpvpOptimizer$onAfterDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!OptimizerConfig.getInstance().isMasterEnabled() || this.currentScreen != null) return;
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod != null && this.attackCooldown > 0 && mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
            this.attackCooldown = 0;
            mod.getLeftClickOptimizer().recordClearedMissPenalty();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void cpvpOptimizer$onBeforeDoItemUse(CallbackInfo ci) {
        if (!OptimizerConfig.getInstance().isMasterEnabled()
                || this.player == null
                || this.interactionManager == null
                || this.currentScreen != null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod != null && mod.getHotbarOptimizer().isInstantSlotSyncEnabled()) {
            ((ClientPlayerInteractionManagerAccessor) this.interactionManager).cpvp$syncSelectedSlot();
        }
    }

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void cpvpOptimizer$onAfterDoItemUse(CallbackInfo ci) {
        if (!OptimizerConfig.getInstance().isMasterEnabled()
                || this.player == null
                || this.world == null
                || this.currentScreen != null) {
            return;
        }
        CPVPOptimizerClient mod = CPVPOptimizerClient.getInstance();
        if (mod == null || this.itemUseCooldown <= 0) return;

        ItemStack mainHand = this.player.getMainHandStack();
        ItemStack offHand = this.player.getOffHandStack();

        // GUARD 1: Never zero-cooldown Elytra or Armor equip/swap!
        // In 1.21.11, right-clicking swaps armor/elytra with your chestplate.
        // Zeroing cooldown causes a single click to swap and immediately un-swap on the next tick.
        if (cpvpOptimizer$isEquippableGear(mainHand) || (mainHand.isEmpty() && cpvpOptimizer$isEquippableGear(offHand))) {
            return;
        }

        // GUARD 2: If player is holding a continuous-use item (Shield, Bow, Crossbow, Food, Potion, Trident),
        // never zero-cooldown or it will stutter/interrupt the use animation.
        if (this.player.isUsingItem() || cpvpOptimizer$isContinuousUseOrCooldownItem(mainHand)) {
            return;
        }

        // Check targeted block
        if (this.crosshairTarget instanceof BlockHitResult blockHit && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockState targetState = this.world.getBlockState(blockHit.getBlockPos());

            // 1. Respawn Anchor Target Interaction (Charging with Glowstone or Detonating)
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

            // GUARD 3: Protect Containers, Doors, Trapdoors, Gates, Levers, and Workstations when not sneaking
            // so a single right-click opens them cleanly without double-toggling.
            if (!this.player.isSneaking() && cpvpOptimizer$isInteractiveContainerOrDoor(targetState.getBlock())) {
                return;
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

        // 4. Block Placement (Obsidian, Respawn Anchor, Glowstone, Cobweb, etc.)
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

        // 5. Safe Fast Right-Click for splash/lingering potions, snowballs, eggs, firework rockets
        if (mod.getRightClickOptimizer().isImmediateUseEnabled() && cpvpOptimizer$isSafeFastRightClickItem(mainHand)) {
            this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getRightClickOptimizer().getItemUseCooldownTicks());
            mod.getRightClickOptimizer().recordFastRightClick();
        }
    }

    @Unique
    private static boolean cpvpOptimizer$isEquippableGear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isOf(Items.ELYTRA)) return true;
        return stack.contains(DataComponentTypes.EQUIPPABLE);
    }

    @Unique
    private static boolean cpvpOptimizer$isContinuousUseOrCooldownItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.SHIELD)) {
            return true;
        }
        return stack.getMaxUseTime(MinecraftClient.getInstance().player) > 0;
    }

    @Unique
    private static boolean cpvpOptimizer$isSafeFastRightClickItem(ItemStack stack) {
        return stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION)
                || stack.isOf(Items.SNOWBALL)
                || stack.isOf(Items.EGG)
                || stack.isOf(Items.WIND_CHARGE)
                || stack.isOf(Items.FIREWORK_ROCKET);
    }

    @Unique
    private static boolean cpvpOptimizer$isInteractiveContainerOrDoor(Block block) {
        return block instanceof BlockWithEntity
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock
                || block instanceof StonecutterBlock
                || block instanceof CartographyTableBlock
                || block instanceof SmithingTableBlock
                || block instanceof BedBlock;
    }
}
