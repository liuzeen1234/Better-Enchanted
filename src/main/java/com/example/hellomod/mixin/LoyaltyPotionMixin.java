package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 忠诚 (Loyalty) 附魔 — 药水版本（Tick逻辑部分）
 *
 * 参考 Minecraft Java 1.20.4 三叉戟忠诚附魔规则，改编为药水版本：
 * - 药水掷出后飞行5秒（100 ticks），之后自动返回投掷者
 * - 返回速度：基础速度 × 忠诚等级（等级越高返回越快）
 *   参考原版三叉戟：returnSpeed = loyaltyLevel（1/2/3级分别为1/2/3的速度系数）
 * - 掷出时碰到实体或方块：正常消耗（原版 onCollision 处理）
 * - 返回时碰到实体或方块：正常消耗（原版 onCollision 处理）
 * - 返回到投掷者手中时：归还药水物品给玩家
 *
 * 实现方式：
 * 1. 在 PotionItemMixin 中识别忠诚附魔并传递到投射物
 * 2. 本 Mixin 在 tick 中检测药水是否带有忠诚附魔
 * 3. 飞行阶段（前5秒）：正常物理飞行，碰到东西正常消耗
 * 4. 返回阶段（5秒后）：关闭重力，设置速度朝向投掷者飞行
 * 5. 返回途中碰到实体或方块：由 LoyaltyCollisionMixin 处理
 * 6. 到达投掷者附近（1.5格内）：归还物品，销毁实体
 *
 * NBT 标记（存储在药水 ItemStack 的 NBT 中）：
 * - "LoyaltyTicks": int, 已飞行的 tick 数
 * - "LoyaltyReturning": boolean, 是否处于返回阶段
 * - "LoyaltyReturnTicks": int, 返回阶段已飞行的 tick 数
 * - "LoyaltyOwnerUUID": string, 投掷者 UUID（用于返回寻找目标）
 */
@Mixin(value = ThrownEntity.class, priority = 800)
public abstract class LoyaltyPotionMixin {

    /** 飞行时间阈值：2秒 = 40 ticks */
    @Unique
    private static final int LOYALTY_FLIGHT_TICKS = 40;

    /** 返回速度基础值（格/tick），等级越高速度越快 */
    @Unique
    private static final double LOYALTY_BASE_RETURN_SPEED = 0.5;

    /** 归还距离阈值（格） */
    @Unique
    private static final double LOYALTY_PICKUP_DISTANCE = 1.5;

    /** 最大返回时间（防止永远追踪）：10秒 = 200 ticks */
    @Unique
    private static final int LOYALTY_MAX_RETURN_TICKS = 200;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onLoyaltyTick(CallbackInfo ci) {
        ThrownEntity self = (ThrownEntity) (Object) this;

        // 只处理 PotionEntity
        if (!(self instanceof PotionEntity potionEntity)) {
            return;
        }

        World world = self.getWorld();
        if (world.isClient()) {
            return;
        }

        ItemStack potionStack = potionEntity.getStack();
        int loyaltyLevel = EnchantmentHelper.getLevel(Enchantments.LOYALTY, potionStack);

        if (loyaltyLevel <= 0) {
            return;
        }

        NbtCompound nbt = potionStack.getOrCreateNbt();

        // 初始化飞行计时器（首次进入）
        if (!nbt.contains("LoyaltyTicks")) {
            nbt.putInt("LoyaltyTicks", 0);
            nbt.putBoolean("LoyaltyReturning", false);
            // 记录投掷者 UUID
            Entity owner = potionEntity.getOwner();
            if (owner != null) {
                nbt.putString("LoyaltyOwnerUUID", owner.getUuidAsString());
            }
            HelloMod.LOGGER.info("[Loyalty] Initialized loyalty potion! Level={}, owner={}",
                    loyaltyLevel, owner != null ? owner.getName().getString() : "null");
            return; // 第一tick 初始化完成，正常执行原版 tick
        }

        boolean isReturning = nbt.getBoolean("LoyaltyReturning");
        int ticks = nbt.getInt("LoyaltyTicks");

        if (!isReturning) {
            // === 飞行阶段：计时，到5秒后切换为返回阶段 ===
            ticks++;
            nbt.putInt("LoyaltyTicks", ticks);

            if (ticks >= LOYALTY_FLIGHT_TICKS) {
                // 切换到返回阶段
                nbt.putBoolean("LoyaltyReturning", true);
                nbt.putInt("LoyaltyReturnTicks", 0);

                // 关闭重力，准备返回
                self.setNoGravity(true);

                HelloMod.LOGGER.info("[Loyalty] Flight phase complete ({} ticks). Switching to return phase.",
                        ticks);
            }
            // 飞行阶段不干预物理，让原版 tick 正常执行
            return;
        }

        // === 返回阶段：朝投掷者飞行 ===
        int returnTicks = nbt.getInt("LoyaltyReturnTicks");
        returnTicks++;
        nbt.putInt("LoyaltyReturnTicks", returnTicks);

        // 安全检查：超过最大返回时间，销毁
        if (returnTicks > LOYALTY_MAX_RETURN_TICKS) {
            HelloMod.LOGGER.info("[Loyalty] Return timeout! Discarding potion.");
            self.discard();
            ci.cancel();
            return;
        }

        // 找到投掷者
        Entity owner = potionEntity.getOwner();
        if (owner == null || !owner.isAlive()) {
            // 通过 UUID 找
            String ownerUUID = nbt.getString("LoyaltyOwnerUUID");
            if (!ownerUUID.isEmpty() && world.getServer() != null) {
                try {
                    ServerPlayerEntity serverPlayer = world.getServer().getPlayerManager()
                            .getPlayer(java.util.UUID.fromString(ownerUUID));
                    if (serverPlayer != null && serverPlayer.isAlive()) {
                        owner = serverPlayer;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (owner == null || !owner.isAlive()) {
            // 找不到投掷者，销毁
            HelloMod.LOGGER.info("[Loyalty] Owner not found or dead. Discarding potion.");
            self.discard();
            ci.cancel();
            return;
        }

        // 计算到投掷者的方向和距离
        Vec3d potionPos = self.getPos();
        Vec3d ownerPos = owner.getEyePos(); // 朝向玩家眼睛位置
        Vec3d direction = ownerPos.subtract(potionPos);
        double distance = direction.length();

        // 到达归还距离：归还物品
        if (distance <= LOYALTY_PICKUP_DISTANCE) {
            // 归还药水给玩家
            if (owner instanceof PlayerEntity player) {
                // 给予物品
                ItemStack returnStack = potionStack.copy();
                // 清除忠诚相关的运行时 NBT
                NbtCompound returnNbt = returnStack.getNbt();
                if (returnNbt != null) {
                    returnNbt.remove("LoyaltyTicks");
                    returnNbt.remove("LoyaltyReturning");
                    returnNbt.remove("LoyaltyReturnTicks");
                    returnNbt.remove("LoyaltyOwnerUUID");
                }

                if (!player.getInventory().insertStack(returnStack)) {
                    // 背包满了，掉落在地
                    player.dropItem(returnStack, false);
                }

                // 播放回收音效（参考三叉戟回收音效）
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS,
                        1.0f, 1.0f);

                HelloMod.LOGGER.info("[Loyalty] Potion returned to player: {}",
                        player.getName().getString());
            }

            // 销毁药水实体
            self.discard();
            ci.cancel();
            return;
        }

        // 计算返回速度（参考原版三叉戟忠诚返回逻辑）
        // 原版三叉戟 Loyalty 每tick: velocity = direction.normalize() * 0.05 * loyaltyLevel
        // 我们的药水使用更大的基础速度以适配药水的使用场景
        double returnSpeed = LOYALTY_BASE_RETURN_SPEED * loyaltyLevel;
        Vec3d returnVelocity = direction.normalize().multiply(returnSpeed);

        // 设置速度朝向投掷者
        self.setVelocity(returnVelocity);
        self.velocityModified = true;
        self.setNoGravity(true);

        // 不取消 tick — 让原版 ThrownEntity.tick() 继续执行碰撞检测
        // 如果返回途中碰到方块或其他实体（非投掷者），原版 onCollision 会被触发，药水正常消耗
        // 投掷者碰撞的排除由 LoyaltyCollisionMixin 处理
    }
}
