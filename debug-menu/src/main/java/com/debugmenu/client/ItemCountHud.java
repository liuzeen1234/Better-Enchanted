package com.debugmenu.client;

import com.debugmenu.config.DebugMenuConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/**
 * 手持物品数量 HUD：显示当前主手物品名称、数量、NBT 信息。
 * 显示位置：画面左上角。
 */
public class ItemCountHud {

    private static int lastCount = -1;

    public static void render(DrawContext drawContext) {
        if (!DebugMenuConfig.isItemHudEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;

        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.isEmpty()) {
            lastCount = -1;
            return;
        }

        String itemName = mainHand.getName().getString();
        int count = mainHand.getCount();

        if (count != lastCount) {
            lastCount = count;
        }

        String text = String.format("[Client] %s x%d", itemName, count);
        drawContext.drawText(textRenderer, text, 4, 4, 0x00FF00, true);

        // 高级物品显示：NBT 标签和耐久度
        if (DebugMenuConfig.isAdvancedItemHudEnabled()) {
            int yOffset = 16;

            // 显示耐久度（如有）
            if (mainHand.isDamageable()) {
                int currentDurability = mainHand.getMaxDamage() - mainHand.getDamage();
                int maxDurability = mainHand.getMaxDamage();
                String durabilityText = String.format("(%d/%d)", currentDurability, maxDurability);
                drawContext.drawText(textRenderer, durabilityText, 4, 4 + yOffset, 0xFFAA00, true);
                yOffset += 12;
            }

            // 显示所有 NBT 标签（0.5x 缩放）
            NbtCompound nbt = mainHand.getNbt();
            if (nbt != null) {
                float scale = 0.5f;
                drawContext.getMatrices().push();
                drawContext.getMatrices().scale(scale, scale, 1.0f);

                int scaledX = (int) (4 / scale);
                int scaledYOffset = (int) ((4 + yOffset) / scale);
                int maxLineLength = 120;

                for (String key : nbt.getKeys()) {
                    NbtElement element = nbt.get(key);
                    String nbtText = key + ": " + (element != null ? element.asString() : "null");
                    int startIndex = 0;
                    while (startIndex < nbtText.length()) {
                        int endIndex = Math.min(startIndex + maxLineLength, nbtText.length());
                        String line = nbtText.substring(startIndex, endIndex);
                        drawContext.drawText(textRenderer, line, scaledX, scaledYOffset, 0xAAAAAA, true);
                        scaledYOffset += 12;
                        startIndex = endIndex;
                    }
                }

                drawContext.getMatrices().pop();
            }
        }
    }
}
