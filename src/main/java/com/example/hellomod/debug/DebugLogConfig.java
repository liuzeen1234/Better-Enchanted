package com.example.hellomod.debug;

import com.example.hellomod.config.ModConfig;

/**
 * 调试日志开关配置。
 * 控制9种调试日志的输出开关，运行时生效，默认全部关闭。
 * 所有状态委托给 ModConfig 进行持久化存储。
 */
public class DebugLogConfig {

    // ========== Getters ==========

    public static boolean isCakeDebugEnabled() {
        return ModConfig.isCakeDebugEnabled();
    }

    public static boolean isPlaceDebugEnabled() {
        return ModConfig.isPlaceDebugEnabled();
    }

    public static boolean isFoodDebugEnabled() {
        return ModConfig.isFoodDebugEnabled();
    }

    public static boolean isPotionDebugEnabled() {
        return ModConfig.isPotionDebugEnabled();
    }

    public static boolean isPotionDamageEnabled() {
        return ModConfig.isPotionDamageEnabled();
    }

    public static boolean isSwiftThrowEnabled() {
        return ModConfig.isSwiftThrowEnabled();
    }

    public static boolean isClientDebugEnabled() {
        return ModConfig.isClientDebugEnabled();
    }

    public static boolean isFrostWalkerEnabled() {
        return ModConfig.isFrostWalkerEnabled();
    }

    public static boolean isInfinityCooldownEnabled() {
        return ModConfig.isInfinityCooldownEnabled();
    }

    public static boolean isPlayerBehaviorLogEnabled() {
        return ModConfig.isPlayerBehaviorLogEnabled();
    }

    // ========== Toggles ==========

    public static void toggleCakeDebug() {
        ModConfig.setCakeDebugEnabled(!ModConfig.isCakeDebugEnabled());
    }

    public static void togglePlaceDebug() {
        ModConfig.setPlaceDebugEnabled(!ModConfig.isPlaceDebugEnabled());
    }

    public static void toggleFoodDebug() {
        ModConfig.setFoodDebugEnabled(!ModConfig.isFoodDebugEnabled());
    }

    public static void togglePotionDebug() {
        ModConfig.setPotionDebugEnabled(!ModConfig.isPotionDebugEnabled());
    }

    public static void togglePotionDamage() {
        ModConfig.setPotionDamageEnabled(!ModConfig.isPotionDamageEnabled());
    }

    public static void toggleSwiftThrow() {
        ModConfig.setSwiftThrowEnabled(!ModConfig.isSwiftThrowEnabled());
    }

    public static void toggleClientDebug() {
        ModConfig.setClientDebugEnabled(!ModConfig.isClientDebugEnabled());
    }

    public static void toggleFrostWalker() {
        ModConfig.setFrostWalkerEnabled(!ModConfig.isFrostWalkerEnabled());
    }

    public static void toggleInfinityCooldown() {
        ModConfig.setInfinityCooldownEnabled(!ModConfig.isInfinityCooldownEnabled());
    }

    public static void togglePlayerBehaviorLog() {
        ModConfig.setPlayerBehaviorLogEnabled(!ModConfig.isPlayerBehaviorLogEnabled());
    }
}
