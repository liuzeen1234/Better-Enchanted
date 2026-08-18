package com.example.hellomod.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEatFoodMixin {

    /**
     * 当玩家吃食物时，如果食物上有锋利附魔，每级造成1点伤害（0.5颗心）
     */
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClient()) {
            int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
            if (sharpnessLevel > 0) {
                PlayerEntity player = (PlayerEntity) (Object) this;
                // 使用锋利附魔公式：0.5 × 等级 + 0.5
                float damage = 0.5f * sharpnessLevel + 0.5f;
                player.damage(world.getDamageSources().generic(), damage);
            }
        }
    }
}
