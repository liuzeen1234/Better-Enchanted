package com.debugmenu.api;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 调试菜单 API：其他 Mod 通过此类注册调试开关。
 *
 * <p>使用方法（在其他 Mod 的初始化阶段调用）：
 * <pre>{@code
 * DebugMenuApi.register(new DebugToggleEntry(
 *     "my-mod", "my-mod:feature_debug", "功能调试",
 *     () -> myDebugEnabled, (v) -> { myDebugEnabled = v; saveConfig(); }
 * ));
 * }</pre>
 *
 * <p>调试菜单 Mod 会自动读取所有注册的条目并生成 UI。
 * 如果没有任何注册，菜单将显示"无可控制的开关"。
 */
public final class DebugMenuApi {

    private static final List<DebugToggleEntry> entries = new CopyOnWriteArrayList<>();

    private DebugMenuApi() {}

    /**
     * 注册一个调试开关。
     *
     * @param entry 调试开关条目
     */
    public static void register(DebugToggleEntry entry) {
        Objects.requireNonNull(entry, "DebugToggleEntry cannot be null");
        entries.add(entry);
    }

    /**
     * 批量注册调试开关。
     *
     * @param newEntries 调试开关条目集合
     */
    public static void registerAll(Collection<DebugToggleEntry> newEntries) {
        for (DebugToggleEntry entry : newEntries) {
            register(entry);
        }
    }

    /**
     * 获取所有已注册的调试开关（只读视图）。
     */
    public static List<DebugToggleEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * 是否有已注册的调试开关。
     */
    public static boolean hasEntries() {
        return !entries.isEmpty();
    }

    /**
     * 按 modId 分组获取所有条目。
     */
    public static Map<String, List<DebugToggleEntry>> getEntriesByMod() {
        Map<String, List<DebugToggleEntry>> grouped = new LinkedHashMap<>();
        for (DebugToggleEntry entry : entries) {
            grouped.computeIfAbsent(entry.getModId(), k -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    /**
     * 根据 key 查找条目。
     *
     * @param key 开关的唯一标识
     * @return 对应条目，未找到返回 null
     */
    public static DebugToggleEntry getEntry(String key) {
        for (DebugToggleEntry entry : entries) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 快速查询某个开关是否启用（便捷方法）。
     * 如果 key 不存在，返回 false。
     *
     * @param key 开关的唯一标识
     * @return 开关是否启用
     */
    public static boolean isEnabled(String key) {
        DebugToggleEntry entry = getEntry(key);
        return entry != null && entry.isEnabled();
    }
}
