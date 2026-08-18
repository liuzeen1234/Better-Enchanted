package com.example.hellomod.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

/**
 * 迅投 (Swift Throw) 附魔
 *
 * 效果：附魔在投掷物上时提升投掷物的初速度。
 * 每级增加50%的原始初速度。
 * 公式：实际初速度 = 原始初速度 × (1 + 0.5 × 等级)
 *
 * 等级1: 1.5x  等级2: 2.0x  等级3: 2.5x  等级4: 3.0x
 *
 * 非作弊最高等级：4
 * 理论最高等级：255
 */
public class SwiftThrowEnchantment extends Enchantment {

    public SwiftThrowEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getMinPower(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 15;
    }

    /**
     * 根据附魔等级计算速度倍率。
     * 公式：multiplier = 1 + 0.5 * level
     *
     * @param level 附魔等级
     * @return 速度倍率
     */
    public static float getSpeedMultiplier(int level) {
        if (level <= 0) return 1.0f;
        return 1.0f + 0.5f * level;
    }
}
