package com.example.hellomod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * 实体血量 HUD：显示玩家准星所指实体的名称和血量。
 * 格式：[实体名称][当前血量/最大血量]
 * 显示位置：画面右上角
 * 可通过按键开关显示。
 */
public class EntityHealthHud {

    private static boolean enabled = true;
    private static double reachDistance = 128.0;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static double getReachDistance() {
        return reachDistance;
    }

    public static void setReachDistance(double distance) {
        reachDistance = distance;
    }

    public static void render(DrawContext drawContext, float tickDelta) {
        if (!enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        LivingEntity target = getTargetedEntity(client);
        if (target == null) return;

        String name = target.getName().getString();
        float currentHealth = target.getHealth();
        float maxHealth = target.getMaxHealth();

        // 格式: [实体名称][当前血量/最大血量]
        String text = String.format("[%s][%.1f/%.1f]", name, currentHealth, maxHealth);

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int textWidth = textRenderer.getWidth(text);

        // 右上角，留出 4px 边距
        int x = screenWidth - textWidth - 4;
        int y = 4;

        // 绘制带阴影的文字，颜色为红色
        drawContext.drawText(textRenderer, text, x, y, 0xFF5555, true);
    }

    /**
     * 通过射线追踪获取玩家准星所指的 LivingEntity。
     */
    private static LivingEntity getTargetedEntity(MinecraftClient client) {
        if (client.cameraEntity == null) return null;

        Vec3d cameraPos = client.cameraEntity.getCameraPosVec(1.0F);
        Vec3d lookVec = client.cameraEntity.getRotationVec(1.0F);
        Vec3d reachEnd = cameraPos.add(lookVec.multiply(reachDistance));

        // 先检查方块碰撞距离，实体不应在方块后面被选中
        HitResult blockHit = client.cameraEntity.raycast(reachDistance, 1.0F, false);
        double maxDist = reachDistance;
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

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return null;
    }
}
