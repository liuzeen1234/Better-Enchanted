package com.example.hellomod.client;

import com.example.hellomod.entity.ModEntities;
import com.example.hellomod.network.EntityNbtResponseS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.example.hellomod.config.ModConfig;
import com.example.hellomod.debug.DebugLogConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：注册 HUD debug 渲染，实时显示手持物品数量。
 * 同时注册实体血量 HUD 和其开关按键。
 */
public class HelloModClient implements ClientModInitializer {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BetterEnchanted");
    private int lastCount = -1;

    /** 打开调试菜单的按键，默认无绑定 */
    private static KeyBinding openDebugMenuKey;

    public static boolean isItemHudEnabled() {
        return ModConfig.isItemHudEnabled();
    }

    public static void toggleItemHud() {
        ModConfig.setItemHudEnabled(!ModConfig.isItemHudEnabled());
    }

    public static boolean isAdvancedItemHudEnabled() {
        return ModConfig.isAdvancedItemHudEnabled();
    }

    public static void toggleAdvancedItemHud() {
        ModConfig.setAdvancedItemHudEnabled(!ModConfig.isAdvancedItemHudEnabled());
    }

    @Override
    public void onInitializeClient() {
        // 加载持久化配置
        ModConfig.load();

        // 注册实体 NBT 响应包的客户端接收器
        EntityNbtResponseS2CPacket.registerClientReceiver();

        // 注册无限附魔冷却客户端同步
        InfinityCooldownClientState.register();

        // 注册超级附魔金苹果投掷实体的渲染器（空渲染器，不显示投掷物贴图）
        // 避免因投掷物速度过快导致的贴图显示问题 (MC-128812)
        EntityRendererRegistry.register(ModEntities.SUPER_GOLDEN_APPLE_ENTITY, EmptyEntityRenderer::new);

        // 注册按键绑定: 打开调试功能菜单（默认无绑定）
        openDebugMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hello-mod.open_debug_menu", // 翻译键
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // 默认无绑定，玩家可自行设置
                "category.hello-mod.hud" // 按键分类
        ));

        // 监听按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDebugMenuKey.wasPressed()) {
                client.setScreen(new DebugMenuScreen());
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
        if (!isItemHudEnabled()) return;

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
            if (DebugLogConfig.isClientDebugEnabled()) LOGGER.info("[ClientDebug] MainHand: {} x{}", itemName, count);
            lastCount = count;
        }

        String text = String.format("[Client] %s x%d", itemName, count);
        drawContext.drawText(textRenderer, text, 4, 4, 0x00FF00, true);

        // 高级物品显示：NBT标签和耐久度
        if (isAdvancedItemHudEnabled()) {
            int yOffset = 16; // 从第一行下方开始

            // 显示耐久度（如有）
            if (mainHand.isDamageable()) {
                int currentDurability = mainHand.getMaxDamage() - mainHand.getDamage();
                int maxDurability = mainHand.getMaxDamage();
                String durabilityText = String.format("(%d/%d)", currentDurability, maxDurability);
                drawContext.drawText(textRenderer, durabilityText, 4, 4 + yOffset, 0xFFAA00, true);
                yOffset += 12;
            }

            // 显示所有NBT标签（0.5x 缩放，与实体详细信息一致）
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
