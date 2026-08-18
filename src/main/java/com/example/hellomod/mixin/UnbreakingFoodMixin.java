package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 耐久附魔（Unbreaking）应用于食物：食用后有概率不消耗食物。
 *
 * 参考MC 1.20.4原版耐久附魔逻辑（EnchantmentHelper.shouldSkipDurabilityLoss）：
 * - 对于非盔甲物品：有 level / (level + 1) 的概率跳过耐久消耗
 * - 即消耗概率 = 1 / (level + 1)
 * - 耐久I：50%不消耗，耐久II：66.7%不消耗，耐久III：75%不消耗
 *
 * 实现方式：在 eatFood 的 HEAD 记录食用前数量，
 * 在 RETURN 时如果耐久检查通过，恢复1个（即抵消这一次消耗），
 * 并通过发送 slot 更新包强制同步客户端。
 */
@Mixin(PlayerEntity.class)
public abstract class UnbreakingFoodMixin {

    @Unique
    private int hellomod_countBeforeEat = -1;

    @Unique
    private int hellomod_unbreakingLevel = 0;

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFoodHead(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient()) {
            hellomod_unbreakingLevel = 0;
            return;
        }

        hellomod_unbreakingLevel = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
        if (hellomod_unbreakingLevel > 0) {
            hellomod_countBeforeEat = stack.getCount();
            HelloMod.LOGGER.info("[UnbreakingDebug] HEAD: count before eat = {}, unbreaking level = {}",
                    hellomod_countBeforeEat, hellomod_unbreakingLevel);
        }
    }

    @Inject(method = "eatFood", at = @At("RETURN"))
    private void onEatFoodReturn(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient() || hellomod_unbreakingLevel <= 0) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack resultStack = cir.getReturnValue();

        int countAfterEat = resultStack.getCount();
        HelloMod.LOGGER.info("[UnbreakingDebug] RETURN: count after eat = {}, saved before = {}",
                countAfterEat, hellomod_countBeforeEat);

        // 参考MC 1.20.4 EnchantmentHelper中的耐久逻辑：
        // if (random.nextInt(level + 1) > 0) return true; // 跳过消耗
        if (player.getRandom().nextInt(hellomod_unbreakingLevel + 1) > 0) {
            // 耐久触发：恢复1个，但不超过食用前的数量
            int newCount = Math.min(countAfterEat + 1, hellomod_countBeforeEat);
            resultStack.setCount(newCount);
            HelloMod.LOGGER.info("[UnbreakingDebug] Unbreaking triggered! count: {} -> {}",
                    countAfterEat, newCount);

            // 强制同步客户端：发送 slot 更新包
            if (player instanceof ServerPlayerEntity serverPlayer) {
                int selectedSlot = serverPlayer.getInventory().selectedSlot;
                // ScreenHandlerSlotUpdateS2CPacket 参数：
                // syncId (-2 表示直接更新玩家背包 slot，不经过 screen handler)
                // revision (0)
                // slot index (快捷栏 slot = 36 + selectedSlot)
                // stack
                serverPlayer.networkHandler.sendPacket(
                        new ScreenHandlerSlotUpdateS2CPacket(-2, 0, selectedSlot, resultStack.copy())
                );
            }
        } else {
            HelloMod.LOGGER.info("[UnbreakingDebug] Unbreaking did NOT trigger. Food consumed normally. count = {}",
                    countAfterEat);
        }

        // 重置状态
        hellomod_countBeforeEat = -1;
        hellomod_unbreakingLevel = 0;
    }
}
