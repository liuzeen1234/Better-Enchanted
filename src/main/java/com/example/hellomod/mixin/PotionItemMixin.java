package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
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
 *    - 参考MC 1.20.4原版耐久逻辑：消耗概率 = 1/(level+1)
 *    - 即 level/(level+1) 的概率不消耗
 *    - 与无限互斥（后续实现）
 */
@Mixin(ThrowablePotionItem.class)
public abstract class PotionItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);

        // 检查是否有我们关心的附魔
        int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
        int unbreakingLevel = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);

        // 如果没有任何附魔，让原版逻辑处理
        if (sharpnessLevel <= 0 && unbreakingLevel <= 0) {
            return;
        }

        HelloMod.LOGGER.info("[PotionDebug] Throwing enchanted potion! Sharpness={}, Unbreaking={}",
                sharpnessLevel, unbreakingLevel);

        if (!world.isClient()) {
            // 创建药水实体
            PotionEntity potionEntity = new PotionEntity(world, user);
            potionEntity.setItem(stack);
            potionEntity.setVelocity(user, user.getPitch(), user.getYaw(), -20.0f, 0.5f, 1.0f);

            // 将附魔信息写入投射物的自定义 NBT
            // PotionEntity 通过 setItem 保存了 ItemStack，附魔信息已经在 ItemStack 中
            // 但我们额外存一份到 entity NBT 以便命中时快速读取
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

        // 耐久附魔：有概率不消耗
        if (!user.getAbilities().creativeMode) {
            if (unbreakingLevel > 0 && user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
                // 耐久触发：不消耗药水
                HelloMod.LOGGER.info("[PotionDebug] Unbreaking triggered! Potion NOT consumed.");
            } else {
                // 正常消耗
                stack.decrement(1);
                HelloMod.LOGGER.info("[PotionDebug] Potion consumed normally.");
            }
        }

        cir.setReturnValue(TypedActionResult.success(stack, world.isClient()));
    }
}
