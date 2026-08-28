package com.debugmenu.api;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 调试开关条目：其他 Mod 注册调试开关时使用此格式。
 *
 * <p>此文件为 API 存根（stub），供编译时引用。
 * 运行时由 debug-menu Mod 提供实际实现。
 */
public class DebugToggleEntry {

    private final String modId;
    private final String key;
    private final String displayName;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public DebugToggleEntry(String modId, String key, String displayName,
                            Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.modId = modId;
        this.key = key;
        this.displayName = displayName;
        this.getter = getter;
        this.setter = setter;
    }

    public String getModId() {
        return modId;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return getter.get();
    }

    public void setEnabled(boolean enabled) {
        setter.accept(enabled);
    }

    public void toggle() {
        setter.accept(!getter.get());
    }
}
