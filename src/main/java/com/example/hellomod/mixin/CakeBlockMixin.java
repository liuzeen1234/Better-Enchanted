package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.block.CakeEnchantmentStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public abstract class CakeBlockMixin {

    /**
     * 注入 tryEat 静态方法，当玩家成功吃蛋糕时应用附魔效果（锋利伤害 + 击退）。
     */
    @Inject(method = "tryEat", at = @At("RETURN"))
    private static void onTryEat(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        HelloMod.LOGGER.info("[CakeDebug] tryEat called! Result: {}", cir.getReturnValue());
        if (cir.getReturnValue() == ActionResult.SUCCESS) {
            if (world instanceof World realWorld && !realWorld.isClient()) {
                // 锋利附魔：造成伤害
                int sharpnessLevel = CakeEnchantmentStorage.get(pos);
                HelloMod.LOGGER.info("[CakeDebug] Server side - SharpnessLevel from storage: {}", sharpnessLevel);
                if (sharpnessLevel > 0) {
                    float damage = 0.5f * sharpnessLevel + 0.5f;
                    HelloMod.LOGGER.info("[CakeDebug] Dealing {} damage to player", damage);
                    player.damage(realWorld.getDamageSources().generic(), damage);
                }

                // 击退附魔：对食用者施加击退
                int knockbackLevel = CakeEnchantmentStorage.getKnockback(pos);
                HelloMod.LOGGER.info("[CakeDebug] Server side - KnockbackLevel from storage: {}", knockbackLevel);
                if (knockbackLevel > 0) {
                    applyKnockback(player, knockbackLevel);
                    HelloMod.LOGGER.info("[CakeDebug] Applied knockback level {} to player", knockbackLevel);
                }

                // 火焰附加附魔：点燃食用者
                // 参考MC 1.20.4原版火焰附加逻辑：target.setOnFireFor(level * 4)
                int fireAspectLevel = CakeEnchantmentStorage.getFireAspect(pos);
                HelloMod.LOGGER.info("[CakeDebug] Server side - FireAspectLevel from storage: {}", fireAspectLevel);
                if (fireAspectLevel > 0) {
                    player.setOnFireFor(fireAspectLevel * 4);
                    HelloMod.LOGGER.info("[CakeDebug] Set player on fire for {} seconds (level {})", fireAspectLevel * 4, fireAspectLevel);
                }

                // 如果蛋糕被吃完了（方块不再是蛋糕），移除存储
                if (!realWorld.getBlockState(pos).isOf(net.minecraft.block.Blocks.CAKE)) {
                    CakeEnchantmentStorage.remove(pos);
                    HelloMod.LOGGER.info("[CakeDebug] Cake fully eaten, removed storage at {}", pos);
                }
            }
        }
    }

    /**
     * 对玩家施加击退效果。
     * 参考MC 1.20.4原版击退逻辑：
     * - 击退方向为玩家面朝方向的反方向（食物把人往后推）
     * - 强度 = 0.5 * 等级
     */
    private static void applyKnockback(PlayerEntity player, int level) {
        // 随机方向击退
        double angle = player.getRandom().nextDouble() * 2.0 * Math.PI;
        double dirX = Math.sin(angle);
        double dirZ = -Math.cos(angle);

        double strength = 0.5 * level;
        net.minecraft.util.math.Vec3d currentVel = player.getVelocity();
        double newX = currentVel.x / 2.0 - dirX * strength;
        double newY = player.isOnGround() ? Math.min(0.4, currentVel.y / 2.0 + strength) : currentVel.y;
        double newZ = currentVel.z / 2.0 - dirZ * strength;

        player.setVelocity(newX, newY, newZ);
        player.velocityModified = true;
    }
}
