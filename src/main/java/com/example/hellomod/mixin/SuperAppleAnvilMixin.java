package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.advancement.UltimateAppleChecker;
import com.example.hellomod.item.ModItems;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧 Mixin：
 * 1. 锁定超级附魔金苹果的 RepairCost 为固定值 10，防止多次铁砧操作后惩罚指数增长。
 * 2. 玩家取出产物时，检查是否满足"终极金苹果"成就条件并授予。
 */
@Mixin(AnvilScreenHandler.class)
public abstract class SuperAppleAnvilMixin extends ForgingScreenHandler {

    private static final Identifier ULTIMATE_APPLE_ADVANCEMENT_ID =
            new Identifier(HelloMod.MOD_ID, "challenge/ultimate_golden_apple");

    public SuperAppleAnvilMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    /**
     * 注入 updateResult 方法尾部，在铁砧计算完结果后，
     * 如果输出物品是超级附魔金苹果，则将其 RepairCost 锁定为 10。
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void lockSuperAppleRepairCost(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack output = handler.getSlot(2).getStack();
        if (!output.isEmpty() && output.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            output.setRepairCost(10);
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
     * 授予"终极金苹果"成就并给予 1000 经验等级。
     */
    private void grantAdvancement(ServerPlayerEntity player) {
        AdvancementEntry advancement = player.getServer().getAdvancementLoader()
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

        // 给予 1000 经验等级
        player.addExperienceLevels(1000);

        HelloMod.LOGGER.info("[UltimateApple] Player {} achieved Ultimate Golden Apple!", player.getName().getString());
    }
}
