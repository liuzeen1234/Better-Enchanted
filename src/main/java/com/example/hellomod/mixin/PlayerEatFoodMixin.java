package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.damage.SharpFoodDamageSource;
import com.example.hellomod.effect.FrostWalkerFoodEffect;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEatFoodMixin {

    /**
     * 当玩家吃食物时，如果食物上有锋利附魔，每级造成伤害。
     * 注入在 HEAD 保证在食物被消耗前读取附魔信息。
     */
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        HelloMod.LOGGER.info("[FoodDebug] eatFood HEAD called! isClient={}, Item: {}, NBT: {}",
                world.isClient(), stack.getItem(), stack.getNbt());

        if (!world.isClient()) {
            // 锋利附魔：造成伤害（使用自定义伤害源，死亡消息显示食物名称）
            int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
            if (sharpnessLevel > 0) {
                float damage = 0.5f * sharpnessLevel + 0.5f;
                HelloMod.LOGGER.info("[FoodDebug] Sharpness level: {}, damage: {}", sharpnessLevel, damage);
                player.damage(SharpFoodDamageSource.create(world, stack), damage);
            }

            // 击退附魔：对食用者施加击退
            int knockbackLevel = EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, stack);
            HelloMod.LOGGER.info("[FoodDebug] Knockback level: {}", knockbackLevel);
            if (knockbackLevel > 0) {
                applyKnockback(player, knockbackLevel);
            }

            // 火焰附加附魔：点燃食用者
            // 参考MC 1.20.4原版火焰附加逻辑：在EnchantmentHelper.onTargetDamaged中
            // 调用target.setOnFireFor(level * 4)，即每级点燃4秒
            int fireAspectLevel = EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, stack);
            HelloMod.LOGGER.info("[FoodDebug] Fire Aspect level: {}", fireAspectLevel);
            if (fireAspectLevel > 0) {
                player.setOnFireFor(fireAspectLevel * 4);
                HelloMod.LOGGER.info("[FoodDebug] Set player on fire for {} seconds", fireAspectLevel * 4);
            }

            // 冰霜行者附魔：获得冰霜行者效果
            int frostWalkerLevel = EnchantmentHelper.getLevel(Enchantments.FROST_WALKER, stack);
            HelloMod.LOGGER.info("[FoodDebug] Frost Walker level: {}", frostWalkerLevel);
            if (frostWalkerLevel > 0) {
                FrostWalkerFoodEffect.apply(player, frostWalkerLevel);
                HelloMod.LOGGER.info("[FoodDebug] Applied Frost Walker level {} to player", frostWalkerLevel);
            }
        }
    }

    /**
     * 对玩家施加击退效果。
     * 参考MC 1.20.4原版击退逻辑（PlayerEntity.attack中对目标调用takeKnockback）：
     * - 在 attack() 中：target.takeKnockback(0.5 * knockbackLevel, sin(attackerYaw), -cos(attackerYaw))
     * - takeKnockback(strength, x, z) 内部会将实体推向 (-x, 0, -z) 方向（归一化后乘以strength）
     * - 这里传入玩家面朝方向作为"攻击来源方向"，实现往身后推的效果
     */
    private static void applyKnockback(PlayerEntity player, int level) {
        // 随机方向击退
        double angle = player.getRandom().nextDouble() * 2.0 * Math.PI;
        double dirX = Math.sin(angle);
        double dirZ = -Math.cos(angle);

        double strength = 0.5 * level;

        Vec3d currentVel = player.getVelocity();
        double newX = currentVel.x / 2.0 - dirX * strength;
        double newY = player.isOnGround() ? Math.min(0.4, currentVel.y / 2.0 + strength) : currentVel.y;
        double newZ = currentVel.z / 2.0 - dirZ * strength;

        player.setVelocity(newX, newY, newZ);
        player.velocityModified = true;
    }
}
