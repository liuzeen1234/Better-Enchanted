package com.example.hellomod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：注册 HUD debug 渲染，实时显示手持物品数量。
 * 同时注册实体血量 HUD 和其开关按键。
 */
public class HelloModClient implements ClientModInitializer {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BetterEnchanted");
    private int lastCount = -1;

    /** 手持物品显示是否开启 */
    private static boolean itemHudEnabled = true;

    /** 切换实体血量 HUD 的按键，默认无绑定 */
    private static KeyBinding toggleHealthHudKey;

    /** 切换手持物品显示的按键，默认无绑定 */
    private static KeyBinding toggleItemHudKey;

    @Override
    public void onInitializeClient() {
        // 注册无限附魔冷却客户端同步
        InfinityCooldownClientState.register();

        // 注册按键绑定: 切换实体血量显示（默认无绑定）
        toggleHealthHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hello-mod.toggle_health_hud", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // 默认无绑定，玩家可自行设置
                "category.hello-mod.hud" // 按键分类
        ));

        // 注册按键绑定: 切换手持物品显示（默认无绑定）
        toggleItemHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hello-mod.toggle_item_hud", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // 默认无绑定，玩家可自行设置
                "category.hello-mod.hud" // 按键分类
        ));

        // 监听按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHealthHudKey.wasPressed()) {
                EntityHealthHud.toggle();
                if (client.player != null) {
                    String status = EntityHealthHud.isEnabled() ? "§a开启" : "§c关闭";
                    client.player.sendMessage(
                            Text.literal("§7[实体血量HUD] " + status),
                            true // actionBar
                    );
                }
            }
            while (toggleItemHudKey.wasPressed()) {
                itemHudEnabled = !itemHudEnabled;
                if (client.player != null) {
                    String status = itemHudEnabled ? "§a开启" : "§c关闭";
                    client.player.sendMessage(
                            Text.literal("§7[手持物品显示] " + status),
                            true // actionBar
                    );
                }
            }
        });

        // 注册 HUD 渲染回调
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            // 原有的手持物品数量显示
            renderItemCountHud(drawContext);

            // 实体血量显示
            EntityHealthHud.render(drawContext, tickDelta);
        });
    }

    private void renderItemCountHud(DrawContext drawContext) {
        if (!itemHudEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;

        // 获取当前主手物品
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.isEmpty()) {
            lastCount = -1;
            return;
        }

        String itemName = mainHand.getName().getString();
        int count = mainHand.getCount();

        // 数量变化时打印日志，方便对照服务端
        if (count != lastCount) {
            LOGGER.info("[ClientDebug] MainHand: {} x{}", itemName, count);
            lastCount = count;
        }

        String text = String.format("[Client] %s x%d", itemName, count);
        drawContext.drawText(textRenderer, text, 4, 4, 0x00FF00, true);
    }
}
