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

        int currentSlot = this.player.getInventory().getSelectedSlot();
        if (mod.getHotbarOptimizer().checkSlotChanged(currentSlot)) {
            if (mod.getHotbarOptimizer().isInstantSlotSyncEnabled()) {
                imAccessor.cpvp$syncSelectedSlot();
            }
            if (mod.getHotbarOptimizer().shouldResetUseDelayOnSwitch()) {
                ItemStack newlySelected = this.player.getMainHandStack();
                // Only clear use delay on hotbar switch if the target action's module is actually enabled
                if (!cpvpOptimizer$isEquippableGear(newlySelected)) {
                    if (newlySelected.getItem() instanceof BlockItem) {
                        if (mod.getBlockPlacementOptimizer().isZeroPlaceDelayEnabled()) {
                            this.itemUseCooldown = 0;
                        }
                    } else {
                        this.itemUseCooldown = 0;
                    }
                }
                if (mod.getLeftClickOptimizer().shouldClearMissPenalty()) {
                    this.attackCooldown = 0;
                }
            }
        }

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

        // GUARD 1: Never zero-cooldown Elytra or Armor equip/swap
        if (cpvpOptimizer$isEquippableGear(mainHand) || (mainHand.isEmpty() && cpvpOptimizer$isEquippableGear(offHand))) {
            return;
        }

        // GUARD 2: Continuous-use items (Shield, Bow, Food, Drinkable Potion, Ender Pearl)
        if (this.player.isUsingItem() || cpvpOptimizer$isContinuousUseOrCooldownItem(mainHand)) {
            return;
        }

        // Check targeted block
        if (this.crosshairTarget instanceof BlockHitResult blockHit && this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockState targetState = this.world.getBlockState(blockHit.getBlockPos());

            // STRICT ANCHOR / GLOWSTONE SEPARATION:
            // Only triggers when interacting with an ALREADY-PLACED Respawn Anchor in the world (and not sneaking to place a block against it)
            if (targetState.isOf(Blocks.RESPAWN_ANCHOR) && !this.player.isSneaking()) {
                int charges = targetState.get(RespawnAnchorBlock.CHARGES);
                if (mainHand.isOf(Items.GLOWSTONE) && charges < 4) {
                    if (mod.getGlowstoneOptimizer().isZeroGlowstoneDelayEnabled()) {
                        this.itemUseCooldown = 0;
                        mod.getGlowstoneOptimizer().recordFastCharge();
                    }
                    return;
                }
                // Detonating a charged Respawn Anchor (charges > 0 and not holding Glowstone)
                if (charges > 0 && !mainHand.isOf(Items.GLOWSTONE)) {
                    if (mod.getAnchorOptimizer().isInstantAnchorInteractEnabled()) {
                        this.itemUseCooldown = 0;
                        mod.getAnchorOptimizer().recordAnchorAction(charges);
                    }
                    return;
                }
                // If charges == 0 and holding a normal block, fall through to BlockPlacementOptimizer only!
            }

            // GUARD 3: Protect Containers, Doors, Trapdoors, Gates, Workstations
            if (!this.player.isSneaking() && cpvpOptimizer$isInteractiveContainerOrDoor(targetState.getBlock())) {
                return;
            }
        }

        // 2. End Crystal Placement (Strictly controlled by Crystal Optimizer)
        if (mainHand.isOf(Items.END_CRYSTAL)) {
            if (mod.getCrystalOptimizer().isZeroCrystalPlaceDelayEnabled()) {
                this.itemUseCooldown = 0;
                mod.getCrystalOptimizer().recordFastCrystalPlace();
            }
            return;
        }

        // 3. Experience Bottle Throw (Strictly controlled by XP Optimizer)
        if (mainHand.isOf(Items.EXPERIENCE_BOTTLE)) {
            if (mod.getXpOptimizer().isFastXpThrowEnabled()) {
                this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getXpOptimizer().getXpUseDelayTicks());
                mod.getXpOptimizer().recordFastThrow();
            }
            return;
        }

        // 4. Block Placement (Strictly controlled ONLY by Block Placement Optimizer!)
        // If Block Placement Optimizer is OFF, placing ANY block (including Respawn Anchor block or Obsidian) keeps vanilla 4-tick delay!
        if (mainHand.getItem() instanceof BlockItem) {
            if (mod.getBlockPlacementOptimizer().isZeroPlaceDelayEnabled()) {
                this.itemUseCooldown = Math.min(this.itemUseCooldown, mod.getBlockPlacementOptimizer().getPlacementCooldownTicks());
                mod.getBlockPlacementOptimizer().recordFastPlacement();
            }
            return;
        }

        // 5. Non-block Fast Right-Click throwables (Splash Potion, Snowball, Wind Charge, Firework)
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
