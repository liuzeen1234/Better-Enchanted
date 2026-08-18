package com.example.hellomod.enchantment;

import com.example.hellomod.HelloMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 自定义附魔注册类。
 */
public class ModEnchantments {

    /**
     * 迅投 (Swift Throw) — 提升投掷物初速度，每级+50%原始初速度
     */
    public static final Enchantment SWIFT_THROW = Registry.register(
            Registries.ENCHANTMENT,
            new Identifier(HelloMod.MOD_ID, "swift_throw"),
            new SwiftThrowEnchantment()
    );

    /**
     * 在 ModInitializer 中调用以触发静态初始化完成注册。
     */
    public static void register() {
        HelloMod.LOGGER.info("[ModEnchantments] Registered custom enchantments.");
    }
}
