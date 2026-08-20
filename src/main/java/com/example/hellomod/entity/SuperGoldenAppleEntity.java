package com.example.hellomod.entity;

import com.example.hellomod.HelloMod;
import com.example.hellomod.damage.SharpPotionDamageSource;
import com.example.hellomod.damage.PowerPotionDamageSource;
import com.example.hellomod.item.ModItems;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 超级附魔金苹果投掷实体。
 *
 * 落地后行为：
 * 1. 先触发喷溅效果 — 范围4格内实体获得 buff，距离越远持续时间越短
 * 2. 再生成效果云 — AreaEffectCloud
 * 3. 应用附魔效果（锋利、力量、冲击、火矢、引雷）
 */
public class SuperGoldenAppleEntity extends ThrownItemEntity {

    /** 附魔数据 NBT */
    private NbtCompound appleNbt = new NbtCompound();

    public SuperGoldenAppleEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public SuperGoldenAppleEntity(World world, PlayerEntity user) {
        super(ModEntities.SUPER_GOLDEN_APPLE_ENTITY, user, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SUPER_ENCHANTED_GOLDEN_APPLE;
    }

    public NbtCompound getAppleNbt() {
        return appleNbt;
    }

    public void setAppleNbt(NbtCompound nbt) {
        this.appleNbt = nbt;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("AppleEnchantData", appleNbt.copy());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("AppleEnchantData")) {
            appleNbt = nbt.getCompound("AppleEnchantData").copy();
        }
    }

    @Override
    public void tick() {
        // 射线追踪传送逻辑（迅投等级>20）
        if (!this.getWorld().isClient() && appleNbt.getBoolean("SwiftThrowRaycast")) {
            handleRaycastTick();
            return; // 跳过正常物理 tick
        }
        super.tick();
    }

    private void handleRaycastTick() {
        float speed = appleNbt.getFloat("SwiftThrowSpeed");
        double dirX = appleNbt.getDouble("SwiftThrowDirX");
        double dirY = appleNbt.getDouble("SwiftThrowDirY");
        double dirZ = appleNbt.getDouble("SwiftThrowDirZ");

        if (speed <= 0) return;

        Vec3d direction = new Vec3d(dirX, dirY, dirZ).normalize();
        Vec3d startPos = this.getPos();
        Vec3d endPos = startPos.add(direction.multiply(speed));

        // 射线追踪方块
        RaycastContext raycastContext = new RaycastContext(
                startPos, endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this
        );
        BlockHitResult blockHit = this.getWorld().raycast(raycastContext);

        // 检查实体
        EntityHitResult entityHit = raycastEntity(startPos, endPos);

        // 确定最近命中
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
            // 命中：清除标记并触发碰撞
            appleNbt.remove("SwiftThrowRaycast");
            appleNbt.remove("SwiftThrowSpeed");
            appleNbt.remove("SwiftThrowDirX");
            appleNbt.remove("SwiftThrowDirY");
            appleNbt.remove("SwiftThrowDirZ");

            Vec3d hitPos = closestHit.getPos();
            this.setPosition(hitPos);
            this.onCollision(closestHit);
        } else {
            // 未命中：传送到射线末端
            this.setPosition(endPos);
        }
    }

    private EntityHitResult raycastEntity(Vec3d start, Vec3d end) {
        Vec3d diff = end.subtract(start);
        Box searchBox = this.getBoundingBox().stretch(diff).expand(1.0);

        Entity owner = this.getOwner();
        Predicate<Entity> filter = entity ->
                !entity.isSpectator() && entity.canHit() && entity != owner && entity != this;

        List<Entity> entities = this.getWorld().getOtherEntities(this, searchBox, filter);

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

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (!this.getWorld().isClient()) {
            Vec3d hitPos = hitResult.getPos();

            // 1. 喷溅效果
            applySplashEffects(hitPos);

            // 2. 附魔效果（锋利、力量、冲击、火矢、引雷）
            applyEnchantEffects(hitPos, hitResult);

            // 3. 生成效果云
            spawnAreaEffectCloud(hitPos);

            // 移除实体
            this.discard();
        }
    }

    /**
     * 喷溅效果：范围4格内所有生物获得 buff。
     * 持续时间按距离衰减：time = maxTime * (1 - distance/4)
     */
    private void applySplashEffects(Vec3d hitPos) {
        World world = this.getWorld();
        Box box = new Box(hitPos.x - 4, hitPos.y - 4, hitPos.z - 4,
                hitPos.x + 4, hitPos.y + 4, hitPos.z + 4);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> true);

        for (LivingEntity entity : entities) {
            double distance = entity.getPos().distanceTo(hitPos);
            if (distance > 4.0) continue;

            double durationMultiplier = 1.0 - distance / 4.0;
            if (durationMultiplier <= 0) continue;

            // Regeneration V (30s = 600 ticks)
            int regenDuration = (int) (600 * durationMultiplier);
            if (regenDuration > 0) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, regenDuration, 4));
            }

            // Absorption IV (2min = 2400 ticks)
            int absorpDuration = (int) (2400 * durationMultiplier);
            if (absorpDuration > 0) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, absorpDuration, 3));
            }

            // Resistance I (5min = 6000 ticks)
            int resistDuration = (int) (6000 * durationMultiplier);
            if (resistDuration > 0) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistDuration, 0));
            }

            // Fire Resistance I (5min = 6000 ticks)
            int fireResDuration = (int) (6000 * durationMultiplier);
            if (fireResDuration > 0) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResDuration, 0));
            }
        }
    }

    /**
     * 应用附魔效果：锋利、力量、冲击、火矢、引雷。
     */
    private void applyEnchantEffects(Vec3d hitPos, HitResult hitResult) {
        World world = this.getWorld();
        int sharpnessLevel = appleNbt.getInt("SharpnessLevel");
        int powerLevel = appleNbt.getInt("PowerLevel");
        int punchLevel = appleNbt.getInt("PunchLevel");
        int flameLevel = appleNbt.getInt("FlameLevel");
        int channelingLevel = appleNbt.getInt("ChannelingLevel");

        if (sharpnessLevel <= 0 && powerLevel <= 0 && punchLevel <= 0
                && flameLevel <= 0 && channelingLevel <= 0) {
            return;
        }

        Box box = new Box(hitPos.x - 4, hitPos.y - 4, hitPos.z - 4,
                hitPos.x + 4, hitPos.y + 4, hitPos.z + 4);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> true);

        Entity directHitEntity = null;
        if (hitResult instanceof EntityHitResult entityHitResult) {
            directHitEntity = entityHitResult.getEntity();
        }

        for (LivingEntity entity : entities) {
            double distance = entity.getPos().distanceTo(hitPos);
            if (distance > 4.0) continue;

            double distanceFactor = 1.0 - distance / 4.0;
            boolean isDirect = entity == directHitEntity;

            // 锋利：伤害 = 0.5 * level + 0.5
            if (sharpnessLevel > 0) {
                float damage = (0.5f * sharpnessLevel + 0.5f) * (isDirect ? 1.0f : (float) distanceFactor);
                if (damage > 0) {
                    entity.damage(SharpPotionDamageSource.create(world), damage);
                }
            }

            // 力量：伤害 = level + 1（与锋利叠加）
            if (powerLevel > 0) {
                float damage = (powerLevel + 1.0f) * (isDirect ? 1.0f : (float) distanceFactor);
                if (damage > 0) {
                    entity.damage(PowerPotionDamageSource.create(world), damage);
                }
            }

            // 冲击：击退
            if (punchLevel > 0) {
                double knockbackStrength = punchLevel * 0.6 * (isDirect ? 1.0 : distanceFactor);
                Vec3d velocity = this.getVelocity();
                double horizLen = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                if (horizLen > 0.0001) {
                    entity.takeKnockback(knockbackStrength, -velocity.x / horizLen, -velocity.z / horizLen);
                }
            }

            // 火矢：点燃
            if (flameLevel > 0) {
                int fireTime = isDirect ? 5 : (int) (5 * distanceFactor);
                if (fireTime > 0) {
                    entity.setOnFireFor(fireTime);
                }
            }

            // 引雷：雷暴天气 + 露天
            if (channelingLevel > 0 && world.isThundering()) {
                BlockPos entityPos = entity.getBlockPos();
                if (world.isSkyVisible(entityPos)) {
                    if (world instanceof ServerWorld serverWorld) {
                        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
                        if (lightning != null) {
                            lightning.refreshPositionAfterTeleport(entity.getX(), entity.getY(), entity.getZ());
                            serverWorld.spawnEntity(lightning);
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成效果云（参考滞留型药水机制）。
     */
    private void spawnAreaEffectCloud(Vec3d hitPos) {
        World world = this.getWorld();
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, hitPos.x, hitPos.y, hitPos.z);
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner) {
            cloud.setOwner(livingOwner);
        }

        cloud.setRadius(3.0f);
        cloud.setRadiusOnUse(-0.5f);
        cloud.setWaitTime(10);
        cloud.setRadiusGrowth(-cloud.getRadius() / (float) cloud.getDuration());
        cloud.setDuration(600); // 30s

        // 添加效果
        cloud.addEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 4));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));

        world.spawnEntity(cloud);
    }
}
