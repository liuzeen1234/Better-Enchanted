package com.debugmenu.mixin;

import com.debugmenu.api.DebugMenuApi;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家方块交互行为日志 Mixin。
 * 跟踪：破坏方块、右键方块交互、使用物品。
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class PlayerBlockInteractLogMixin {

    @Unique
    private static final Logger debug_menu$LOGGER = LoggerFactory.getLogger("DebugMenu");

    @Unique
    private static final String debug_menu$BEHAVIOR_LOG_KEY = "behavior_log";

    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void debug_menu$onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;

        World world = player.getWorld();
        BlockState blockState = world.getBlockState(pos);
        String blockName = blockState.getBlock().getName().getString();
        ItemStack tool = player.getMainHandStack();
        String toolName = tool.isEmpty() ? "空手" : tool.getName().getString();

        BlockEntity blockEntity = world.getBlockEntity(pos);
        String blockEntityInfo = blockEntity != null
                ? " [方块实体: " + blockEntity.getClass().getSimpleName() + "]"
                : "";

        debug_menu$LOGGER.info("[BehaviorLog] {} 破坏方块: {} [坐标: ({}, {}, {}), 工具: {}]{}",
                player.getName().getString(), blockName,
                pos.getX(), pos.getY(), pos.getZ(), toolName, blockEntityInfo);
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void debug_menu$onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState blockState = world.getBlockState(pos);
        String blockName = blockState.getBlock().getName().getString();
        String itemName = stack.isEmpty() ? "空手" : stack.getName().getString();

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            String blockEntityType = blockEntity.getClass().getSimpleName();
            debug_menu$LOGGER.info("[BehaviorLog] {} 右键方块实体: {} ({}) [坐标: ({}, {}, {}), 手持: {}, 手: {}]",
                    player.getName().getString(), blockName, blockEntityType,
                    pos.getX(), pos.getY(), pos.getZ(), itemName, hand.name());
        } else {
            debug_menu$LOGGER.info("[BehaviorLog] {} 右键方块: {} [坐标: ({}, {}, {}), 手持: {}, 手: {}]",
                    player.getName().getString(), blockName,
                    pos.getX(), pos.getY(), pos.getZ(), itemName, hand.name());
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void debug_menu$onInteractItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;
        if (stack.isEmpty()) return;

        debug_menu$LOGGER.info("[BehaviorLog] {} 使用物品: {} [手: {}, 坐标: ({}, {}, {})]",
                player.getName().getString(), stack.getName().getString(), hand.name(),
                String.format("%.1f", player.getX()),
                String.format("%.1f", player.getY()),
                String.format("%.1f", player.getZ()));
    }
}
