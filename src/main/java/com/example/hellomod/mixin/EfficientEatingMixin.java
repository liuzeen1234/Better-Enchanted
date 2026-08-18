package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 效率附魔：加快食用速度。
 * 每级效率减少10%的进食时间（即加快10%的进食速度）。
 * 例如：效率I = 减少10%时间（28.8 tick ≈ 29 tick），效率V = 减少50%时间（16 tick）。
 *
 * 实现方式：Mixin 到 Item.getMaxUseTime()，当物品是食物且有效率附魔时，减少返回值。
 */
@Mixin(Item.class)
public abstract class EfficientEatingMixin {

    @Inject(method = "getMaxUseTime", at = @At("RETURN"), cancellable = true)
    private void onGetMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        // 只处理食物物品
        if (!stack.getItem().isFood()) {
            return;
        }

        int efficiencyLevel = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
        if (efficiencyLevel > 0) {
            int originalTime = cir.getReturnValue();
            // 每级减少10%的进食时间，最低保留1 tick
            double reduction = efficiencyLevel * 0.1;
            // 上限为90%减少（效率最高9级，但实用最高5级=50%减少）
            reduction = Math.min(reduction, 0.9);
            int newTime = Math.max(1, (int) (originalTime * (1.0 - reduction)));

            HelloMod.LOGGER.info("[EfficiencyDebug] Efficiency level: {}, original time: {}, new time: {}",
                    efficiencyLevel, originalTime, newTime);
            cir.setReturnValue(newTime);
        }
    }
}
