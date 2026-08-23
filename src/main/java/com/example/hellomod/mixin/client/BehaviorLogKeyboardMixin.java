package com.example.hellomod.mixin.client;

import com.example.hellomod.debug.DebugLogConfig;
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
 * 跟踪玩家的按键操作（按下/释放）。
 * 仅记录游戏内按键（非界面打字），避免刷屏。
 */
@Mixin(Keyboard.class)
public abstract class BehaviorLogKeyboardMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterEnchanted");

    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * 按键事件记录
     */
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        if (client.player == null) return;

        // 只记录按下事件（action=1），不记录释放和重复
        if (action != GLFW.GLFW_PRESS) return;

        // 如果在界面中输入文字，不记录（由界面日志覆盖）
        if (client.currentScreen != null) return;

        String keyName = hello_mod$getKeyName(key);
        if (keyName != null) {
            LOGGER.info("[BehaviorLog] 按键: {}", keyName);
        }
    }

    @Unique
    private String hello_mod$getKeyName(int key) {
        // 只记录有意义的游戏按键，忽略修饰键等
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
            default -> null; // 其他按键不记录
        };
    }
}
