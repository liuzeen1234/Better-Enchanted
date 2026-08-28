package com.debugmenu.client;

import com.debugmenu.config.DebugMenuConfig;
import com.debugmenu.network.EntityNbtCache;
import com.debugmenu.network.EntityNbtRequestC2SPacket;
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
 */
public class EntityHealthHud {

    public static boolean isEnabled() {
        return DebugMenuConfig.isEntityHealthHudEnabled();
    }

    public static void toggle() {
        DebugMenuConfig.setEntityHealthHudEnabled(!DebugMenuConfig.isEntityHealthHudEnabled());
    }

    public static double getReachDistance() {
        return DebugMenuConfig.getEntityHealthHudReachDistance();
    }

    public static void setReachDistance(double distance) {
        DebugMenuConfig.setEntityHealthHudReachDistance(distance);
    }

    public static boolean isDetailedInfoEnabled() {
        return DebugMenuConfig.isEntityHealthHudDetailedInfo();
    }

    public static void toggleDetailedInfo() {
        DebugMenuConfig.setEntityHealthHudDetailedInfo(!DebugMenuConfig.isEntityHealthHudDetailedInfo());
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
            text = String.format("[%s][-/-]", name);
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int textWidth = textRenderer.getWidth(text);

        int x = screenWidth - textWidth - 4;
        int y = 4;

        drawContext.drawText(textRenderer, text, x, y, 0xFF5555, true);

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

        NbtCompound nbt = EntityNbtCache.get(entityId);

        if (nbt == null) {
            if (EntityNbtCache.canRequest()) {
                EntityNbtRequestC2SPacket.send(entityId);
                EntityNbtCache.markRequested();
            }
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
        int scaledScreenWidth = (int) (screenWidth / scale);
        int scaledStartY = (int) (startY / scale);
        int maxLineLength = 120;

        drawContext.getMatrices().push();
        drawContext.getMatrices().scale(scale, scale, 1.0f);

        int yOffset = scaledStartY;

        // 优先显示 ActiveEffects
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
            if (key.equals("ActiveEffects")) continue;

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
     * 通过射线追踪获取玩家准星所指的实体。
     */
    private static Entity getTargetedEntity(MinecraftClient client) {
        if (client.cameraEntity == null) return null;

        double reach = getReachDistance();
        Vec3d cameraPos = client.cameraEntity.getCameraPosVec(1.0F);
        Vec3d lookVec = client.cameraEntity.getRotationVec(1.0F);
        Vec3d reachEnd = cameraPos.add(lookVec.multiply(reach));

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
