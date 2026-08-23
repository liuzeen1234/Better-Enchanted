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
 * 等级1: 1.5x  等级2: 2.0x  等级3: 2.5x  等级4: 3.0x ... 等级10: 6.0x
 *
 * 附魔台最高等级：10（仅可附到书上，通过MinPower限制）
 * 铁砧合法最高等级：19（getMaxLevel()=19，无法触发>20级射线追踪模式）
 * 理论最高等级：255
 */
public class SwiftThrowEnchantment extends Enchantment {

    public SwiftThrowEnchantment() {
        // 使用 EnchantmentTarget.VANISHABLE 作为占位，实际通过 isAcceptableItem 控制只能附到书上
        super(Rarity.RARE, EnchantmentTarget.VANISHABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        // 返回19以允许铁砧合并到19级，但无法达到20级触发射线追踪模式
        // 附魔台通过 getMinPower/getMaxPower 限制只能出到10级
        return 19;
    }

    @Override
    public int getMinPower(int level) {
        if (level <= 10) {
            // 1~10级：MinPower范围 1~28，在附魔台30级范围内可达
            return (int) (1 + (level - 1) * 3);
        }
        // 11~25级：MinPower > 50，附魔台不可能达到，只能通过铁砧合并获得
        return 50 + (level - 11) * 10;
    }

    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 15;
    }

    /**
     * 控制附魔台上只能附到附魔书上。
     * 铁砧操作不受此限制（铁砧使用 Enchantment.isAcceptableItem 但也可通过附魔书转移）。
     */
    @Override
    public boolean isAcceptableItem(net.minecraft.item.ItemStack stack) {
        // 只接受附魔书
        return stack.getItem() instanceof net.minecraft.item.EnchantedBookItem
                || stack.getItem() instanceof net.minecraft.item.BookItem;
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
