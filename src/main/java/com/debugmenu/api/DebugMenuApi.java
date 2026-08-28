package com.debugmenu.api;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 调试菜单 API：其他 Mod 通过此类注册调试开关。
 *
 * <p>此文件为 API 存根（stub），供编译时引用。
 * 运行时由 debug-menu Mod 提供实际实现（同包同类名，会被 classloader 优先加载）。
 * 如果 debug-menu 未安装，此类仍可使用但不会有菜单 UI。
 */
public final class DebugMenuApi {

    private static final List<DebugToggleEntry> entries = new CopyOnWriteArrayList<>();

    private DebugMenuApi() {}

    public static void register(DebugToggleEntry entry) {
        Objects.requireNonNull(entry, "DebugToggleEntry cannot be null");
        entries.add(entry);
    }

    public static void registerAll(Collection<DebugToggleEntry> newEntries) {
        for (DebugToggleEntry entry : newEntries) {
            register(entry);
        }
    }

    public static List<DebugToggleEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static boolean hasEntries() {
        return !entries.isEmpty();
    }

    public static Map<String, List<DebugToggleEntry>> getEntriesByMod() {
        Map<String, List<DebugToggleEntry>> grouped = new LinkedHashMap<>();
        for (DebugToggleEntry entry : entries) {
            grouped.computeIfAbsent(entry.getModId(), k -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    public static DebugToggleEntry getEntry(String key) {
        for (DebugToggleEntry entry : entries) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        return null;
    }

    public static boolean isEnabled(String key) {
        DebugToggleEntry entry = getEntry(key);
        return entry != null && entry.isEnabled();
    }
}
