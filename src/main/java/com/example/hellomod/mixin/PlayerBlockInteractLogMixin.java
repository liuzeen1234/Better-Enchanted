package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.debug.DebugLogConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家方块交互行为日志 Mixin。
 * 跟踪：
 * - 破坏方块
 * - 右键方块交互（包括方块实体如箱子、熔炉、附魔台等）
 * - 使用物品（右键空气）
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class PlayerBlockInteractLogMixin {

    @Shadow
    @Final
    protected ServerPlayerEntity player;

    /**
     * 方块被破坏时记录
     */
    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;

        World world = player.getWorld();
        BlockState blockState = world.getBlockState(pos);
        String blockName = blockState.getBlock().getName().getString();
        ItemStack tool = player.getMainHandStack();
        String toolName = tool.isEmpty() ? "空手" : tool.getName().getString();

        // 检查是否有方块实体
        BlockEntity blockEntity = world.getBlockEntity(pos);
        String blockEntityInfo = blockEntity != null
                ? " [方块实体: " + blockEntity.getClass().getSimpleName() + "]"
                : "";

        HelloMod.LOGGER.info("[BehaviorLog] {} 破坏方块: {} [坐标: ({}, {}, {}), 工具: {}]{}",
                player.getName().getString(), blockName,
                pos.getX(), pos.getY(), pos.getZ(), toolName, blockEntityInfo);
    }

    /**
     * 右键方块交互时记录（包括打开箱子、熔炉、附魔台等方块实体菜单）
     */
    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState blockState = world.getBlockState(pos);
        String blockName = blockState.getBlock().getName().getString();
        String itemName = stack.isEmpty() ? "空手" : stack.getName().getString();

        // 检查是否有方块实体（箱子、熔炉、酿造台等）
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            String blockEntityType = blockEntity.getClass().getSimpleName();
            HelloMod.LOGGER.info("[BehaviorLog] {} 右键方块实体: {} ({}) [坐标: ({}, {}, {}), 手持: {}, 手: {}]",
                    player.getName().getString(), blockName, blockEntityType,
                    pos.getX(), pos.getY(), pos.getZ(), itemName, hand.name());
        } else {
            HelloMod.LOGGER.info("[BehaviorLog] {} 右键方块: {} [坐标: ({}, {}, {}), 手持: {}, 手: {}]",
                    player.getName().getString(), blockName,
                    pos.getX(), pos.getY(), pos.getZ(), itemName, hand.name());
        }
    }

    /**
     * 使用物品（右键空气，如吃食物、喝药水、投掷雪球等）
     */
    @Inject(method = "interactItem", at = @At("HEAD"))
    private void onInteractItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        if (stack.isEmpty()) return;

        HelloMod.LOGGER.info("[BehaviorLog] {} 使用物品: {} [手: {}, 坐标: ({}, {}, {})]",
                player.getName().getString(), stack.getName().getString(), hand.name(),
                String.format("%.1f", player.getX()),
                String.format("%.1f", player.getY()),
                String.format("%.1f", player.getZ()));
    }
}
