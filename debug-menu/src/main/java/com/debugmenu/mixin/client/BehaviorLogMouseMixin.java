package com.debugmenu.mixin.client;

import com.debugmenu.api.DebugMenuApi;
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
 * 跟踪玩家的鼠标点击和滚轮操作。
 */
@Mixin(Mouse.class)
public abstract class BehaviorLogMouseMixin {

    @Unique
    private static final Logger debug_menu$LOGGER = LoggerFactory.getLogger("DebugMenu");

    @Unique
    private static final String debug_menu$BEHAVIOR_LOG_KEY = "behavior_log";

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void debug_menu$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;
        if (client.player == null) return;

        if (action != GLFW.GLFW_PRESS) return;

        String buttonName = switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "左键";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "右键";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "中键";
            default -> "鼠标按键" + button;
        };

        Screen currentScreen = client.currentScreen;
        if (currentScreen != null) {
            String screenName = currentScreen.getClass().getSimpleName();
            String title = currentScreen.getTitle().getString();
            debug_menu$LOGGER.info("[BehaviorLog] 鼠标 {} 点击 [界面: {} ({})]",
                    buttonName, title.isEmpty() ? screenName : title, screenName);
            return;
        }

        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null) {
            debug_menu$LOGGER.info("[BehaviorLog] 鼠标 {} [目标: 无]", buttonName);
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        String heldItem = mainHand.isEmpty() ? "空手" : mainHand.getName().getString();

        switch (hitResult.getType()) {
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                Entity target = entityHit.getEntity();
                debug_menu$LOGGER.info("[BehaviorLog] 鼠标 {} [目标实体: {} ({}), 手持: {}]",
                        buttonName, target.getName().getString(),
                        target.getType().getUntranslatedName(), heldItem);
            }
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos pos = blockHit.getBlockPos();
                String blockName = client.world != null
                        ? client.world.getBlockState(pos).getBlock().getName().getString()
                        : "未知";
                debug_menu$LOGGER.info("[BehaviorLog] 鼠标 {} [目标方块: {} ({}, {}, {}), 手持: {}]",
                        buttonName, blockName, pos.getX(), pos.getY(), pos.getZ(), heldItem);
            }
            case MISS -> {
                debug_menu$LOGGER.info("[BehaviorLog] 鼠标 {} [目标: 空气, 手持: {}]", buttonName, heldItem);
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void debug_menu$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;
        if (client.player == null) return;

        if (client.currentScreen != null) return;

        if (vertical != 0) {
            String direction = vertical > 0 ? "上" : "下";
            debug_menu$LOGGER.info("[BehaviorLog] 鼠标滚轮: {} ({})", direction, String.format("%.1f", vertical));
        }
    }
}
