package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.debug.DebugLogConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家行为实时日志 Mixin。
 * 跟踪玩家的所有重要行为，包括：
 * - 攻击实体
 * - 与实体交互（右键实体）
 * - 受到伤害
 * - 死亡
 * - 丢弃物品
 * - 跳跃
 * - 切换手持物品
 * - 疾跑/潜行状态变化
 * - 移动（每秒或每2格记录一次）
 * - 捡起物品
 *
 * 通过调试菜单中的 [BehaviorLog] 开关控制，默认关闭。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerBehaviorLogMixin {

    @Unique
    private int hello_mod$lastSelectedSlot = -1;

    @Unique
    private boolean hello_mod$wasSprinting = false;

    @Unique
    private boolean hello_mod$wasSneaking = false;

    @Unique
    private boolean hello_mod$wasSwimming = false;

    @Unique
    private boolean hello_mod$wasFlying = false;

    @Unique
    private Vec3d hello_mod$lastLoggedPos = null;

    @Unique
    private int hello_mod$posLogCooldown = 0;

    @Unique
    private int hello_mod$lastFoodLevel = -1;

    /**
     * 攻击实体时记录
     */
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        String targetName = target.getName().getString();
        String targetType = target.getType().getUntranslatedName();
        ItemStack weapon = player.getMainHandStack();
        String weaponName = weapon.isEmpty() ? "空手" : weapon.getName().getString();

        if (target instanceof LivingEntity living) {
            HelloMod.LOGGER.info("[BehaviorLog] {} 攻击了 {} ({}) [武器: {}, 目标血量: {}/{}]",
                    player.getName().getString(), targetName, targetType, weaponName,
                    String.format("%.1f", living.getHealth()), String.format("%.1f", living.getMaxHealth()));
        } else {
            HelloMod.LOGGER.info("[BehaviorLog] {} 攻击了 {} ({}) [武器: {}]",
                    player.getName().getString(), targetName, targetType, weaponName);
        }
    }

    /**
     * 与实体交互（右键实体）时记录
     */
    @Inject(method = "interact", at = @At("HEAD"))
    private void onInteractEntity(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        ItemStack heldItem = player.getStackInHand(hand);
        String itemName = heldItem.isEmpty() ? "空手" : heldItem.getName().getString();
        String entityName = entity.getName().getString();
        String entityType = entity.getType().getUntranslatedName();

        HelloMod.LOGGER.info("[BehaviorLog] {} 右键交互实体: {} ({}) [手持: {}, 手: {}]",
                player.getName().getString(), entityName, entityType, itemName, hand.name());
    }

    /**
     * 受到伤害时记录
     */
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        String sourceName = source.getName();
        Entity attacker = source.getAttacker();
        String attackerName = attacker != null ? attacker.getName().getString() : "无";

        HelloMod.LOGGER.info("[BehaviorLog] {} 受到伤害 [来源: {}, 攻击者: {}, 伤害量: {}, 当前血量: {}/{}]",
                player.getName().getString(), sourceName, attackerName,
                String.format("%.1f", amount),
                String.format("%.1f", player.getHealth()),
                String.format("%.1f", player.getMaxHealth()));
    }

    /**
     * 死亡时记录
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        Entity attacker = damageSource.getAttacker();
        String attackerName = attacker != null ? attacker.getName().getString() : "无";

        HelloMod.LOGGER.info("[BehaviorLog] ===== {} 死亡！ [死因: {}, 攻击者: {}, 坐标: ({}, {}, {})] =====",
                player.getName().getString(), damageSource.getName(), attackerName,
                String.format("%.1f", player.getX()),
                String.format("%.1f", player.getY()),
                String.format("%.1f", player.getZ()));
    }

    /**
     * 丢弃物品时记录
     */
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"))
    private void onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<?> cir) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        if (stack.isEmpty()) return;

        HelloMod.LOGGER.info("[BehaviorLog] {} 丢弃物品: {} x{}",
                player.getName().getString(), stack.getName().getString(), stack.getCount());
    }

    /**
     * tick 中检测状态变化（疾跑、潜行、游泳、飞行、位置变化、手持切换、饥饿值变化）
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        // 疾跑状态变化
        boolean isSprinting = player.isSprinting();
        if (isSprinting != hello_mod$wasSprinting) {
            hello_mod$wasSprinting = isSprinting;
            HelloMod.LOGGER.info("[BehaviorLog] {} {} 疾跑",
                    player.getName().getString(), isSprinting ? "开始" : "停止");
        }

        // 潜行状态变化
        boolean isSneaking = player.isSneaking();
        if (isSneaking != hello_mod$wasSneaking) {
            hello_mod$wasSneaking = isSneaking;
            HelloMod.LOGGER.info("[BehaviorLog] {} {} 潜行",
                    player.getName().getString(), isSneaking ? "开始" : "停止");
        }

        // 游泳状态变化
        boolean isSwimming = player.isSwimming();
        if (isSwimming != hello_mod$wasSwimming) {
            hello_mod$wasSwimming = isSwimming;
            HelloMod.LOGGER.info("[BehaviorLog] {} {} 游泳",
                    player.getName().getString(), isSwimming ? "开始" : "停止");
        }

        // 飞行状态变化
        boolean isFlying = player.getAbilities().flying;
        if (isFlying != hello_mod$wasFlying) {
            hello_mod$wasFlying = isFlying;
            HelloMod.LOGGER.info("[BehaviorLog] {} {} 飞行",
                    player.getName().getString(), isFlying ? "开始" : "停止");
        }

        // 手持物品切换
        int currentSlot = player.getInventory().selectedSlot;
        if (currentSlot != hello_mod$lastSelectedSlot) {
            if (hello_mod$lastSelectedSlot != -1) {
                ItemStack currentItem = player.getInventory().getMainHandStack();
                String itemName = currentItem.isEmpty() ? "空" : currentItem.getName().getString() + " x" + currentItem.getCount();
                HelloMod.LOGGER.info("[BehaviorLog] {} 切换快捷栏: 槽位{} -> 槽位{} [当前: {}]",
                        player.getName().getString(), hello_mod$lastSelectedSlot + 1, currentSlot + 1, itemName);
            }
            hello_mod$lastSelectedSlot = currentSlot;
        }

        // 饥饿值变化
        int currentFood = player.getHungerManager().getFoodLevel();
        if (hello_mod$lastFoodLevel == -1) {
            hello_mod$lastFoodLevel = currentFood;
        } else if (currentFood != hello_mod$lastFoodLevel) {
            HelloMod.LOGGER.info("[BehaviorLog] {} 饥饿值变化: {} -> {}",
                    player.getName().getString(), hello_mod$lastFoodLevel, currentFood);
            hello_mod$lastFoodLevel = currentFood;
        }

        // 位置变化（每秒记录或移动超过2格时记录）
        Vec3d currentPos = player.getPos();
        if (hello_mod$posLogCooldown > 0) {
            hello_mod$posLogCooldown--;
        }
        if (hello_mod$lastLoggedPos == null) {
            hello_mod$lastLoggedPos = currentPos;
        } else if (hello_mod$posLogCooldown <= 0) {
            double distance = hello_mod$lastLoggedPos.distanceTo(currentPos);
            if (distance >= 2.0) {
                HelloMod.LOGGER.info("[BehaviorLog] {} 移动: ({}, {}, {}) -> ({}, {}, {}) [距离: {}格, 方向: {}]",
                        player.getName().getString(),
                        String.format("%.1f", hello_mod$lastLoggedPos.x),
                        String.format("%.1f", hello_mod$lastLoggedPos.y),
                        String.format("%.1f", hello_mod$lastLoggedPos.z),
                        String.format("%.1f", currentPos.x),
                        String.format("%.1f", currentPos.y),
                        String.format("%.1f", currentPos.z),
                        String.format("%.1f", distance),
                        hello_mod$getMovementDirection(player));
                hello_mod$lastLoggedPos = currentPos;
                hello_mod$posLogCooldown = 10; // 0.5秒冷却
            }
        }
    }

    @Unique
    private String hello_mod$getMovementDirection(PlayerEntity player) {
        float yaw = player.getYaw() % 360;
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) return "南";
        if (yaw >= 45 && yaw < 135) return "西";
        if (yaw >= 135 && yaw < 225) return "北";
        return "东";
    }

    /**
     * 跳跃时记录
     */
    @Inject(method = "jump", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        HelloMod.LOGGER.info("[BehaviorLog] {} 跳跃 [坐标: ({}, {}, {})]",
                player.getName().getString(),
                String.format("%.1f", player.getX()),
                String.format("%.1f", player.getY()),
                String.format("%.1f", player.getZ()));
    }
}
