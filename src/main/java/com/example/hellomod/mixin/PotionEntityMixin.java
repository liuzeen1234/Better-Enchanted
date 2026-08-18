package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.damage.SharpPotionDamageSource;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin PotionEntity，在药水碰撞时应用锋利附魔效果。
 *
 * 锋利 (Sharpness) [药水]：
 * - 掷出的药水砸中实体时造成伤害
 * - 伤害公式同食物锋利：0.5 * level + 0.5
 * - 对砸中位置周围4格范围内的实体造成伤害（参考喷溅药水的影响范围）
 */
@Mixin(PotionEntity.class)
public abstract class PotionEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"))
    private void onPotionCollision(HitResult hitResult, CallbackInfo ci) {
        PotionEntity self = (PotionEntity) (Object) this;
        World world = self.getWorld();

        if (world.isClient()) {
            return;
        }

        ItemStack potionStack = self.getStack();
        int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, potionStack);

        if (sharpnessLevel <= 0) {
            return;
        }

        // 伤害公式同食物锋利：0.5 * level + 0.5
        float damage = 0.5f * sharpnessLevel + 0.5f;

        HelloMod.LOGGER.info("[PotionSharpness] Potion hit! Sharpness level={}, damage={}", sharpnessLevel, damage);

        // 如果直接命中了实体，对该实体造成伤害
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity target = entityHitResult.getEntity();
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.damage(SharpPotionDamageSource.create(world), damage);
                HelloMod.LOGGER.info("[PotionSharpness] Direct hit on entity: {}", livingTarget.getName().getString());
            }
        }

        // 对溅射范围内的实体也造成伤害（参考喷溅药水的4格范围）
        Box splashBox = new Box(self.getX() - 4.0, self.getY() - 2.0, self.getZ() - 4.0,
                self.getX() + 4.0, self.getY() + 2.0, self.getZ() + 4.0);
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class, splashBox,
                entity -> entity.isAlive() && entity.squaredDistanceTo(self) <= 16.0);

        for (LivingEntity entity : nearbyEntities) {
            // 避免对直接命中的实体重复造成伤害
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                if (entity == entityHitResult.getEntity()) {
                    continue;
                }
            }

            // 根据距离衰减伤害（越近越疼，类似喷溅药水效果）
            double distance = Math.sqrt(entity.squaredDistanceTo(self));
            if (distance < 4.0) {
                double factor = 1.0 - (distance / 4.0);
                float splashDamage = (float) (damage * factor);
                if (splashDamage > 0.5f) {
                    entity.damage(SharpPotionDamageSource.create(world), splashDamage);
                    HelloMod.LOGGER.info("[PotionSharpness] Splash damage {} to entity: {} (distance: {})",
                            splashDamage, entity.getName().getString(), distance);
                }
            }
        }
    }
}
