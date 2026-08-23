package com.example.hellomod.item;

import com.example.hellomod.enchantment.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

import java.util.Map;

/**
 * 超级附魔金苹果合成后自动添加迅投 255 附魔。
 * 
 * 通过 Mixin CraftingResultSlot 或事件来调用此方法。
 * 这里提供一个工具方法，在合成输出时调用。
 */
public class SuperAppleCraftingHandler {

    /**
     * 为合成产出的超级附魔金苹果添加迅投 255 附魔。
     */
    public static ItemStack applyDefaultEnchantments(ItemStack stack) {
        if (stack.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            // 添加迅投 255
            stack.addEnchantment(ModEnchantments.SWIFT_THROW, 255);
        }
        return stack;
    }
}
