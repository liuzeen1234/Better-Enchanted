package com.debugmenu.mixin;

import com.debugmenu.api.DebugMenuApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家行为实时日志 Mixin。
 * 跟踪玩家的所有重要行为：攻击、交互、受伤、死亡、丢弃物品、
 * 跳跃、疾跑/潜行/游泳/飞行状态变化、移动、快捷栏切换、饥饿值变化。
 *
 * 通过 DebugMenuApi 中注册的开关控制，默认关闭。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerBehaviorLogMixin {

    @Unique
    private static final Logger debug_menu$LOGGER = LoggerFactory.getLogger("DebugMenu");

    @Unique
    private static final String debug_menu$BEHAVIOR_LOG_KEY = "behavior_log";

    @Unique
    private int debug_menu$lastSelectedSlot = -1;

    @Unique
    private boolean debug_menu$wasSprinting = false;

    @Unique
    private boolean debug_menu$wasSneaking = false;

    @Unique
    private boolean debug_menu$wasSwimming = false;

    @Unique
    private boolean debug_menu$wasFlying = false;

    @Unique
    private Vec3d debug_menu$lastLoggedPos = null;

    @Unique
    private int debug_menu$posLogCooldown = 0;

    @Unique
    private int debug_menu$lastFoodLevel = -1;

    @Unique
    private boolean debug_menu$isBehaviorLogEnabled() {
        return DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void debug_menu$onAttack(Entity target, CallbackInfo ci) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        String targetName = target.getName().getString();
        String targetType = target.getType().getUntranslatedName();
        ItemStack weapon = player.getMainHandStack();
        String weaponName = weapon.isEmpty() ? "空手" : weapon.getName().getString();

        if (target instanceof LivingEntity living) {
            debug_menu$LOGGER.info("[BehaviorLog] {} 攻击了 {} ({}) [武器: {}, 目标血量: {}/{}]",
                    player.getName().getString(), targetName, targetType, weaponName,
                    String.format("%.1f", living.getHealth()), String.format("%.1f", living.getMaxHealth()));
        } else {
            debug_menu$LOGGER.info("[BehaviorLog] {} 攻击了 {} ({}) [武器: {}]",
                    player.getName().getString(), targetName, targetType, weaponName);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void debug_menu$onInteractEntity(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        ItemStack heldItem = player.getStackInHand(hand);
        String itemName = heldItem.isEmpty() ? "空手" : heldItem.getName().getString();
        String entityName = entity.getName().getString();
        String entityType = entity.getType().getUntranslatedName();

        debug_menu$LOGGER.info("[BehaviorLog] {} 右键交互实体: {} ({}) [手持: {}, 手: {}]",
                player.getName().getString(), entityName, entityType, itemName, hand.name());
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void debug_menu$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        String sourceName = source.getName();
        Entity attacker = source.getAttacker();
        String attackerName = attacker != null ? attacker.getName().getString() : "无";

        debug_menu$LOGGER.info("[BehaviorLog] {} 受到伤害 [来源: {}, 攻击者: {}, 伤害量: {}, 当前血量: {}/{}]",
                player.getName().getString(), sourceName, attackerName,
                String.format("%.1f", amount),
                String.format("%.1f", player.getHealth()),
                String.format("%.1f", player.getMaxHealth()));
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void debug_menu$onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        Entity attacker = damageSource.getAttacker();
        String attackerName = attacker != null ? attacker.getName().getString() : "无";

        debug_menu$LOGGER.info("[BehaviorLog] ===== {} 死亡！ [死因: {}, 攻击者: {}, 坐标: ({}, {}, {})] =====",
                player.getName().getString(), damageSource.getName(), attackerName,
                String.format("%.1f", player.getX()),
                String.format("%.1f", player.getY()),
                String.format("%.1f", player.getZ()));
    }

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"))
    private void debug_menu$onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<?> cir) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;
        if (stack.isEmpty()) return;

        debug_menu$LOGGER.info("[BehaviorLog] {} 丢弃物品: {} x{}",
                player.getName().getString(), stack.getName().getString(), stack.getCount());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void debug_menu$onTick(CallbackInfo ci) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        // 疾跑状态变化
        boolean isSprinting = player.isSprinting();
        if (isSprinting != debug_menu$wasSprinting) {
            debug_menu$wasSprinting = isSprinting;
            debug_menu$LOGGER.info("[BehaviorLog] {} {} 疾跑",
                    player.getName().getString(), isSprinting ? "开始" : "停止");
        }

        // 潜行状态变化
        boolean isSneaking = player.isSneaking();
        if (isSneaking != debug_menu$wasSneaking) {
            debug_menu$wasSneaking = isSneaking;
            debug_menu$LOGGER.info("[BehaviorLog] {} {} 潜行",
                    player.getName().getString(), isSneaking ? "开始" : "停止");
        }

        // 游泳状态变化
        boolean isSwimming = player.isSwimming();
        if (isSwimming != debug_menu$wasSwimming) {
            debug_menu$wasSwimming = isSwimming;
            debug_menu$LOGGER.info("[BehaviorLog] {} {} 游泳",
                    player.getName().getString(), isSwimming ? "开始" : "停止");
        }

        // 飞行状态变化
        boolean isFlying = player.getAbilities().flying;
        if (isFlying != debug_menu$wasFlying) {
            debug_menu$wasFlying = isFlying;
            debug_menu$LOGGER.info("[BehaviorLog] {} {} 飞行",
                    player.getName().getString(), isFlying ? "开始" : "停止");
        }

        // 手持物品切换
        int currentSlot = player.getInventory().selectedSlot;
        if (currentSlot != debug_menu$lastSelectedSlot) {
            if (debug_menu$lastSelectedSlot != -1) {
                ItemStack currentItem = player.getInventory().getMainHandStack();
                String itemName = currentItem.isEmpty() ? "空" : currentItem.getName().getString() + " x" + currentItem.getCount();
                debug_menu$LOGGER.info("[BehaviorLog] {} 切换快捷栏: 槽位{} -> 槽位{} [当前: {}]",
                        player.getName().getString(), debug_menu$lastSelectedSlot + 1, currentSlot + 1, itemName);
            }
            debug_menu$lastSelectedSlot = currentSlot;
        }

        // 饥饿值变化
        int currentFood = player.getHungerManager().getFoodLevel();
        if (debug_menu$lastFoodLevel == -1) {
            debug_menu$lastFoodLevel = currentFood;
        } else if (currentFood != debug_menu$lastFoodLevel) {
            debug_menu$LOGGER.info("[BehaviorLog] {} 饥饿值变化: {} -> {}",
                    player.getName().getString(), debug_menu$lastFoodLevel, currentFood);
            debug_menu$lastFoodLevel = currentFood;
        }

        // 位置变化
        Vec3d currentPos = player.getPos();
        if (debug_menu$posLogCooldown > 0) {
            debug_menu$posLogCooldown--;
        }
        if (debug_menu$lastLoggedPos == null) {
            debug_menu$lastLoggedPos = currentPos;
        } else if (debug_menu$posLogCooldown <= 0) {
            double distance = debug_menu$lastLoggedPos.distanceTo(currentPos);
            if (distance >= 2.0) {
                debug_menu$LOGGER.info("[BehaviorLog] {} 移动: ({}, {}, {}) -> ({}, {}, {}) [距离: {}格, 方向: {}]",
                        player.getName().getString(),
                        String.format("%.1f", debug_menu$lastLoggedPos.x),
                        String.format("%.1f", debug_menu$lastLoggedPos.y),
                        String.format("%.1f", debug_menu$lastLoggedPos.z),
                        String.format("%.1f", currentPos.x),
                        String.format("%.1f", currentPos.y),
                        String.format("%.1f", currentPos.z),
                        String.format("%.1f", distance),
                        debug_menu$getMovementDirection(player));
                debug_menu$lastLoggedPos = currentPos;
                debug_menu$posLogCooldown = 10;
            }
        }
    }

    @Unique
    private String debug_menu$getMovementDirection(PlayerEntity player) {
        float yaw = player.getYaw() % 360;
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) return "南";
        if (yaw >= 45 && yaw < 135) return "西";
        if (yaw >= 135 && yaw < 225) return "北";
        return "东";
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void debug_menu$onJump(CallbackInfo ci) {
        if (!debug_menu$isBehaviorLogEnabled()) return;
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient()) return;

        debug_menu$LOGGER.info("[BehaviorLog] {} 跳跃 [坐标: ({}, {}, {})]",
                player.getName().getString(),
                String.format("%.1f", player.getX()),
                String.format("%.1f", player.getY()),
                String.format("%.1f", player.getZ()));
    }
}
