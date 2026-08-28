package com.debugmenu.mixin.client;

import com.debugmenu.api.DebugMenuApi;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端键盘输入行为日志 Mixin。
 * 跟踪玩家的按键操作。
 */
@Mixin(Keyboard.class)
public abstract class BehaviorLogKeyboardMixin {

    @Unique
    private static final Logger debug_menu$LOGGER = LoggerFactory.getLogger("DebugMenu");

    @Unique
    private static final String debug_menu$BEHAVIOR_LOG_KEY = "behavior_log";

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void debug_menu$onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;
        if (client.player == null) return;

        if (action != GLFW.GLFW_PRESS) return;
        if (client.currentScreen != null) return;

        String keyName = debug_menu$getKeyName(key);
        if (keyName != null) {
            debug_menu$LOGGER.info("[BehaviorLog] 按键: {}", keyName);
        }
    }

    @Unique
    private String debug_menu$getKeyName(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_W -> "W (前进)";
            case GLFW.GLFW_KEY_S -> "S (后退)";
            case GLFW.GLFW_KEY_A -> "A (左移)";
            case GLFW.GLFW_KEY_D -> "D (右移)";
            case GLFW.GLFW_KEY_SPACE -> "空格 (跳跃)";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "左Shift (潜行)";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "左Ctrl (疾跑)";
            case GLFW.GLFW_KEY_E -> "E (物品栏)";
            case GLFW.GLFW_KEY_Q -> "Q (丢弃)";
            case GLFW.GLFW_KEY_F -> "F (副手切换)";
            case GLFW.GLFW_KEY_R -> "R";
            case GLFW.GLFW_KEY_T -> "T (聊天)";
            case GLFW.GLFW_KEY_TAB -> "Tab (玩家列表)";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc (暂停)";
            case GLFW.GLFW_KEY_F1 -> "F1 (隐藏HUD)";
            case GLFW.GLFW_KEY_F2 -> "F2 (截图)";
            case GLFW.GLFW_KEY_F3 -> "F3 (调试)";
            case GLFW.GLFW_KEY_F5 -> "F5 (视角)";
            case GLFW.GLFW_KEY_1 -> "1 (快捷栏)";
            case GLFW.GLFW_KEY_2 -> "2 (快捷栏)";
            case GLFW.GLFW_KEY_3 -> "3 (快捷栏)";
            case GLFW.GLFW_KEY_4 -> "4 (快捷栏)";
            case GLFW.GLFW_KEY_5 -> "5 (快捷栏)";
            case GLFW.GLFW_KEY_6 -> "6 (快捷栏)";
            case GLFW.GLFW_KEY_7 -> "7 (快捷栏)";
            case GLFW.GLFW_KEY_8 -> "8 (快捷栏)";
            case GLFW.GLFW_KEY_9 -> "9 (快捷栏)";
            default -> null;
        };
    }
}
