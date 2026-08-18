package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 迅投附魔等级>20时的射线追踪传送逻辑。
 *
 * Mixin到ThrownEntity（PotionEntity的父类），在tick方法中：
 * 当药水带有 SwiftThrowRaycast NBT标记时，每tick执行：
 * 1. 计算本tick药水应飞过的距离（= SwiftThrowSpeed 格/tick）
 * 2. 从当前位置沿存储的方向做射线追踪
 * 3. 若命中方块或实体：传送药水到命中点，清除标记让原版碰撞逻辑生效
 * 4. 若未命中：传送药水到射线末端，下tick继续
 */
@Mixin(ThrownEntity.class)
public abstract class SwiftThrowTickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        ThrownEntity self = (ThrownEntity) (Object) this;

        // 只处理 PotionEntity
        if (!(self instanceof PotionEntity potionEntity)) {
            return;
        }

        World world = self.getWorld();
        if (world.isClient()) {
            return;
        }

        NbtCompound potionNbt = potionEntity.getStack().getNbt();
        if (potionNbt == null || !potionNbt.getBoolean("SwiftThrowRaycast")) {
            return;
        }

        float speed = potionNbt.getFloat("SwiftThrowSpeed");
        double dirX = potionNbt.getDouble("SwiftThrowDirX");
        double dirY = potionNbt.getDouble("SwiftThrowDirY");
        double dirZ = potionNbt.getDouble("SwiftThrowDirZ");

        if (speed <= 0) {
            return;
        }

        Vec3d direction = new Vec3d(dirX, dirY, dirZ).normalize();
        Vec3d startPos = self.getPos();
        Vec3d endPos = startPos.add(direction.multiply(speed));

        // 1. 射线追踪方块
        RaycastContext raycastContext = new RaycastContext(
                startPos, endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                self
        );
        BlockHitResult blockHit = world.raycast(raycastContext);

        // 2. 检查射线路径上的实体
        EntityHitResult entityHit = raycastEntity(world, self, startPos, endPos);

        // 3. 确定最近的命中点
        HitResult closestHit = null;
        double closestDist = Double.MAX_VALUE;

        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double dist = startPos.squaredDistanceTo(blockHit.getPos());
            if (dist < closestDist) {
                closestDist = dist;
                closestHit = blockHit;
            }
        }

        if (entityHit != null) {
            double dist = startPos.squaredDistanceTo(entityHit.getPos());
            if (dist < closestDist) {
                closestDist = dist;
                closestHit = entityHit;
            }
        }

        if (closestHit != null && closestHit.getType() != HitResult.Type.MISS) {
            // 命中！传送药水到命中点，清除射线追踪标记
            Vec3d hitPos = closestHit.getPos();
            self.setPosition(hitPos);

            // 清除射线追踪标记使其回归正常行为
            potionNbt.remove("SwiftThrowRaycast");
            potionNbt.remove("SwiftThrowSpeed");
            potionNbt.remove("SwiftThrowDirX");
            potionNbt.remove("SwiftThrowDirY");
            potionNbt.remove("SwiftThrowDirZ");

            // 设置速度为方向上的微小值，触发原版碰撞检测
            self.setVelocity(direction.multiply(0.1));

            HelloMod.LOGGER.info("[SwiftThrow Raycast] HIT at ({}, {}, {}), type={}",
                    hitPos.x, hitPos.y, hitPos.z, closestHit.getType());

            // 不取消tick，让原版tick处理碰撞事件
        } else {
            // 未命中：传送到射线末端
            self.setPosition(endPos);
            // 保持微小速度避免实体被移除（0 tick速度的投射物可能被清理）
            self.setVelocity(direction.multiply(0.01));

            HelloMod.LOGGER.info("[SwiftThrow Raycast] No hit, teleported to ({}, {}, {})",
                    endPos.x, endPos.y, endPos.z);

            // 取消原版tick的位移逻辑，因为我们自己处理了
            // 不取消，因为需要age计数等生命周期处理
        }
    }

    /**
     * 在射线路径上检查实体碰撞。
     */
    private static EntityHitResult raycastEntity(World world, Entity self, Vec3d start, Vec3d end) {
        Vec3d diff = end.subtract(start);
        Box searchBox = self.getBoundingBox().stretch(diff).expand(1.0);

        Entity owner = null;
        if (self instanceof ThrownEntity thrownEntity) {
            owner = thrownEntity.getOwner();
        }

        Entity finalOwner = owner;
        Predicate<Entity> filter = entity ->
                !entity.isSpectator()
                        && entity.canHit()
                        && entity != finalOwner
                        && entity != self;

        List<Entity> entities = world.getOtherEntities(self, searchBox, filter);

        Entity closestEntity = null;
        Vec3d closestPos = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            Box entityBox = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> hitOpt = entityBox.raycast(start, end);

            if (hitOpt.isPresent()) {
                double dist = start.squaredDistanceTo(hitOpt.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestEntity = entity;
                    closestPos = hitOpt.get();
                }
            }
        }

        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestPos);
        }
        return null;
    }
}
