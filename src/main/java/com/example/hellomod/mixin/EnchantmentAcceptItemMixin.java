package com.example.hellomod.mixin;

import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.item.SuperEnchantedGoldenAppleItem;
import com.example.hellomod.item.UltimateEnchantedGoldenAppleItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让食物和药水通过 Enchantment.isAcceptableItem() 的检查，
 * 使它们可以在铁砧上通过附魔书获得附魔。
 *
 * 食物支持的附魔：锋利、击退、火焰附加、效率、冰霜行者、耐久
 * 药水支持的附魔：锋利、力量、冲击、火矢、无限、耐久、多重射击、快速装填、穿透、引雷、忠诚、迅投
 * 超级/终极附魔金苹果：同时支持食物+药水全部附魔
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentAcceptItemMixin {

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void allowFoodAndPotionEnchanting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Enchantment self = (Enchantment) (Object) this;

        // 超级/终极附魔金苹果：同时支持食物和药水附魔
        if (stack.getItem() instanceof SuperEnchantedGoldenAppleItem
                || stack.getItem() instanceof UltimateEnchantedGoldenAppleItem) {
            if (isFoodEnchantment(self) || isPotionEnchantment(self)) {
                cir.setReturnValue(true);
            }
        } else if (stack.getItem().isFood() && isFoodEnchantment(self)) {
            cir.setReturnValue(true);
        } else if (stack.getItem() instanceof PotionItem && isPotionEnchantment(self)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 判断附魔是否为食物可用的附魔。
     */
    private static boolean isFoodEnchantment(Enchantment enchantment) {
        return enchantment == Enchantments.SHARPNESS
                || enchantment == Enchantments.KNOCKBACK
                || enchantment == Enchantments.FIRE_ASPECT
                || enchantment == Enchantments.EFFICIENCY
                || enchantment == Enchantments.FROST_WALKER
                || enchantment == Enchantments.UNBREAKING;
    }

    /**
     * 判断附魔是否为药水可用的附魔。
     */
    private static boolean isPotionEnchantment(Enchantment enchantment) {
        return enchantment == Enchantments.SHARPNESS
                || enchantment == Enchantments.POWER
                || enchantment == Enchantments.PUNCH
                || enchantment == Enchantments.FLAME
                || enchantment == Enchantments.INFINITY
                || enchantment == Enchantments.UNBREAKING
                || enchantment == Enchantments.MULTISHOT
                || enchantment == Enchantments.QUICK_CHARGE
                || enchantment == Enchantments.PIERCING
                || enchantment == Enchantments.CHANNELING
                || enchantment == Enchantments.LOYALTY
                || enchantment == ModEnchantments.SWIFT_THROW;
    }
}
