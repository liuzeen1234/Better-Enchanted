package com.example.hellomod.block;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储放置的蛋糕的锋利附魔等级。
 * 使用坐标映射来追踪哪些蛋糕有附魔。
 */
public class CakeEnchantmentStorage {

    private static final Map<BlockPos, Integer> SHARPNESS_MAP = new HashMap<>();

    public static void set(BlockPos pos, int level) {
        if (level > 0) {
            SHARPNESS_MAP.put(pos.toImmutable(), level);
        }
    }

    public static int get(BlockPos pos) {
        return SHARPNESS_MAP.getOrDefault(pos instanceof net.minecraft.util.math.BlockPos.Mutable ? pos.toImmutable() : pos, 0);
    }

    public static void remove(BlockPos pos) {
        SHARPNESS_MAP.remove(pos instanceof net.minecraft.util.math.BlockPos.Mutable ? pos.toImmutable() : pos);
    }

    public static boolean has(BlockPos pos) {
        return SHARPNESS_MAP.containsKey(pos instanceof net.minecraft.util.math.BlockPos.Mutable ? pos.toImmutable() : pos);
    }
}
