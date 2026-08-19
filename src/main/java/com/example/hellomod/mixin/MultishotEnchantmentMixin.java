package com.example.hellomod.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.MultishotEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将多重射击 (Multishot) 附魔的最高合法等级提升到 10 级。
 * 原版仅为 1 级。
 *
 * 因为 MultishotEnchantment 没有自己重写 getMaxLevel()，
 * 所以 Mixin 目标为父类 Enchantment，并在注入中检查 this 是否为 MultishotEnchantment 实例。
 */
@Mixin(Enchantment.class)
public abstract class MultishotEnchantmentMixin {

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void modifyMultishotMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof MultishotEnchantment) {
            cir.setReturnValue(10);
        }
    }
}
