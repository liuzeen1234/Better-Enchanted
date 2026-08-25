package com.example.hellomod.mixin;

import com.example.hellomod.enchantment.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 附魔台辅助类：为食物和药水构建可用的附魔列表。
 * 模仿原版 EnchantmentHelper.getPossibleEntries() 的逻辑，
 * 根据附魔力度(power)计算每个附魔可能出现的等级。
 */
public class EnchantingTableHelper {

    /**
     * 食物可附魔列表：锋利、击退、火焰附加、效率、冰霜行者、耐久
     */
    private static final Enchantment[] FOOD_ENCHANTMENTS = {
            Enchantments.SHARPNESS,
            Enchantments.KNOCKBACK,
            Enchantments.FIRE_ASPECT,
            Enchantments.EFFICIENCY,
            Enchantments.FROST_WALKER,
            Enchantments.UNBREAKING
    };

    /**
     * 药水可附魔列表：锋利、力量、冲击、火矢、无限、耐久、多重射击、快速装填、穿透、引雷、忠诚、迅投
     */
    private static final Enchantment[] POTION_ENCHANTMENTS = {
            Enchantments.SHARPNESS,
            Enchantments.POWER,
            Enchantments.PUNCH,
            Enchantments.FLAME,
            Enchantments.INFINITY,
            Enchantments.UNBREAKING,
            Enchantments.MULTISHOT,
            Enchantments.QUICK_CHARGE,
            Enchantments.PIERCING,
            Enchantments.CHANNELING,
            Enchantments.LOYALTY,
            ModEnchantments.SWIFT_THROW
    };

    /**
     * 为食物/药水构建附魔台可出现的附魔及等级列表。
     * 参考原版 EnchantmentHelper.getPossibleEntries() 的逻辑：
     * - 遍历所有可用附魔
     * - 对每个附魔，从最高等级向下查找第一个满足 minPower <= power 的等级
     * - 同时检查 power <= maxPower（上限检查）
     * - 宝藏附魔需要 treasureAllowed=true
     *
     * 当 isFood 和 isPotion 同时为 true 时（超级/终极金苹果），
     * 返回食物+药水附魔的合并列表（去重）。
     *
     * @param power           附魔力度（由附魔台等级和周围书架计算）
     * @param stack           要附魔的物品
     * @param treasureAllowed 是否允许宝藏附魔
     * @param isFood          是否为食物
     * @param isPotion        是否为药水
     * @return 可用的附魔等级列表
     */
    public static List<EnchantmentLevelEntry> buildEnchantmentEntries(int power, ItemStack stack,
                                                                      boolean treasureAllowed,
                                                                      boolean isFood, boolean isPotion) {
        List<EnchantmentLevelEntry> entries = new ArrayList<>();
        // 用 Set 去重（超级/终极金苹果同时 isFood+isPotion，锋利/耐久等在两个列表中都有）
        java.util.Set<Enchantment> addedEnchantments = new java.util.HashSet<>();

        Enchantment[][] enchantmentSets;
        if (isFood && isPotion) {
            // 超级/终极附魔金苹果：同时使用食物和药水附魔列表
            enchantmentSets = new Enchantment[][]{FOOD_ENCHANTMENTS, POTION_ENCHANTMENTS};
        } else if (isFood) {
            enchantmentSets = new Enchantment[][]{FOOD_ENCHANTMENTS};
        } else {
            enchantmentSets = new Enchantment[][]{POTION_ENCHANTMENTS};
        }

        for (Enchantment[] allowedEnchantments : enchantmentSets) {
            for (Enchantment enchantment : allowedEnchantments) {
                // 去重
                if (addedEnchantments.contains(enchantment)) {
                    continue;
                }

                // 宝藏附魔检查（冰霜行者是宝藏附魔）
                if (enchantment.isTreasure() && !treasureAllowed) {
                    continue;
                }

                // 从最高等级向下遍历，找到第一个满足条件的等级
                for (int level = enchantment.getMaxLevel(); level >= 1; level--) {
                    int minPower = enchantment.getMinPower(level);
                    int maxPower = enchantment.getMaxPower(level);

                    if (power >= minPower && power <= maxPower) {
                        entries.add(new EnchantmentLevelEntry(enchantment, level));
                        addedEnchantments.add(enchantment);
                        break;
                    }
                }
            }
        }

        return entries;
    }
}
