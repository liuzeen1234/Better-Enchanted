package com.example.hellomod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod 配置持久化管理。
 * 配置文件保存在 config/hello-mod.json，每次设置变更后自动保存，启动时自动加载。
 */
public class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("BetterEnchanted");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("hello-mod.json");

    private static ModConfigData data = new ModConfigData();

    /**
     * 配置数据结构
     */
    public static class ModConfigData {
        // === 实体血量 HUD ===
        public boolean entityHealthHudEnabled = false;
        public double entityHealthHudReachDistance = 128.0;

        // === 手持物品 HUD ===
        public boolean itemHudEnabled = false;
        public boolean advancedItemHudEnabled = false;

        // === Debug 日志开关 ===
        public boolean cakeDebugEnabled = false;
        public boolean placeDebugEnabled = false;
        public boolean foodDebugEnabled = false;
        public boolean potionDebugEnabled = false;
        public boolean potionDamageEnabled = false;
        public boolean swiftThrowEnabled = false;
        public boolean clientDebugEnabled = false;
        public boolean frostWalkerEnabled = false;
        public boolean infinityCooldownEnabled = false;
    }

    /** 加载配置，如果文件不存在则使用默认值并创建文件 */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfigData loaded = GSON.fromJson(json, ModConfigData.class);
                if (loaded != null) {
                    data = loaded;
                    LOGGER.info("[Config] Loaded config from {}", CONFIG_PATH);
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                LOGGER.warn("[Config] Failed to load config, using defaults: {}", e.getMessage());
            }
        } else {
            // 首次运行，保存默认配置
            save();
            LOGGER.info("[Config] Created default config at {}", CONFIG_PATH);
        }
    }

    /** 保存当前配置到文件 */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("[Config] Failed to save config: {}", e.getMessage());
        }
    }

    /** 获取配置数据（只读访问用） */
    public static ModConfigData getData() {
        return data;
    }

    // ==================== 实体血量 HUD ====================

    public static boolean isEntityHealthHudEnabled() {
        return data.entityHealthHudEnabled;
    }

    public static void setEntityHealthHudEnabled(boolean enabled) {
        data.entityHealthHudEnabled = enabled;
        save();
    }

    public static double getEntityHealthHudReachDistance() {
        return data.entityHealthHudReachDistance;
    }

    public static void setEntityHealthHudReachDistance(double distance) {
        data.entityHealthHudReachDistance = distance;
        save();
    }

    // ==================== 手持物品 HUD ====================

    public static boolean isItemHudEnabled() {
        return data.itemHudEnabled;
    }

    public static void setItemHudEnabled(boolean enabled) {
        data.itemHudEnabled = enabled;
        save();
    }

    public static boolean isAdvancedItemHudEnabled() {
        return data.advancedItemHudEnabled;
    }

    public static void setAdvancedItemHudEnabled(boolean enabled) {
        data.advancedItemHudEnabled = enabled;
        save();
    }

    // ==================== Debug 日志开关 ====================

    public static boolean isCakeDebugEnabled() {
        return data.cakeDebugEnabled;
    }

    public static void setCakeDebugEnabled(boolean enabled) {
        data.cakeDebugEnabled = enabled;
        save();
    }

    public static boolean isPlaceDebugEnabled() {
        return data.placeDebugEnabled;
    }

    public static void setPlaceDebugEnabled(boolean enabled) {
        data.placeDebugEnabled = enabled;
        save();
    }

    public static boolean isFoodDebugEnabled() {
        return data.foodDebugEnabled;
    }

    public static void setFoodDebugEnabled(boolean enabled) {
        data.foodDebugEnabled = enabled;
        save();
    }

    public static boolean isPotionDebugEnabled() {
        return data.potionDebugEnabled;
    }

    public static void setPotionDebugEnabled(boolean enabled) {
        data.potionDebugEnabled = enabled;
        save();
    }

    public static boolean isPotionDamageEnabled() {
        return data.potionDamageEnabled;
    }

    public static void setPotionDamageEnabled(boolean enabled) {
        data.potionDamageEnabled = enabled;
        save();
    }

    public static boolean isSwiftThrowEnabled() {
        return data.swiftThrowEnabled;
    }

    public static void setSwiftThrowEnabled(boolean enabled) {
        data.swiftThrowEnabled = enabled;
        save();
    }

    public static boolean isClientDebugEnabled() {
        return data.clientDebugEnabled;
    }

    public static void setClientDebugEnabled(boolean enabled) {
        data.clientDebugEnabled = enabled;
        save();
    }

    public static boolean isFrostWalkerEnabled() {
        return data.frostWalkerEnabled;
    }

    public static void setFrostWalkerEnabled(boolean enabled) {
        data.frostWalkerEnabled = enabled;
        save();
    }

    public static boolean isInfinityCooldownEnabled() {
        return data.infinityCooldownEnabled;
    }

    public static void setInfinityCooldownEnabled(boolean enabled) {
        data.infinityCooldownEnabled = enabled;
        save();
    }
}
