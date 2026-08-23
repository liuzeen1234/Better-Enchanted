package com.example.hellomod.mixin.client;

import com.example.hellomod.debug.DebugLogConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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
 * 客户端鼠标输入行为日志 Mixin。
 * 跟踪玩家的鼠标点击操作（左键/右键/中键），
 * 以及鼠标滚轮滚动。
 */
@Mixin(Mouse.class)
public abstract class BehaviorLogMouseMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterEnchanted");

    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * 鼠标按键事件记录
     */
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        if (client.player == null) return;

        // 只记录按下事件
        if (action != GLFW.GLFW_PRESS) return;

        String buttonName = switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "左键";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "右键";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "中键";
            default -> "鼠标按键" + button;
        };

        // 如果在界面中，记录界面内点击
        Screen currentScreen = client.currentScreen;
        if (currentScreen != null) {
            String screenName = currentScreen.getClass().getSimpleName();
            String title = currentScreen.getTitle().getString();
            LOGGER.info("[BehaviorLog] 鼠标 {} 点击 [界面: {} ({})]",
                    buttonName, title.isEmpty() ? screenName : title, screenName);
            return;
        }

        // 游戏内点击：分析目标
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null) {
            LOGGER.info("[BehaviorLog] 鼠标 {} [目标: 无]", buttonName);
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        String heldItem = mainHand.isEmpty() ? "空手" : mainHand.getName().getString();

        switch (hitResult.getType()) {
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                Entity target = entityHit.getEntity();
                LOGGER.info("[BehaviorLog] 鼠标 {} [目标实体: {} ({}), 手持: {}]",
                        buttonName, target.getName().getString(),
                        target.getType().getUntranslatedName(), heldItem);
            }
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos pos = blockHit.getBlockPos();
                String blockName = client.world != null
                        ? client.world.getBlockState(pos).getBlock().getName().getString()
                        : "未知";
                LOGGER.info("[BehaviorLog] 鼠标 {} [目标方块: {} ({}, {}, {}), 手持: {}]",
                        buttonName, blockName, pos.getX(), pos.getY(), pos.getZ(), heldItem);
            }
            case MISS -> {
                LOGGER.info("[BehaviorLog] 鼠标 {} [目标: 空气, 手持: {}]", buttonName, heldItem);
            }
        }
    }

    /**
     * 鼠标滚轮事件记录（物品切换等）
     */
    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        if (client.player == null) return;

        // 在界面中的滚轮不记录（界面自身处理）
        if (client.currentScreen != null) return;

        if (vertical != 0) {
            String direction = vertical > 0 ? "上" : "下";
            LOGGER.info("[BehaviorLog] 鼠标滚轮: {} ({})", direction, String.format("%.1f", vertical));
        }
    }
}
