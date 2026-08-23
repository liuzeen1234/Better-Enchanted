package com.example.hellomod.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超级附魔金苹果合成时药水效果合并工具类。
 *
 * 合并规则（来自设计文档）：
 * - 持续性效果：相同类型+相同等级 → 持续时间叠加；相同类型+不同等级 → 只保留高等级
 * - 瞬时效果：每瓶各触发一次，记录触发次数
 */
public class SuperApplePotionMerger {

    /**
     * 将药水效果按合并规则处理后写入 NBT。
     */
    public static void writeMergedEffects(NbtCompound nbt, String effectsKey, String instantKey, List<StatusEffectInstance> rawEffects) {
        // 分离瞬时效果和持续性效果
        List<StatusEffectInstance> instantEffects = new ArrayList<>();
        List<StatusEffectInstance> durationEffects = new ArrayList<>();

        for (StatusEffectInstance effect : rawEffects) {
            if (effect.getEffectType().isInstant()) {
                instantEffects.add(effect);
            } else {
                durationEffects.add(effect);
            }
        }

        // 合并持续性效果
        Map<String, int[]> mergedMap = new HashMap<>(); // value: [amplifier, duration]
        for (StatusEffectInstance effect : durationEffects) {
            String id = Registries.STATUS_EFFECT.getId(effect.getEffectType()).toString();
            int amplifier = effect.getAmplifier();
            int duration = effect.getDuration();

            int[] existing = mergedMap.get(id);
            if (existing == null) {
                mergedMap.put(id, new int[]{amplifier, duration});
            } else {
                if (amplifier == existing[0]) {
                    // 相同等级：持续时间叠加
                    existing[1] += duration;
                } else if (amplifier > existing[0]) {
                    // 更高等级：只保留高等级（时间取高等级的）
                    existing[0] = amplifier;
                    existing[1] = duration;
                }
                // 低等级则忽略
            }
        }

        // 写入持续性效果 NbtList
        NbtList effectsList = new NbtList();
        for (Map.Entry<String, int[]> entry : mergedMap.entrySet()) {
            NbtCompound effectNbt = new NbtCompound();
            effectNbt.putString("Id", entry.getKey());
            effectNbt.putInt("Amplifier", entry.getValue()[0]);
            effectNbt.putInt("Duration", entry.getValue()[1]);
            effectNbt.putBoolean("Instant", false);
            effectsList.add(effectNbt);
        }
        nbt.put(effectsKey, effectsList);

        // 合并瞬时效果 — 记录每种效果+等级的触发次数
        // key格式: "id:amplifier"
        Map<String, int[]> instantMap = new HashMap<>(); // value: [amplifier, count]
        List<String> instantIds = new ArrayList<>(); // 保持顺序
        for (StatusEffectInstance effect : instantEffects) {
            String id = Registries.STATUS_EFFECT.getId(effect.getEffectType()).toString();
            int amplifier = effect.getAmplifier();
            String key = id + ":" + amplifier;

            int[] existing = instantMap.get(key);
            if (existing == null) {
                instantMap.put(key, new int[]{amplifier, 1});
                instantIds.add(key);
            } else {
                existing[1]++;
            }
        }

        // 写入瞬时效果 NbtList
        NbtList instantList = new NbtList();
        for (String key : instantIds) {
            String id = key.substring(0, key.lastIndexOf(':'));
            int[] data = instantMap.get(key);
            NbtCompound icNbt = new NbtCompound();
            icNbt.putString("Id", id);
            icNbt.putInt("Amplifier", data[0]);
            icNbt.putInt("Count", data[1]);
            instantList.add(icNbt);
        }
        nbt.put(instantKey, instantList);
    }
}
