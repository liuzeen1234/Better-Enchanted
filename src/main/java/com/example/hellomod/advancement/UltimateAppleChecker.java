package com.example.hellomod.advancement;

import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.item.ModItems;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 终极金苹果成就判定工具类。
 * 
 * 检查一个超级附魔金苹果是否同时拥有所有 14 种有效附魔，
 * 且每种附魔均为合法最高等级。
 */
public class UltimateAppleChecker {

    /**
     * 完美附魔表：附魔 -> 要求的最高等级
     */
    private static final Map<Enchantment, Integer> REQUIRED_ENCHANTMENTS = new LinkedHashMap<>();

    static {
        // 自定义附魔
        REQUIRED_ENCHANTMENTS.put(ModEnchantments.SWIFT_THROW, 25);
        // 原版附魔
        REQUIRED_ENCHANTMENTS.put(Enchantments.SHARPNESS, 5);
        REQUIRED_ENCHANTMENTS.put(Enchantments.POWER, 5);
        REQUIRED_ENCHANTMENTS.put(Enchantments.PUNCH, 2);
        REQUIRED_ENCHANTMENTS.put(Enchantments.FLAME, 1);
        REQUIRED_ENCHANTMENTS.put(Enchantments.CHANNELING, 1);
        REQUIRED_ENCHANTMENTS.put(Enchantments.MULTISHOT, 10);
        REQUIRED_ENCHANTMENTS.put(Enchantments.INFINITY, 1);
        REQUIRED_ENCHANTMENTS.put(Enchantments.UNBREAKING, 3);
        REQUIRED_ENCHANTMENTS.put(Enchantments.QUICK_CHARGE, 3);
        REQUIRED_ENCHANTMENTS.put(Enchantments.KNOCKBACK, 2);
        REQUIRED_ENCHANTMENTS.put(Enchantments.FIRE_ASPECT, 2);
        REQUIRED_ENCHANTMENTS.put(Enchantments.EFFICIENCY, 5);
        REQUIRED_ENCHANTMENTS.put(Enchantments.FROST_WALKER, 2);
    }

    /**
     * 检查物品是否满足"终极金苹果"条件。
     *
     * @param stack 待检查的物品
     * @return true 表示该物品是超级附魔金苹果且拥有全部 14 种附魔在最高等级
     */
    public static boolean isUltimate(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            return false;
        }

        for (Map.Entry<Enchantment, Integer> entry : REQUIRED_ENCHANTMENTS.entrySet()) {
            int level = EnchantmentHelper.getLevel(entry.getKey(), stack);
            if (level < entry.getValue()) {
                return false;
            }
        }

        return true;
    }
}
