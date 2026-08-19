package com.example.hellomod.enchantment;

import com.example.hellomod.HelloMod;
import com.example.hellomod.debug.DebugLogConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无限附魔自定义冷却管理器。
 *
 * 设计思路：
 * - 原版 ItemCooldownManager 是按 Item 类型冷却，会导致同种未附魔物品也被冷却
 * - 本管理器通过 NBT 标记 "InfinityMarked" 来识别附有无限的物品
 * - 冷却触发时，所有带有 InfinityMarked 标记的物品统一进入冷却
 * - 弓和弩排除在外（它们使用原版无限机制）
 *
 * NBT 标记：
 * - key: "InfinityMarked"
 * - value: 1b (byte)
 * - 位于 ItemStack 的根 NBT 中
 */
public class InfinityCooldownManager {

    /** NBT 标记 key */
    public static final String INFINITY_MARKED_KEY = "InfinityMarked";

    /** 冷却时间：30秒 = 600 ticks */
    public static final int COOLDOWN_TICKS = 600;

    /** 玩家 UUID -> 剩余冷却 ticks */
    private static final Map<UUID, Integer> cooldownMap = new ConcurrentHashMap<>();

    /**
     * 注册 tick 事件，每 tick 递减冷却计时器。
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            cooldownMap.replaceAll((uuid, remaining) -> remaining - 1);
            cooldownMap.values().removeIf(remaining -> remaining <= 0);
        });
    }

    /**
     * 触发无限附魔冷却（默认30秒），并同步到客户端。
     */
    public static void triggerCooldown(PlayerEntity player) {
        triggerCooldown(player, COOLDOWN_TICKS);
    }

    /**
     * 触发无限附魔冷却（自定义时长），并同步到客户端。
     * 用于快速装填 (Quick Charge) 减少冷却时间。
     *
     * @param player 玩家
     * @param cooldownTicks 冷却时间（ticks）
     */
    public static void triggerCooldown(PlayerEntity player, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            // 冷却时间为0或负数时不触发冷却
            if (DebugLogConfig.isInfinityCooldownEnabled()) HelloMod.LOGGER.info("[InfinityCooldown] Cooldown skipped for player: {} (duration=0)",
                    player.getName().getString());
            return;
        }
        cooldownMap.put(player.getUuid(), cooldownTicks);
        if (DebugLogConfig.isInfinityCooldownEnabled()) HelloMod.LOGGER.info("[InfinityCooldown] Cooldown triggered for player: {}, duration: {}ticks ({}s)",
                player.getName().getString(), cooldownTicks, String.format("%.1f", cooldownTicks / 20.0f));

        // 同步冷却状态到客户端（用于渲染冷却动画）
        if (player instanceof ServerPlayerEntity serverPlayer) {
            InfinityCooldownSync.sendCooldownToClient(serverPlayer, cooldownTicks, cooldownTicks);
        }
    }

    /**
     * 计算快速装填 (Quick Charge) 附魔减少后的冷却时间。
     * 每级减少20%冷却时间：实际冷却 = 基础冷却 × (1 - 0.2 × level)
     * Lv I = 24s(480t), Lv II = 18s(360t), Lv III = 12s(240t), Lv IV = 6s(120t), Lv V = 0s(0t)
     *
     * @param quickChargeLevel 快速装填附魔等级
     * @return 减少后的冷却 ticks
     */
    public static int getReducedCooldown(int quickChargeLevel) {
        if (quickChargeLevel <= 0) {
            return COOLDOWN_TICKS;
        }
        double reduction = 1.0 - 0.2 * quickChargeLevel;
        if (reduction <= 0) {
            return 0;
        }
        return (int) (COOLDOWN_TICKS * reduction);
    }

    /**
     * 检查玩家是否处于无限附魔冷却中。
     */
    public static boolean isOnCooldown(PlayerEntity player) {
        Integer remaining = cooldownMap.get(player.getUuid());
        return remaining != null && remaining > 0;
    }

    /**
     * 获取玩家剩余冷却 ticks。
     */
    public static int getRemainingCooldown(PlayerEntity player) {
        Integer remaining = cooldownMap.get(player.getUuid());
        return remaining != null ? remaining : 0;
    }

    /**
     * 检查物品是否带有 InfinityMarked 标记。
     */
    public static boolean isInfinityMarked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(INFINITY_MARKED_KEY);
    }

    /**
     * 为物品添加 InfinityMarked 标记。
     * 排除弓和弩（它们使用原版无限机制）。
     */
    public static void markInfinity(ItemStack stack) {
        if (stack.isEmpty()) return;
        // 排除弓和弩
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
            return;
        }
        stack.getOrCreateNbt().putBoolean(INFINITY_MARKED_KEY, true);
    }

    /**
     * 检查物品是否应该被标记（有无限附魔且非弓/弩）。
     */
    public static boolean shouldBeMarked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
            return false;
        }
        return net.minecraft.enchantment.EnchantmentHelper.getLevel(
                net.minecraft.enchantment.Enchantments.INFINITY, stack) > 0;
    }

    /**
     * 玩家断开连接时清理冷却数据。
     */
    public static void onPlayerDisconnect(UUID playerUuid) {
        cooldownMap.remove(playerUuid);
    }
}
