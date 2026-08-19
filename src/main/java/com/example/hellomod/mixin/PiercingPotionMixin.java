package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.damage.PowerPotionDamageSource;
import com.example.hellomod.damage.SharpPotionDamageSource;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 穿透 (Piercing) 附魔 — 药水版本
 *
 * 参考 Minecraft Java 1.20.4 弩穿透附魔规则：
 * - 投射物可以穿过等于附魔等级数量的实体（总共命中 level + 1 个实体）
 * - 穿透后继续飞行，对路径上的每个实体进行一次击中判定
 * - 命中方块时正常停止/消失
 * - 已击中的实体不会被重复命中
 *
 * 实现方式：
 * 1. 在 onCollision 的 HEAD 注入，优先级高于 PotionEntityMixin
 * 2. 当穿透生效时（实体命中 + 未达上限）：
 *    - 手动对命中实体施加附魔效果（锋利/力量/冲击/火矢）
 *    - 记录已击中的实体 UUID
 *    - 取消整个 onCollision 调用，阻止药水被销毁
 * 3. 当穿透次数耗尽或命中方块时，不取消，让原版 + PotionEntityMixin 正常处理
 */
@Mixin(value = PotionEntity.class, priority = 900)
public abstract class PiercingPotionMixin {

    /**
     * 内存中缓存已穿透的实体UUID列表
     */
    @Unique
    private Set<UUID> piercedEntities;

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onPiercingCheck(HitResult hitResult, CallbackInfo ci) {
        PotionEntity self = (PotionEntity) (Object) this;
        World world = self.getWorld();

        if (world.isClient()) {
            return;
        }

        ItemStack potionStack = self.getStack();
        int piercingLevel = EnchantmentHelper.getLevel(Enchantments.PIERCING, potionStack);

        if (piercingLevel <= 0) {
            return;
        }

        // 方块命中：正常销毁，不穿透
        if (hitResult.getType() != HitResult.Type.ENTITY) {
            HelloMod.LOGGER.info("[Piercing] Block hit, potion will be destroyed normally.");
            return;
        }

        EntityHitResult entityHitResult = (EntityHitResult) hitResult;
        Entity hitEntity = entityHitResult.getEntity();

        // 初始化穿透实体列表
        if (piercedEntities == null) {
            piercedEntities = loadPiercedEntities(potionStack);
        }

        // 已经穿透过的实体，跳过整个碰撞
        if (piercedEntities.contains(hitEntity.getUuid())) {
            ci.cancel();
            return;
        }

        // 检查穿透数量：可穿透 level 个实体，总共命中 level + 1 个
        int alreadyPierced = piercedEntities.size();

        if (alreadyPierced >= piercingLevel) {
            // 达到穿透上限，这是最后一次命中
            // 让原版 onCollision + PotionEntityMixin 正常处理（包括销毁）
            HelloMod.LOGGER.info("[Piercing] Final hit ({}/{}), target: {}. Potion will be destroyed.",
                    alreadyPierced + 1, piercingLevel + 1, hitEntity.getName().getString());
            return;
        }

        // 穿透生效：手动应用附魔效果，然后取消 onCollision 防止药水被销毁
        piercedEntities.add(hitEntity.getUuid());
        savePiercedEntities(potionStack, piercedEntities);

        HelloMod.LOGGER.info("[Piercing] Pierced through entity: {} ({}/{}). Potion continues flying.",
                hitEntity.getName().getString(), piercedEntities.size(), piercingLevel + 1);

        // 手动应用附魔伤害/效果（复制 PotionEntityMixin 的逻辑）
        applyEnchantmentEffects(self, potionStack, hitEntity);

        // 取消原版 onCollision，药水继续飞行
        ci.cancel();
    }

    /**
     * 手动对命中实体应用附魔效果。
     * 穿透时需要绕过原版 onCollision，所以手动处理锋利/力量/冲击/火矢。
     * 注意：穿透时不触发溅射范围效果（只对直接命中实体生效），
     * 因为溅射效果在最终落地时由原版 onCollision 处理。
     */
    @Unique
    private void applyEnchantmentEffects(PotionEntity self, ItemStack potionStack, Entity hitEntity) {
        int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, potionStack);
        int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, potionStack);
        int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, potionStack);
        int flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, potionStack);

        if (!(hitEntity instanceof LivingEntity livingTarget)) {
            return;
        }

        World world = self.getWorld();

        // 锋利伤害公式：0.5 * level + 0.5
        float sharpnessDamage = sharpnessLevel > 0 ? (0.5f * sharpnessLevel + 0.5f) : 0f;

        // 力量伤害公式：level + 1
        float powerDamage = powerLevel > 0 ? (powerLevel + 1.0f) : 0f;

        float totalDamage = sharpnessDamage + powerDamage;

        // 选择伤害源
        DamageSource damageSource = null;
        if (totalDamage > 0) {
            if (powerLevel > 0 && powerDamage >= sharpnessDamage) {
                damageSource = PowerPotionDamageSource.create(world);
            } else {
                damageSource = SharpPotionDamageSource.create(world);
            }
        }

        // 造成伤害
        if (totalDamage > 0 && damageSource != null) {
            HelloMod.LOGGER.info("[Piercing] Damage on pierced entity: {}, damage={}",
                    livingTarget.getName().getString(), totalDamage);
            livingTarget.damage(damageSource, totalDamage);
        }

        // 冲击击退
        if (punchLevel > 0) {
            Vec3d velocity = self.getVelocity();
            double horizontalLength = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalLength > 0.0) {
                double knockbackStrength = punchLevel * 0.6;
                livingTarget.takeKnockback(knockbackStrength, -velocity.x / horizontalLength, -velocity.z / horizontalLength);
                HelloMod.LOGGER.info("[Piercing] Punch knockback on pierced entity: {}, strength={}",
                        livingTarget.getName().getString(), knockbackStrength);
            }
        }

        // 火矢
        if (flameLevel > 0) {
            livingTarget.setOnFireFor(5);
            HelloMod.LOGGER.info("[Piercing] Flame ignited pierced entity: {} for 5 seconds",
                    livingTarget.getName().getString());
        }

        // 药水原版效果（溅射药水区域效果）不在穿透时触发
        // 只有当药水最终停止（命中方块或达到穿透上限）时才触发区域效果
    }

    /**
     * 从 NBT 加载已穿透的实体列表
     */
    @Unique
    private Set<UUID> loadPiercedEntities(ItemStack stack) {
        Set<UUID> set = new HashSet<>();
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("PiercedEntities")) {
            NbtList list = nbt.getList("PiercedEntities", 8); // 8 = NbtString type
            for (int i = 0; i < list.size(); i++) {
                try {
                    set.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return set;
    }

    /**
     * 将已穿透的实体列表保存到 NBT
     */
    @Unique
    private void savePiercedEntities(ItemStack stack, Set<UUID> entities) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList list = new NbtList();
        for (UUID uuid : entities) {
            list.add(NbtString.of(uuid.toString()));
        }
        nbt.put("PiercedEntities", list);
    }
}
