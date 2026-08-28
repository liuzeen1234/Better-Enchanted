package com.example.hellomod.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 调试日志开关配置。
 * 控制各种调试日志的输出开关，运行时生效，默认全部关闭。
 *
 * <p>当 debug-menu Mod 安装时，这些开关会通过 DebugToggleRegistration
 * 注册到调试菜单 API 中，允许通过菜单 UI 控制。
 *
 * <p>当 debug-menu Mod 未安装时，开关始终为 false（不输出调试日志）。
 */
public class DebugLogConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("BetterEnchanted");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("better-enchanted-debug.json");

    private static DebugLogData data = new DebugLogData();

    public static class DebugLogData {
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

    /** 加载配置 */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                DebugLogData loaded = GSON.fromJson(json, DebugLogData.class);
                if (loaded != null) {
                    data = loaded;
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                LOGGER.warn("[DebugLogConfig] Failed to load config: {}", e.getMessage());
            }
        }
    }

    /** 保存配置 */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("[DebugLogConfig] Failed to save config: {}", e.getMessage());
        }
    }

    // ========== Getters ==========

    public static boolean isCakeDebugEnabled() {
        return data.cakeDebugEnabled;
    }

    public static boolean isPlaceDebugEnabled() {
        return data.placeDebugEnabled;
    }

    public static boolean isFoodDebugEnabled() {
        return data.foodDebugEnabled;
    }

    public static boolean isPotionDebugEnabled() {
        return data.potionDebugEnabled;
    }

    public static boolean isPotionDamageEnabled() {
        return data.potionDamageEnabled;
    }

    public static boolean isSwiftThrowEnabled() {
        return data.swiftThrowEnabled;
    }

    public static boolean isClientDebugEnabled() {
        return data.clientDebugEnabled;
    }

    public static boolean isFrostWalkerEnabled() {
        return data.frostWalkerEnabled;
    }

    public static boolean isInfinityCooldownEnabled() {
        return data.infinityCooldownEnabled;
    }

    // ========== Setters (with save) ==========

    public static void setCakeDebugEnabled(boolean enabled) {
        data.cakeDebugEnabled = enabled;
        save();
    }

    public static void setPlaceDebugEnabled(boolean enabled) {
        data.placeDebugEnabled = enabled;
        save();
    }

    public static void setFoodDebugEnabled(boolean enabled) {
        data.foodDebugEnabled = enabled;
        save();
    }

    public static void setPotionDebugEnabled(boolean enabled) {
        data.potionDebugEnabled = enabled;
        save();
    }

    public static void setPotionDamageEnabled(boolean enabled) {
        data.potionDamageEnabled = enabled;
        save();
    }

    public static void setSwiftThrowEnabled(boolean enabled) {
        data.swiftThrowEnabled = enabled;
        save();
    }

    public static void setClientDebugEnabled(boolean enabled) {
        data.clientDebugEnabled = enabled;
        save();
    }

    public static void setFrostWalkerEnabled(boolean enabled) {
        data.frostWalkerEnabled = enabled;
        save();
    }

    public static void setInfinityCooldownEnabled(boolean enabled) {
        data.infinityCooldownEnabled = enabled;
        save();
    }
}
