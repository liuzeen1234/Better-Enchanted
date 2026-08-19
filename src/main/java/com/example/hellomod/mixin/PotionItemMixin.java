package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.enchantment.InfinityCooldownManager;
import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.enchantment.SwiftThrowEnchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin 投掷型药水（喷溅药水 / 滞留药水）的 use 方法。
 *
 * 功能：
 * 1. 将药水上的附魔信息通过 NBT 传递给 PotionEntity（投射物）
 * 2. 耐久附魔：投掷后有概率不消耗药水
 * 3. 无限附魔 (Infinity)：投掷不消耗药水
 * 4. 迅投 (Swift Throw)：提升初速度 + 调整发射角度
 *    - 等级 1-20：正常物理投掷，速度按公式增加
 *    - 等级 >20：射线追踪模式，通过NBT标记让PotionEntityMixin处理瞬移逻辑
 */
@Mixin(ThrowablePotionItem.class)
public abstract class PotionItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);

        // 检查是否有我们关心的附魔
        int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
        int unbreakingLevel = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
        int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, stack);
        int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, stack);
        int flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, stack);
        int infinityLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, stack);
        int channelingLevel = EnchantmentHelper.getLevel(Enchantments.CHANNELING, stack);
        int swiftThrowLevel = EnchantmentHelper.getLevel(ModEnchantments.SWIFT_THROW, stack);

        // 如果没有任何附魔，让原版逻辑处理
        if (sharpnessLevel <= 0 && unbreakingLevel <= 0 && powerLevel <= 0
                && punchLevel <= 0 && flameLevel <= 0 && infinityLevel <= 0 && channelingLevel <= 0
                && swiftThrowLevel <= 0) {
            return;
        }

        // 确保带无限附魔的物品有 InfinityMarked 标记
        if (infinityLevel > 0 && !InfinityCooldownManager.isInfinityMarked(stack)) {
            InfinityCooldownManager.markInfinity(stack);
        }

        // 冷却检查（仅服务端）：如果物品带 InfinityMarked 标记且玩家处于冷却中，拦截使用
        if (!world.isClient() && InfinityCooldownManager.isInfinityMarked(stack) && InfinityCooldownManager.isOnCooldown(user)) {
            int remaining = InfinityCooldownManager.getRemainingCooldown(user);
            HelloMod.LOGGER.info("[PotionDebug] Infinity cooldown active! Remaining: {} ticks ({}s). Use blocked.",
                    remaining, String.format("%.1f", remaining / 20.0f));
            cir.setReturnValue(TypedActionResult.fail(stack));
            return;
        }

        HelloMod.LOGGER.info("[PotionDebug] Throwing enchanted potion! Sharpness={}, Unbreaking={}, Power={}, Punch={}, Flame={}, Infinity={}, Channeling={}, SwiftThrow={}",
                sharpnessLevel, unbreakingLevel, powerLevel, punchLevel, flameLevel, infinityLevel, channelingLevel, swiftThrowLevel);

        if (!world.isClient()) {
            // 创建药水实体
            PotionEntity potionEntity = new PotionEntity(world, user);
            potionEntity.setItem(stack);

            // 迅投附魔：提升初速度，每级增加50%原始初速度
            // 公式：实际初速度 = 原始初速度(0.5f) × (1 + 0.5 × 等级)
            float baseSpeed = 0.5f;
            float actualSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(swiftThrowLevel);

            // 迅投附魔：调整发射方向偏移角度
            // 公式：y = 80/(4+x)，y为向上偏移角度，x为附魔等级（x>=0）
            // x=0 时 y=20（原版药水行为），等级越高偏移越小（越接近平射）
            // 当偏移角度<1时直接设为0
            float pitchOffsetDeg = 80.0f / (4.0f + swiftThrowLevel);
            float pitchOffset = pitchOffsetDeg < 1.0f ? 0.0f : -pitchOffsetDeg;

            if (swiftThrowLevel > 20) {
                // 等级>20：射线追踪模式
                // 将速度限制在安全范围（5.5，等同于20级），方向正确
                // 通过NBT标记让SwiftThrowTickMixin每tick做射线追踪传送
                float safeLaunchSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(20);

                float adjustedPitch = user.getPitch() + pitchOffset;
                float yawRad = user.getYaw() * ((float) Math.PI / 180.0f);
                float pitchRad = adjustedPitch * ((float) Math.PI / 180.0f);

                double vx = -Math.sin(yawRad) * Math.cos(pitchRad);
                double vy = -Math.sin(pitchRad);
                double vz = Math.cos(yawRad) * Math.cos(pitchRad);

                Vec3d direction = new Vec3d(vx, vy, vz).normalize();

                // 设置安全速度用于生成（确保不会碰到自己）
                potionEntity.setVelocity(direction.multiply(safeLaunchSpeed));

                // 将生成位置沿发射方向前移
                potionEntity.setPosition(
                        potionEntity.getX() + direction.x * 1.5,
                        potionEntity.getY() + direction.y * 1.5,
                        potionEntity.getZ() + direction.z * 1.5
                );

                // 在药水的NBT中写入射线追踪信息
                // SwiftThrowTickMixin 会读取这些数据在每tick做射线追踪传送
                NbtCompound potionNbt = potionEntity.getStack().getOrCreateNbt();
                potionNbt.putBoolean("SwiftThrowRaycast", true);
                potionNbt.putFloat("SwiftThrowSpeed", actualSpeed);
                // 存储方向向量（归一化）
                potionNbt.putDouble("SwiftThrowDirX", direction.x);
                potionNbt.putDouble("SwiftThrowDirY", direction.y);
                potionNbt.putDouble("SwiftThrowDirZ", direction.z);

                // 隐藏药水实体（射线追踪模式不显示药水瓶）
                potionEntity.setInvisible(true);
                potionEntity.setNoGravity(true);

                // 生成暴击粒子弹道：沿发射方向每隔0.5格生成一个crit粒子
                if (world instanceof ServerWorld serverWorld) {
                    Vec3d particleStart = potionEntity.getPos();
                    double particleRange = Math.min(actualSpeed * 2, 64.0); // 最远64格粒子
                    double step = 0.5;
                    for (double d = 0; d < particleRange; d += step) {
                        double px = particleStart.x + direction.x * d;
                        double py = particleStart.y + direction.y * d;
                        double pz = particleStart.z + direction.z * d;
                        serverWorld.spawnParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0);
                    }
                }

                HelloMod.LOGGER.info("[SwiftThrow] Raycast mode! Level={}, speed={}, direction=({}, {}, {})",
                        swiftThrowLevel, actualSpeed, direction.x, direction.y, direction.z);

            } else if (swiftThrowLevel > 0) {
                // 等级1-20：正常物理投掷，手动计算方向
                float adjustedPitch = user.getPitch() + pitchOffset;
                float yawRad = user.getYaw() * ((float) Math.PI / 180.0f);
                float pitchRad = adjustedPitch * ((float) Math.PI / 180.0f);

                double vx = -Math.sin(yawRad) * Math.cos(pitchRad);
                double vy = -Math.sin(pitchRad);
                double vz = Math.cos(yawRad) * Math.cos(pitchRad);

                Vec3d direction = new Vec3d(vx, vy, vz).normalize().multiply(actualSpeed);
                potionEntity.setVelocity(direction);

                // 将生成位置沿发射方向前移
                Vec3d normalizedDir = direction.normalize();
                potionEntity.setPosition(
                        potionEntity.getX() + normalizedDir.x * 1.0,
                        potionEntity.getY() + normalizedDir.y * 1.0,
                        potionEntity.getZ() + normalizedDir.z * 1.0
                );

                HelloMod.LOGGER.info("[SwiftThrow] Normal mode! Level={}, speed={}", swiftThrowLevel, actualSpeed);
            } else {
                // 无迅投时使用原版逻辑
                potionEntity.setVelocity(user, user.getPitch(), user.getYaw(), -20.0f, actualSpeed, 1.0f);
            }

            // 将附魔信息写入投射物的自定义 NBT
            NbtCompound entityNbt = new NbtCompound();
            if (sharpnessLevel > 0) {
                entityNbt.putInt("SharpnessLevel", sharpnessLevel);
            }
            potionEntity.writeCustomDataToNbt(entityNbt);

            world.spawnEntity(potionEntity);
        }

        // 播放音效
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.PLAYERS,
                0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));

        // 统计
        user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));

        // 消耗与冷却逻辑（仅服务端执行）
        if (!world.isClient() && !user.getAbilities().creativeMode) {
            if (infinityLevel > 0) {
                if (unbreakingLevel > 0) {
                    if (user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
                        HelloMod.LOGGER.info("[PotionDebug] Infinity + Unbreaking: durability check PASSED, no cooldown.");
                    } else {
                        InfinityCooldownManager.triggerCooldown(user);
                        HelloMod.LOGGER.info("[PotionDebug] Infinity + Unbreaking: durability check FAILED, 10s cooldown applied.");
                    }
                } else {
                    InfinityCooldownManager.triggerCooldown(user);
                    HelloMod.LOGGER.info("[PotionDebug] Infinity without Unbreaking: 10s cooldown applied.");
                }
                HelloMod.LOGGER.info("[PotionDebug] Infinity active! Potion NOT consumed.");
            } else if (unbreakingLevel > 0 && user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
                HelloMod.LOGGER.info("[PotionDebug] Unbreaking triggered! Potion NOT consumed.");
            } else {
                stack.decrement(1);
                HelloMod.LOGGER.info("[PotionDebug] Potion consumed normally.");
            }
        }

        cir.setReturnValue(TypedActionResult.success(stack, world.isClient()));
    }
}
