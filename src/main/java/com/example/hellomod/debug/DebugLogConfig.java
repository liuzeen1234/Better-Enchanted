package com.example.hellomod.debug;

/**
 * 调试日志开关配置。
 * 控制9种调试日志的输出开关，运行时生效，默认全部开启。
 */
public class DebugLogConfig {

    /** [CakeDebug] 蛋糕食用调试 */
    private static boolean cakeDebugEnabled = true;

    /** [PlaceDebug] 蛋糕放置调试 */
    private static boolean placeDebugEnabled = true;

    /** [FoodDebug] 普通食物食用调试 */
    private static boolean foodDebugEnabled = true;

    /** [PotionDebug] 药水投掷调试 */
    private static boolean potionDebugEnabled = true;

    /** [PotionDamage] 药水伤害调试 */
    private static boolean potionDamageEnabled = true;

    /** [SwiftThrow] 迅投附魔调试 */
    private static boolean swiftThrowEnabled = true;

    /** [ClientDebug] 客户端手持物品调试 */
    private static boolean clientDebugEnabled = true;

    /** [FrostWalker] 冰霜行者效果调试 */
    private static boolean frostWalkerEnabled = true;

    /** [InfinityCooldown] 无限附魔冷却调试 */
    private static boolean infinityCooldownEnabled = true;

    // ========== Getters ==========

    public static boolean isCakeDebugEnabled() {
        return cakeDebugEnabled;
    }

    public static boolean isPlaceDebugEnabled() {
        return placeDebugEnabled;
    }

    public static boolean isFoodDebugEnabled() {
        return foodDebugEnabled;
    }

    public static boolean isPotionDebugEnabled() {
        return potionDebugEnabled;
    }

    public static boolean isPotionDamageEnabled() {
        return potionDamageEnabled;
    }

    public static boolean isSwiftThrowEnabled() {
        return swiftThrowEnabled;
    }

    public static boolean isClientDebugEnabled() {
        return clientDebugEnabled;
    }

    public static boolean isFrostWalkerEnabled() {
        return frostWalkerEnabled;
    }

    public static boolean isInfinityCooldownEnabled() {
        return infinityCooldownEnabled;
    }

    // ========== Toggles ==========

    public static void toggleCakeDebug() {
        cakeDebugEnabled = !cakeDebugEnabled;
    }

    public static void togglePlaceDebug() {
        placeDebugEnabled = !placeDebugEnabled;
    }

    public static void toggleFoodDebug() {
        foodDebugEnabled = !foodDebugEnabled;
    }

    public static void togglePotionDebug() {
        potionDebugEnabled = !potionDebugEnabled;
    }

    public static void togglePotionDamage() {
        potionDamageEnabled = !potionDamageEnabled;
    }

    public static void toggleSwiftThrow() {
        swiftThrowEnabled = !swiftThrowEnabled;
    }

    public static void toggleClientDebug() {
        clientDebugEnabled = !clientDebugEnabled;
    }

    public static void toggleFrostWalker() {
        frostWalkerEnabled = !frostWalkerEnabled;
    }

    public static void toggleInfinityCooldown() {
        infinityCooldownEnabled = !infinityCooldownEnabled;
    }
}
