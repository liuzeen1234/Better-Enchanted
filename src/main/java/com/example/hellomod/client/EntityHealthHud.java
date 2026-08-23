package com.example.hellomod.client;

import com.example.hellomod.config.ModConfig;
import com.example.hellomod.network.EntityNbtCache;
import com.example.hellomod.network.EntityNbtRequestC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 实体血量 HUD：显示玩家准星所指实体的名称和血量。
 * 格式：[实体名称][当前血量/最大血量]
 * 当指向无血量实体时显示：[实体名称][-/-]
 * 显示位置：画面右上角
 * 可通过按键开关显示。
 */
public class EntityHealthHud {

    public static boolean isEnabled() {
        return ModConfig.isEntityHealthHudEnabled();
    }

    public static void toggle() {
        ModConfig.setEntityHealthHudEnabled(!ModConfig.isEntityHealthHudEnabled());
    }

    public static double getReachDistance() {
        return ModConfig.getEntityHealthHudReachDistance();
    }

    public static void setReachDistance(double distance) {
        ModConfig.setEntityHealthHudReachDistance(distance);
    }

    public static boolean isDetailedInfoEnabled() {
        return ModConfig.isEntityHealthHudDetailedInfo();
    }

    public static void toggleDetailedInfo() {
        ModConfig.setEntityHealthHudDetailedInfo(!ModConfig.isEntityHealthHudDetailedInfo());
    }

    public static void render(DrawContext drawContext, float tickDelta) {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Entity target = getTargetedEntity(client);
        if (target == null) return;

        String name = target.getName().getString();
        String text;

        if (target instanceof LivingEntity living) {
            float currentHealth = living.getHealth();
            float maxHealth = living.getMaxHealth();
            text = String.format("[%s][%.1f/%.1f]", name, currentHealth, maxHealth);
        } else {
            // 无血量实体显示 [-/-]
            text = String.format("[%s][-/-]", name);
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int textWidth = textRenderer.getWidth(text);

        // 右上角，留出 4px 边距
        int x = screenWidth - textWidth - 4;
        int y = 4;

        // 绘制带阴影的文字，颜色为红色
        drawContext.drawText(textRenderer, text, x, y, 0xFF5555, true);

        // 显示详细NBT信息（参考高级物品显示格式）
        if (isDetailedInfoEnabled()) {
            renderDetailedInfo(drawContext, textRenderer, target, screenWidth, y + 14);
        }
    }

    /**
     * 渲染实体的详细NBT信息，格式参考高级物品显示，靠右对齐。
     * 使用 0.5x 缩放以在有限空间内显示更多内容。
     * 数据来源：服务端通过网络包同步的完整 NBT（包含药水效果）。
     */
    private static void renderDetailedInfo(DrawContext drawContext, TextRenderer textRenderer, Entity target, int screenWidth, int startY) {
        int entityId = target.getId();

        // 从缓存获取服务端同步的 NBT 数据
        NbtCompound nbt = EntityNbtCache.get(entityId);

        // 如果缓存为空或过期，发送请求
        if (nbt == null) {
            if (EntityNbtCache.canRequest()) {
                EntityNbtRequestC2SPacket.send(entityId);
                EntityNbtCache.markRequested();
            }
            // 缓存未就绪时显示提示
            float scale = 0.5f;
            int scaledScreenWidth = (int) (screenWidth / scale);
            drawContext.getMatrices().push();
            drawContext.getMatrices().scale(scale, scale, 1.0f);
            String loading = "Loading...";
            int loadingWidth = textRenderer.getWidth(loading);
            drawContext.drawText(textRenderer, loading, scaledScreenWidth - loadingWidth - 8, (int)(startY / scale), 0xFFFF55, true);
            drawContext.getMatrices().pop();
            return;
        }

        float scale = 0.5f;
        // 缩放后坐标需要反向放大，因为缩放会使整个坐标系缩小
        int scaledScreenWidth = (int) (screenWidth / scale);
        int scaledStartY = (int) (startY / scale);
        int maxLineLength = 120; // 缩小后每行可以容纳更多字符

        drawContext.getMatrices().push();
        drawContext.getMatrices().scale(scale, scale, 1.0f);

        int yOffset = scaledStartY;

        // 优先显示 ActiveEffects（紧跟在血量行下方）
        if (nbt.contains("ActiveEffects")) {
            NbtElement effectElement = nbt.get("ActiveEffects");
            String effectText = "ActiveEffects: " + (effectElement != null ? effectElement.asString() : "[]");

            int startIndex = 0;
            while (startIndex < effectText.length()) {
                int endIndex = Math.min(startIndex + maxLineLength, effectText.length());
                String line = effectText.substring(startIndex, endIndex);

                int lineWidth = textRenderer.getWidth(line);
                int x = scaledScreenWidth - lineWidth - 8;
                drawContext.drawText(textRenderer, line, x, yOffset, 0x55FF55, true);

                yOffset += 12;
                startIndex = endIndex;
            }
        }

        // 显示其他 NBT 数据
        for (String key : nbt.getKeys()) {
            if (key.equals("ActiveEffects")) continue; // 已经显示过了

            NbtElement element = nbt.get(key);
            String nbtText = key + ": " + (element != null ? element.asString() : "null");

            int startIndex = 0;
            while (startIndex < nbtText.length()) {
                int endIndex = Math.min(startIndex + maxLineLength, nbtText.length());
                String line = nbtText.substring(startIndex, endIndex);

                int lineWidth = textRenderer.getWidth(line);
                int x = scaledScreenWidth - lineWidth - 8;
                drawContext.drawText(textRenderer, line, x, yOffset, 0xAAAAAA, true);

                yOffset += 12;
                startIndex = endIndex;
            }
        }

        drawContext.getMatrices().pop();
    }

    /**
     * 通过射线追踪获取玩家准星所指的实体（包括非 LivingEntity）。
     */
    private static Entity getTargetedEntity(MinecraftClient client) {
        if (client.cameraEntity == null) return null;

        double reach = getReachDistance();
        Vec3d cameraPos = client.cameraEntity.getCameraPosVec(1.0F);
        Vec3d lookVec = client.cameraEntity.getRotationVec(1.0F);
        Vec3d reachEnd = cameraPos.add(lookVec.multiply(reach));

        // 先检查方块碰撞距离，实体不应在方块后面被选中
        HitResult blockHit = client.cameraEntity.raycast(reach, 1.0F, false);
        double maxDist = reach;
        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            maxDist = blockHit.getPos().distanceTo(cameraPos);
            reachEnd = cameraPos.add(lookVec.multiply(maxDist));
        }

        Box searchBox = client.cameraEntity.getBoundingBox()
                .stretch(lookVec.multiply(maxDist))
                .expand(1.0, 1.0, 1.0);

        EntityHitResult entityHit = ProjectileUtil.raycast(
                client.cameraEntity,
                cameraPos,
                reachEnd,
                searchBox,
                entity -> !entity.isSpectator() && entity.canHit(),
                maxDist * maxDist
        );

        if (entityHit != null) {
            return entityHit.getEntity();
        }

        return null;
    }
}
