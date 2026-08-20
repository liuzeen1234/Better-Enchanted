package com.example.hellomod.item;

import com.example.hellomod.HelloMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * 自定义物品注册类。
 */
public class ModItems {

    public static final Item SUPER_ENCHANTED_GOLDEN_APPLE = Registry.register(
            Registries.ITEM,
            new Identifier(HelloMod.MOD_ID, "super_enchanted_golden_apple"),
            new SuperEnchantedGoldenAppleItem(new FabricItemSettings()
                    .maxCount(64)
                    .rarity(Rarity.EPIC))
    );

    /**
     * 在 ModInitializer 中调用以触发静态初始化完成注册。
     */
    public static void register() {
        // 将物品添加到食物创造模式标签页
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(SUPER_ENCHANTED_GOLDEN_APPLE);
        });

        // 也添加到战斗标签页（因为有投掷模式）
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SUPER_ENCHANTED_GOLDEN_APPLE);
        });

        HelloMod.LOGGER.info("[ModItems] Registered custom items.");
    }
}
