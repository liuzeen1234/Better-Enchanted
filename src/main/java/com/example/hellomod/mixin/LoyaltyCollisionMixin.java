package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 忠诚 (Loyalty) 附魔 — 碰撞处理部分
 *
 * 在药水处于忠诚返回阶段时：
 * - 碰到投掷者本人：跳过碰撞（由 LoyaltyPotionMixin 的距离检测来归还物品）
 * - 碰到其他实体或方块：正常触发碰撞（药水消耗掉）
 *
 * 使用 priority 700 确保此检查在 PiercingPotionMixin(900) 和 PotionEntityMixin(1000) 之前执行。
 */
@Mixin(value = PotionEntity.class, priority = 700)
public abstract class LoyaltyCollisionMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onLoyaltyCollisionCheck(HitResult hitResult, CallbackInfo ci) {
        PotionEntity self = (PotionEntity) (Object) this;
        World world = self.getWorld();

        if (world.isClient()) {
            return;
        }

        ItemStack potionStack = self.getStack();
        int loyaltyLevel = EnchantmentHelper.getLevel(Enchantments.LOYALTY, potionStack);

        if (loyaltyLevel <= 0) {
            return;
        }

        NbtCompound nbt = potionStack.getNbt();

        // 只在返回阶段处理
        if (nbt == null || !nbt.getBoolean("LoyaltyReturning")) {
            return;
        }

        // 如果碰到的是实体，且该实体是投掷者本人，则跳过碰撞
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity hitEntity = entityHitResult.getEntity();
            Entity owner = self.getOwner();

            if (owner != null && hitEntity == owner) {
                // 碰到投掷者，不触发碰撞（由 LoyaltyPotionMixin tick 中的距离检测来归还物品）
                HelloMod.LOGGER.info("[Loyalty] Skipping collision with owner during return phase.");
                ci.cancel();
                return;
            }
        }

        // 碰到其他实体或方块：正常触发碰撞（药水消耗掉）
        HelloMod.LOGGER.info("[Loyalty] Potion hit {} during return phase. Will be consumed.",
                hitResult.getType() == HitResult.Type.ENTITY ? "entity" : "block");
    }
}
