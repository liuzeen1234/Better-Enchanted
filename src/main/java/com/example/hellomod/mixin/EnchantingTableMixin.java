package com.example.hellomod.mixin;

import com.example.hellomod.item.SuperEnchantedGoldenAppleItem;
import com.example.hellomod.item.UltimateEnchantedGoldenAppleItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 让食物和药水可以在附魔台中附魔。
 *
 * 原版附魔台流程：
 * 1. EnchantmentScreenHandler slot 检查 item.isEnchantable() — 由 FoodPotionEnchantableMixin 处理
 * 2. EnchantmentHelper.generateEnchantments() 调用 getPossibleEntries(power, item)
 * 3. getPossibleEntries 用 enchantment.target.isAcceptableItem(item) 过滤
 *
 * 本 Mixin 注入 EnchantmentHelper.getPossibleEntries()，
 * 对食物和药水返回我们自定义的可用附魔列表。
 * 超级/终极附魔金苹果同时获得食物+药水全部附魔。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantingTableMixin {

    /**
     * 注入 getPossibleEntries，当物品是食物或药水时，
     * 将结果替换为我们定义的附魔列表。
     */
    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void addFoodPotionEnchantments(int power, ItemStack stack, boolean treasureAllowed,
                                                   CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        Item item = stack.getItem();
        boolean isFood = item.isFood();
        boolean isPotion = item instanceof PotionItem;
        boolean isSuperApple = item instanceof SuperEnchantedGoldenAppleItem
                || item instanceof UltimateEnchantedGoldenAppleItem;

        if (!isFood && !isPotion) {
            return;
        }

        // 原版返回的列表（对食物/药水通常为空，因为没有匹配的EnchantmentTarget）
        List<EnchantmentLevelEntry> originalList = cir.getReturnValue();

        // 如果原版已经有结果（不太可能，但以防万一），直接返回
        if (originalList != null && !originalList.isEmpty()) {
            return;
        }

        // 为食物/药水构建可用附魔列表
        // 超级/终极附魔金苹果同时获得食物+药水附魔
        List<EnchantmentLevelEntry> entries;
        if (isSuperApple) {
            entries = EnchantingTableHelper.buildEnchantmentEntries(power, stack, treasureAllowed, true, true);
        } else {
            entries = EnchantingTableHelper.buildEnchantmentEntries(power, stack, treasureAllowed, isFood, isPotion);
        }

        if (!entries.isEmpty()) {
            cir.setReturnValue(entries);
        }
    }
}
