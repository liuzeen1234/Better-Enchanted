package com.debugmenu.api;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 调试开关条目：其他 Mod 注册调试开关时使用此格式。
 *
 * <p>每个条目描述一个可开关的调试功能：
 * <ul>
 *   <li>modId: 所属 Mod 的 ID</li>
 *   <li>key: 唯一标识（建议格式 "modId:feature_name"）</li>
 *   <li>displayName: 菜单中显示的名称</li>
 *   <li>getter: 获取当前开关状态</li>
 *   <li>setter: 设置开关状态</li>
 * </ul>
 */
public class DebugToggleEntry {

    private final String modId;
    private final String key;
    private final String displayName;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    /**
     * 创建调试开关条目。
     *
     * @param modId       所属 Mod 的 ID
     * @param key         唯一标识键
     * @param displayName 菜单中显示的名称
     * @param getter      获取当前开关状态
     * @param setter      设置开关状态
     */
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
