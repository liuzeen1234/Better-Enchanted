package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.advancement.UltimateAppleChecker;
import com.example.hellomod.item.ModItems;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧 Mixin：
 * 1. 超级附魔金苹果无视 "Too Expensive!" 的 39 级上限限制，始终可以在铁砧上操作。
 * 2. 锁定超级附魔金苹果的 RepairCost 为 0，防止惩罚无限增长。
 * 3. 玩家取出产物时，检查是否满足"终极金苹果"成就条件并授予。
 *
 * 实现原理：
 * - 在 updateResult HEAD 阶段，如果输入是超级金苹果，临时将玩家标记为"创造模式经验"
 *   这样原版代码在检查 player.getAbilities().creativeMode 时会跳过 Too Expensive 检查
 * - 在 updateResult TAIL 阶段，恢复标记，并将输出物品的 RepairCost 重置为 0
 */
@Mixin(AnvilScreenHandler.class)
public abstract class SuperAppleAnvilMixin extends ForgingScreenHandler {

    @Shadow @Final private Property levelCost;

    @Shadow
    private int repairItemUsage;

    /** 标记当前 updateResult 调用是否为超级金苹果操作 */
    @Unique
    private boolean hello_mod$isSuperAppleOperation = false;

    /** 备份玩家原始的创造模式状态 */
    @Unique
    private boolean hello_mod$originalCreativeMode = false;

    private static final Identifier ULTIMATE_APPLE_ADVANCEMENT_ID =
            new Identifier(HelloMod.MOD_ID, "challenge/ultimate_golden_apple");

    public SuperAppleAnvilMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    /**
     * 在 updateResult 开始前，如果第一个输入是超级附魔金苹果，
     * 临时将玩家设为创造模式，这样原版代码会跳过 "Too Expensive" 检查。
     */
    @Inject(method = "updateResult", at = @At("HEAD"))
    private void beforeUpdateResult(CallbackInfo ci) {
        hello_mod$isSuperAppleOperation = false;

        ItemStack input = this.input.getStack(0);
        if (!input.isEmpty() && (input.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE
                || input.getItem() == ModItems.ULTIMATE_ENCHANTED_GOLDEN_APPLE)) {
            // 获取玩家并临时设为创造模式
            if (this.player != null) {
                hello_mod$originalCreativeMode = this.player.getAbilities().creativeMode;
                this.player.getAbilities().creativeMode = true;
                hello_mod$isSuperAppleOperation = true;
            }
        }
    }

    /**
     * 在 updateResult 结束后，恢复玩家的创造模式状态，
     * 并将超级/终极金苹果的输出 RepairCost 锁定为 0。
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void afterUpdateResult(CallbackInfo ci) {
        if (hello_mod$isSuperAppleOperation && this.player != null) {
            // 恢复创造模式状态
            this.player.getAbilities().creativeMode = hello_mod$originalCreativeMode;
            hello_mod$isSuperAppleOperation = false;

            // 锁定输出物品的 RepairCost 为 10
            ItemStack output = this.output.getStack(0);
            if (!output.isEmpty() && (output.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE
                    || output.getItem() == ModItems.ULTIMATE_ENCHANTED_GOLDEN_APPLE)) {
                output.setRepairCost(10);
            }
        }
    }

    /**
     * 注入 onTakeOutput 方法头部，在玩家从铁砧取出物品时，
     * 检查产物是否满足"终极金苹果"成就条件。
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void checkUltimateAppleAdvancement(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (UltimateAppleChecker.isUltimate(stack)) {
                grantAdvancement(serverPlayer);
            }
        }
    }

    /**
     * 授予"终极金苹果"成就。
     * 奖励（经验+终极金苹果）由 AdvancementRewardMixin 在进度完成时统一处理。
     */
    private void grantAdvancement(ServerPlayerEntity player) {
        Advancement advancement = player.getServer().getAdvancementLoader()
                .get(ULTIMATE_APPLE_ADVANCEMENT_ID);
        if (advancement == null) {
            HelloMod.LOGGER.warn("[UltimateApple] Advancement not found: {}", ULTIMATE_APPLE_ADVANCEMENT_ID);
            return;
        }

        AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
        if (progress.isDone()) {
            // 成就已获得，不重复授予
            return;
        }

        // 授予所有 criteria（只有一个 "impossible"）
        for (String criterion : progress.getUnobtainedCriteria()) {
            player.getAdvancementTracker().grantCriterion(advancement, criterion);
        }

        HelloMod.LOGGER.info("[UltimateApple] Player {} achieved Ultimate Golden Apple!", player.getName().getString());
    }
}
