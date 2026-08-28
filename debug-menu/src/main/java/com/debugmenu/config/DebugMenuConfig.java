package com.debugmenu.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 调试菜单 Mod 自身的配置持久化。
 *
 * <p>存储内容：
 * <ul>
 *   <li>所有调试开关的 on/off 状态（按 key 存储）</li>
 *   <li>HUD 相关设置（实体血量显示距离、开关等）</li>
 * </ul>
 *
 * <p>配置文件位于 config/debug-menu.json
 */
public class DebugMenuConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("DebugMenu");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("debug-menu.json");

    private static ConfigData data = new ConfigData();

    public static class ConfigData {
        /** 各开关状态：key -> enabled */
        public Map<String, Boolean> toggleStates = new HashMap<>();

        // === 实体血量 HUD ===
        public boolean entityHealthHudEnabled = false;
        public double entityHealthHudReachDistance = 128.0;
        public boolean entityHealthHudDetailedInfo = false;

        // === 手持物品 HUD ===
        public boolean itemHudEnabled = false;
        public boolean advancedItemHudEnabled = false;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ConfigData loaded = GSON.fromJson(json, ConfigData.class);
                if (loaded != null) {
                    data = loaded;
                    LOGGER.info("[DebugMenu] Loaded config from {}", CONFIG_PATH);
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                LOGGER.warn("[DebugMenu] Failed to load config, using defaults: {}", e.getMessage());
            }
        } else {
            save();
            LOGGER.info("[DebugMenu] Created default config at {}", CONFIG_PATH);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("[DebugMenu] Failed to save config: {}", e.getMessage());
        }
    }

    // ==================== 开关状态存取 ====================

    /**
     * 获取某个开关的持久化状态。
     * 如果从未设置过，返回 defaultValue。
     */
    public static boolean getToggleState(String key, boolean defaultValue) {
        return data.toggleStates.getOrDefault(key, defaultValue);
    }

    /**
     * 设置某个开关的持久化状态。
     */
    public static void setToggleState(String key, boolean enabled) {
        data.toggleStates.put(key, enabled);
        save();
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

    public static boolean isEntityHealthHudDetailedInfo() {
        return data.entityHealthHudDetailedInfo;
    }

    public static void setEntityHealthHudDetailedInfo(boolean enabled) {
        data.entityHealthHudDetailedInfo = enabled;
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
}
