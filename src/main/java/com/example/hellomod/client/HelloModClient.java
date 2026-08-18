package com.example.hellomod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * 客户端入口：注册 HUD debug 渲染，实时显示手持物品数量。
 */
public class HelloModClient implements ClientModInitializer {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("hello-mod");
    private int lastCount = -1;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
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
        });
    }
}
