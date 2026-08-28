package com.debugmenu.client;

import com.debugmenu.api.DebugMenuApi;
import com.debugmenu.api.DebugToggleEntry;
import com.debugmenu.config.DebugMenuConfig;
import com.debugmenu.network.EntityNbtResponseS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 调试菜单 Mod 客户端入口。
 * 注册按键绑定、HUD 渲染回调、网络包接收器。
 *
 * <p>同时注册调试 Mod 自带的"玩家行为日志"开关到 DebugMenuApi，
 * 使行为日志 Mixin 可以通过 API 查询开关状态。
 */
public class DebugMenuClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("DebugMenu");

    /** 打开调试菜单的按键，默认无绑定 */
    private static KeyBinding openDebugMenuKey;

    /** 行为日志开关状态（调试 Mod 自带功能） */
    private static boolean behaviorLogEnabled = false;

    @Override
    public void onInitializeClient() {
        // 加载配置
        DebugMenuConfig.load();

        // 从配置恢复行为日志状态
        behaviorLogEnabled = DebugMenuConfig.getToggleState("behavior_log", false);

        // 注册调试 Mod 自带的"玩家行为日志"开关
        DebugMenuApi.register(new DebugToggleEntry(
                "debug-menu", "behavior_log", "玩家行为日志",
                () -> behaviorLogEnabled,
                (enabled) -> {
                    behaviorLogEnabled = enabled;
                    DebugMenuConfig.setToggleState("behavior_log", enabled);
                }
        ));

        // 注册实体 NBT 响应包的客户端接收器
        EntityNbtResponseS2CPacket.registerClientReceiver();

        // 注册按键绑定: 打开调试功能菜单（默认无绑定）
        openDebugMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.debug-menu.open_debug_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.debug-menu.general"
        ));

        // 监听按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDebugMenuKey.wasPressed()) {
                client.setScreen(new DebugMenuScreen());
            }
        });

        // 注册 HUD 渲染回调
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            ItemCountHud.render(drawContext);
            EntityHealthHud.render(drawContext, tickDelta);
        });

        LOGGER.info("[DebugMenu] Client initialized.");
    }
}
