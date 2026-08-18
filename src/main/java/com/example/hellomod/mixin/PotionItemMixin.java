package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.enchantment.InfinityCooldownManager;
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
 * 3. 无限附魔 (Infinity)：投掷不消耗药水
 *    - 若同时有耐久：耐久判定成功 → 不进入冷却；判定失败 → 进入10s冷却
 *    - 若没有耐久：始终进入10s冷却
 *    - 冷却通过自定义 InfinityCooldownManager 管理（基于 NBT 标记）
 *    - 只有带 InfinityMarked NBT 的物品会受冷却影响，不影响同种未附魔物品
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

        // 如果没有任何附魔，让原版逻辑处理
        if (sharpnessLevel <= 0 && unbreakingLevel <= 0 && powerLevel <= 0
                && punchLevel <= 0 && flameLevel <= 0 && infinityLevel <= 0) {
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

        HelloMod.LOGGER.info("[PotionDebug] Throwing enchanted potion! Sharpness={}, Unbreaking={}, Power={}, Punch={}, Flame={}, Infinity={}",
                sharpnessLevel, unbreakingLevel, powerLevel, punchLevel, flameLevel, infinityLevel);

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

        // 消耗与冷却逻辑（仅服务端执行，避免单人游戏中客户端/服务端共享static状态的问题）
        if (!world.isClient() && !user.getAbilities().creativeMode) {
            if (infinityLevel > 0) {
                // 无限附魔：不消耗药水，但需要判定冷却
                if (unbreakingLevel > 0) {
                    // 有耐久附魔：进行耐久判定
                    // 判定成功（不消耗）→ 不进入冷却
                    // 判定失败（消耗）→ 进入10s冷却
                    if (user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
                        // 耐久判定成功：不冷却
                        HelloMod.LOGGER.info("[PotionDebug] Infinity + Unbreaking: durability check PASSED, no cooldown.");
                    } else {
                        // 耐久判定失败：进入10s冷却（自定义冷却，只影响带标记的物品）
                        InfinityCooldownManager.triggerCooldown(user);
                        HelloMod.LOGGER.info("[PotionDebug] Infinity + Unbreaking: durability check FAILED, 10s cooldown applied.");
                    }
                } else {
                    // 没有耐久附魔：始终进入10s冷却
                    InfinityCooldownManager.triggerCooldown(user);
                    HelloMod.LOGGER.info("[PotionDebug] Infinity without Unbreaking: 10s cooldown applied.");
                }
                HelloMod.LOGGER.info("[PotionDebug] Infinity active! Potion NOT consumed.");
            } else if (unbreakingLevel > 0 && user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
                // 仅耐久附魔（无无限）：耐久触发，不消耗药水
                HelloMod.LOGGER.info("[PotionDebug] Unbreaking triggered! Potion NOT consumed.");
            } else {
                // 无无限且无耐久 / 耐久判定失败：正常消耗
                stack.decrement(1);
                HelloMod.LOGGER.info("[PotionDebug] Potion consumed normally.");
            }
        }

        cir.setReturnValue(TypedActionResult.success(stack, world.isClient()));
    }
}
