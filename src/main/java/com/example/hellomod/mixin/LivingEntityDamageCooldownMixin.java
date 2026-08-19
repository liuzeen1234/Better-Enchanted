package com.example.hellomod.mixin;

import com.example.hellomod.damage.ModDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 移除药水附魔伤害和闪电伤害的受伤无敌帧（damage cooldown / invulnerability ticks）。
 *
 * 原版 LivingEntity.damage() 在受到伤害后会检查 timeUntilRegen，
 * 在此期间后续伤害会被忽略（除非伤害值更高则只补差值）。
 *
 * 这导致药水同时附魔锋利+力量时只有一个生效，或者引雷的闪电伤害被之前的
 * 药水伤害产生的无敌帧挡住。
 *
 * 本 Mixin 在 damage() 方法开头检查伤害源类型，如果是以下三种之一：
 * - sharp_potion（锋利药水伤害）
 * - power_potion（力量药水伤害）
 * - lightning_bolt（闪电伤害）
 * 则将 hurtTime 和 timeUntilRegen（通过 Entity 层级）重置为 0，使伤害可以立即生效。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageCooldownMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void bypassDamageCooldown(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // 检查伤害源类型是否是需要跳过冷却的类型
        if (source.isOf(ModDamageTypes.SHARP_POTION)
                || source.isOf(ModDamageTypes.POWER_POTION)
                || source.isOf(DamageTypes.LIGHTNING_BOLT)) {
            // 重置受伤冷却，让伤害立即生效
            LivingEntity self = (LivingEntity) (Object) this;
            self.timeUntilRegen = 0;
            self.hurtTime = 0;
        }
    }
}
