package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.damage.PowerPotionDamageSource;
import com.example.hellomod.damage.SharpPotionDamageSource;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin PotionEntity，在药水碰撞时应用锋利、力量、冲击和火矢附魔效果。
 *
 * 锋利 (Sharpness) [药水]：
 * - 掷出的药水砸中实体时造成伤害
 * - 伤害公式同食物锋利：0.5 * level + 0.5
 * - 对砸中位置周围4格范围内的实体造成伤害（参考喷溅药水的影响范围）
 *
 * 力量 (Power) [药水]：
 * - 掷出的药水砸中实体时造成伤害
 * - 伤害公式参考MC 1.20.4弓力量附魔：damage = (level * 0.5 + 0.5) * 2 = level + 1
 *   （即模拟箭矢base=2的力量加成公式）
 * - 与锋利伤害叠加生效
 * - 对砸中位置周围4格范围内的实体造成伤害（参考喷溅药水的影响范围）
 *
 * 冲击 (Punch) [药水]：
 * - 掷出的药水砸中实体时造成击退
 * - 参考MC 1.20.4弓冲击附魔（AbstractArrowEntity.onHit）：
 *   entity.takeKnockback(punchLevel * 0.6, -normalizedVelX, -normalizedVelZ)
 * - 击退方向为药水飞行方向（水平分量归一化）
 * - 对溅射范围内的实体也施加击退（按距离衰减）
 *
 * 火矢 (Flame) [药水]：
 * - 掷出的药水砸中实体时点燃目标
 * - 参考MC 1.20.4弓火矢附魔规则：
 *   直接命中的实体着火5秒（100 ticks）
 *   溅射范围内的实体也会着火，时长按距离衰减
 * - 火矢附魔只有1级（与原版一致）
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
        int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, potionStack);
        int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, potionStack);
        int flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, potionStack);

        if (sharpnessLevel <= 0 && powerLevel <= 0 && punchLevel <= 0 && flameLevel <= 0) {
            return;
        }

        // 锋利伤害公式：0.5 * level + 0.5
        float sharpnessDamage = sharpnessLevel > 0 ? (0.5f * sharpnessLevel + 0.5f) : 0f;

        // 力量伤害公式参考MC 1.20.4弓的Power附魔：
        // 箭矢base_damage=2, 力量增伤 = base_damage * (level * 0.5 + 0.5) = 2 * (level * 0.5 + 0.5) = level + 1
        // Power I = 2, Power II = 3, Power III = 4, Power IV = 5, Power V = 6
        float powerDamage = powerLevel > 0 ? (powerLevel + 1.0f) : 0f;

        // 锋利与力量叠加
        float totalDamage = sharpnessDamage + powerDamage;

        HelloMod.LOGGER.info("[PotionDamage] Potion hit! Sharpness level={} (dmg={}), Power level={} (dmg={}), Punch level={}, Flame level={}, total dmg={}",
                sharpnessLevel, sharpnessDamage, powerLevel, powerDamage, punchLevel, flameLevel, totalDamage);

        // 选择伤害源：如果两个附魔都有，优先使用力量的伤害源（伤害更高的那个）
        DamageSource damageSource = null;
        if (totalDamage > 0) {
            if (powerLevel > 0 && powerDamage >= sharpnessDamage) {
                damageSource = PowerPotionDamageSource.create(world);
            } else {
                damageSource = SharpPotionDamageSource.create(world);
            }
        }

        // 计算药水飞行方向（用于冲击击退方向）
        // 参考MC 1.20.4 AbstractArrowEntity.onHit：使用投射物的速度向量作为击退方向
        Vec3d velocity = self.getVelocity();
        double horizontalLength = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // 如果直接命中了实体
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity target = entityHitResult.getEntity();
            if (target instanceof LivingEntity livingTarget) {
                // 造成伤害
                if (totalDamage > 0 && damageSource != null) {
                    HelloMod.LOGGER.info("[PotionDamage] Direct hit on entity: {}, health BEFORE={}/{}, damage={}",
                            livingTarget.getName().getString(), livingTarget.getHealth(), livingTarget.getMaxHealth(), totalDamage);
                    livingTarget.damage(damageSource, totalDamage);
                    HelloMod.LOGGER.info("[PotionDamage] Direct hit on entity: {}, health AFTER={}/{}",
                            livingTarget.getName().getString(), livingTarget.getHealth(), livingTarget.getMaxHealth());
                }

                // 冲击击退：参考MC 1.20.4 AbstractArrowEntity.onHit 中的 Punch 逻辑
                // 原版代码：entity.takeKnockback(punchLevel * 0.6, -velocity.x, -velocity.z)
                // takeKnockback 内部会对方向归一化，所以传入原始速度分量即可
                if (punchLevel > 0 && horizontalLength > 0.0) {
                    double knockbackStrength = punchLevel * 0.6;
                    // takeKnockback(strength, x, z) 会将实体推向 (-x, 0, -z) 方向
                    // 我们要让实体沿药水飞行方向被推开，所以传入 velocity 分量的负值
                    // 这样 takeKnockback 内部取反后就是正的飞行方向
                    livingTarget.takeKnockback(knockbackStrength, -velocity.x / horizontalLength, -velocity.z / horizontalLength);
                    HelloMod.LOGGER.info("[PotionDamage] Punch knockback on entity: {}, strength={}, direction=({}, {})",
                            livingTarget.getName().getString(), knockbackStrength,
                            velocity.x / horizontalLength, velocity.z / horizontalLength);
                }

                // 火矢：参考MC 1.20.4弓火矢附魔规则
                // 原版 Flame 附魔使箭矢点燃目标5秒（100 ticks）
                if (flameLevel > 0) {
                    livingTarget.setOnFireFor(5);
                    HelloMod.LOGGER.info("[PotionDamage] Flame ignited entity: {} for 5 seconds (health: {}/{})",
                            livingTarget.getName().getString(), livingTarget.getHealth(), livingTarget.getMaxHealth());
                }
            }
        }

        // 对溅射范围内的实体也造成伤害和击退（参考喷溅药水的4格范围）
        Box splashBox = new Box(self.getX() - 4.0, self.getY() - 2.0, self.getZ() - 4.0,
                self.getX() + 4.0, self.getY() + 2.0, self.getZ() + 4.0);
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class, splashBox,
                entity -> entity.isAlive() && entity.squaredDistanceTo(self) <= 16.0);

        for (LivingEntity entity : nearbyEntities) {
            // 避免对直接命中的实体重复处理
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                if (entity == entityHitResult.getEntity()) {
                    continue;
                }
            }

            double distance = Math.sqrt(entity.squaredDistanceTo(self));
            if (distance < 4.0) {
                double factor = 1.0 - (distance / 4.0);

                // 溅射伤害（距离衰减）
                if (totalDamage > 0 && damageSource != null) {
                    float splashDamage = (float) (totalDamage * factor);
                    if (splashDamage > 0.5f) {
                        entity.damage(damageSource, splashDamage);
                        HelloMod.LOGGER.info("[PotionDamage] Splash damage {} to entity: {} (distance: {})",
                                splashDamage, entity.getName().getString(), distance);
                    }
                }

                // 溅射击退（距离衰减）
                // 溅射范围内的实体，击退方向为从药水落点指向实体的方向
                if (punchLevel > 0) {
                    double dx = entity.getX() - self.getX();
                    double dz = entity.getZ() - self.getZ();
                    double splashHorizontalLength = Math.sqrt(dx * dx + dz * dz);

                    if (splashHorizontalLength > 0.01) {
                        double splashKnockbackStrength = punchLevel * 0.6 * factor;
                        // takeKnockback 将实体推向 (-x, 0, -z) 方向，所以传入负的方向向量
                        entity.takeKnockback(splashKnockbackStrength, -dx / splashHorizontalLength, -dz / splashHorizontalLength);
                        HelloMod.LOGGER.info("[PotionDamage] Splash punch knockback {} to entity: {} (distance: {})",
                                splashKnockbackStrength, entity.getName().getString(), distance);
                    }
                }

                // 溅射火矢（距离衰减）
                // 溅射范围内的实体着火时长按距离衰减，最大5秒
                if (flameLevel > 0) {
                    int fireDuration = (int) Math.ceil(5 * factor);
                    if (fireDuration > 0) {
                        entity.setOnFireFor(fireDuration);
                        HelloMod.LOGGER.info("[PotionDamage] Splash flame ignited entity: {} for {} seconds (distance: {}, health: {}/{})",
                                entity.getName().getString(), fireDuration, distance, entity.getHealth(), entity.getMaxHealth());
                    }
                }
            }
        }
    }
}
