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
     * 注入 tryEat 静态方法，当玩家成功吃蛋糕时造成锋利伤害。
     */
    @Inject(method = "tryEat", at = @At("RETURN"))
    private static void onTryEat(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<ActionResult> cir) {
        HelloMod.LOGGER.info("[CakeDebug] tryEat called! Result: {}", cir.getReturnValue());
        if (cir.getReturnValue() == ActionResult.SUCCESS) {
            if (world instanceof World realWorld && !realWorld.isClient()) {
                int sharpnessLevel = CakeEnchantmentStorage.get(pos);
                HelloMod.LOGGER.info("[CakeDebug] Server side - SharpnessLevel from storage: {}", sharpnessLevel);
                if (sharpnessLevel > 0) {
                    float damage = 0.5f * sharpnessLevel + 0.5f;
                    HelloMod.LOGGER.info("[CakeDebug] Dealing {} damage to player", damage);
                    player.damage(realWorld.getDamageSources().generic(), damage);

                    // 如果蛋糕被吃完了（方块不再是蛋糕），移除存储
                    if (!realWorld.getBlockState(pos).isOf(net.minecraft.block.Blocks.CAKE)) {
                        CakeEnchantmentStorage.remove(pos);
                        HelloMod.LOGGER.info("[CakeDebug] Cake fully eaten, removed storage at {}", pos);
                    }
                }
            }
        }
    }
}
