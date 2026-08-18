package com.example.hellomod.block;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储放置的蛋糕的附魔等级。
 * 使用坐标映射来追踪哪些蛋糕有附魔。
 */
public class CakeEnchantmentStorage {

    private static final Map<BlockPos, Integer> SHARPNESS_MAP = new HashMap<>();
    private static final Map<BlockPos, Integer> KNOCKBACK_MAP = new HashMap<>();
    private static final Map<BlockPos, Integer> FIRE_ASPECT_MAP = new HashMap<>();
    private static final Map<BlockPos, Integer> EFFICIENCY_MAP = new HashMap<>();

    // === 锋利 ===

    public static void set(BlockPos pos, int level) {
        if (level > 0) {
            SHARPNESS_MAP.put(pos.toImmutable(), level);
        }
    }

    public static int get(BlockPos pos) {
        return SHARPNESS_MAP.getOrDefault(normalizePos(pos), 0);
    }

    // === 击退 ===

    public static void setKnockback(BlockPos pos, int level) {
        if (level > 0) {
            KNOCKBACK_MAP.put(pos.toImmutable(), level);
        }
    }

    public static int getKnockback(BlockPos pos) {
        return KNOCKBACK_MAP.getOrDefault(normalizePos(pos), 0);
    }

    // === 火焰附加 ===

    public static void setFireAspect(BlockPos pos, int level) {
        if (level > 0) {
            FIRE_ASPECT_MAP.put(pos.toImmutable(), level);
        }
    }

    public static int getFireAspect(BlockPos pos) {
        return FIRE_ASPECT_MAP.getOrDefault(normalizePos(pos), 0);
    }

    // === 效率 ===

    public static void setEfficiency(BlockPos pos, int level) {
        if (level > 0) {
            EFFICIENCY_MAP.put(pos.toImmutable(), level);
        }
    }

    public static int getEfficiency(BlockPos pos) {
        return EFFICIENCY_MAP.getOrDefault(normalizePos(pos), 0);
    }

    // === 通用 ===

    public static void remove(BlockPos pos) {
        BlockPos normalized = normalizePos(pos);
        SHARPNESS_MAP.remove(normalized);
        KNOCKBACK_MAP.remove(normalized);
        FIRE_ASPECT_MAP.remove(normalized);
        EFFICIENCY_MAP.remove(normalized);
    }

    public static boolean has(BlockPos pos) {
        BlockPos normalized = normalizePos(pos);
        return SHARPNESS_MAP.containsKey(normalized) || KNOCKBACK_MAP.containsKey(normalized) || FIRE_ASPECT_MAP.containsKey(normalized) || EFFICIENCY_MAP.containsKey(normalized);
    }

    private static BlockPos normalizePos(BlockPos pos) {
        return pos instanceof net.minecraft.util.math.BlockPos.Mutable ? pos.toImmutable() : pos;
    }
}
