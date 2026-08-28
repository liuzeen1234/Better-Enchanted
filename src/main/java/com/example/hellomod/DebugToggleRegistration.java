package com.example.hellomod;

import com.debugmenu.api.DebugMenuApi;
import com.debugmenu.api.DebugToggleEntry;
import com.example.hellomod.debug.DebugLogConfig;

/**
 * 可选的调试开关注册。
 * 仅在 debug-menu Mod 存在时被调用。
 *
 * <p>将主 Mod 的各种调试日志开关注册到调试菜单 API 中，
 * 使其可以通过调试菜单统一控制。
 */
public class DebugToggleRegistration {

    public static void register() {
        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "cake_debug", "蛋糕食用调试",
                DebugLogConfig::isCakeDebugEnabled,
                DebugLogConfig::setCakeDebugEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "place_debug", "蛋糕放置调试",
                DebugLogConfig::isPlaceDebugEnabled,
                DebugLogConfig::setPlaceDebugEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "food_debug", "食物食用调试",
                DebugLogConfig::isFoodDebugEnabled,
                DebugLogConfig::setFoodDebugEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "potion_debug", "药水投掷调试",
                DebugLogConfig::isPotionDebugEnabled,
                DebugLogConfig::setPotionDebugEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "potion_damage", "药水伤害调试",
                DebugLogConfig::isPotionDamageEnabled,
                DebugLogConfig::setPotionDamageEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "swift_throw", "迅投附魔调试",
                DebugLogConfig::isSwiftThrowEnabled,
                DebugLogConfig::setSwiftThrowEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "client_debug", "客户端手持调试",
                DebugLogConfig::isClientDebugEnabled,
                DebugLogConfig::setClientDebugEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "frost_walker", "冰霜行者调试",
                DebugLogConfig::isFrostWalkerEnabled,
                DebugLogConfig::setFrostWalkerEnabled
        ));

        DebugMenuApi.register(new DebugToggleEntry(
                "better-enchanted", "infinity_cooldown", "无限冷却调试",
                DebugLogConfig::isInfinityCooldownEnabled,
                DebugLogConfig::setInfinityCooldownEnabled
        ));

        HelloMod.LOGGER.info("[BetterEnchanted] Debug toggles registered to debug-menu.");
    }
}
