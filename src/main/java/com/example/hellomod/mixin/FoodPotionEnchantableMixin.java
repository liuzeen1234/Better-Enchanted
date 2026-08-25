package com.example.hellomod.mixin;

import com.example.hellomod.item.SuperEnchantedGoldenAppleItem;
import com.example.hellomod.item.UltimateEnchantedGoldenAppleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让食物和药水可以在附魔台中使用。
 *
 * 原版 Item.isEnchantable() 的默认实现要求 maxCount==1 && isDamageable()，
 * 食物和药水不满足这个条件，所以附魔台不接受它们。
 *
 * 原版 Item.getEnchantability() 默认返回0，
 * 附魔台用这个值影响附魔的随机选取（值越高越容易出高级附魔），
 * 为食物和药水提供合理的 enchantability 值。
 */
@Mixin(Item.class)
public abstract class FoodPotionEnchantableMixin {

    /**
     * 让食物和药水的 isEnchantable() 返回 true。
     * 这使得附魔台的槽位接受这些物品。
     */
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void makeFoodPotionEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Item self = (Item) (Object) this;
        if (self.isFood() || self instanceof PotionItem) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 为食物和药水提供 enchantability 值。
     * 超级/终极附魔金苹果 enchantability = 22（类似金质，更容易出高级附魔）
     * 普通食物 enchantability = 10（类似铁质工具的附魔能力）
     * 药水 enchantability = 15（类似下界合金，药水可选附魔多所以稍高）
     *
     * 参考原版值：
     * - 木质/皮革：15
     * - 石质/锁链/铁质：9-12
     * - 金质：22-25
     * - 钻石：10
     * - 下界合金：15
     */
    @Inject(method = "getEnchantability", at = @At("HEAD"), cancellable = true)
    private void provideFoodPotionEnchantability(CallbackInfoReturnable<Integer> cir) {
        Item self = (Item) (Object) this;
        if (self instanceof SuperEnchantedGoldenAppleItem || self instanceof UltimateEnchantedGoldenAppleItem) {
            cir.setReturnValue(22);
        } else if (self.isFood()) {
            cir.setReturnValue(10);
        } else if (self instanceof PotionItem) {
            cir.setReturnValue(15);
        }
    }
}
