package com.example.hellomod.item;

import com.example.hellomod.HelloMod;
import com.example.hellomod.enchantment.InfinityCooldownManager;
import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.enchantment.SwiftThrowEnchantment;
import com.example.hellomod.entity.UltimateGoldenAppleEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 终极附魔金苹果物品。
 *
 * 功能：
 * - 双模式切换（食用/投掷），通过左键切换（客户端 mixin 处理，2tick冷却）
 * - 食用模式：瞬吃（效率9），给予 Regeneration V (60s), Resistance III (60s), Strength V (60s)
 * - 投掷模式：右键投掷，落地后4格范围检测，敌对100真实伤害，友好/玩家生命恢复V(60s)
 * - 自带附魔：效率9、耐久10、迅投25、无限1
 * - 食用消耗：耐久判定成功不消耗+3s冷却，失败消耗无冷却
 * - 投掷消耗：无限机制（同超级版）
 */
public class UltimateEnchantedGoldenAppleItem extends Item {

    /** NBT key 用于存储当前模式 */
    public static final String MODE_KEY = "UltimateAppleMode";
    /** 模式值：食用 */
    public static final String MODE_EAT = "eat";
    /** 模式值：投掷 */
    public static final String MODE_THROW = "throw";

    /** 食用时间（32 tick，受效率附魔影响后会大幅缩短） */
    private static final int EAT_DURATION = 32;

    private static final FoodComponent FOOD_COMPONENT = new FoodComponent.Builder()
            .alwaysEdible()
            .hunger(4)
            .saturationModifier(1.2f)
            .build();

    public UltimateEnchantedGoldenAppleItem(Settings settings) {
        super(settings.food(FOOD_COMPONENT));
    }

    // ===== 模式管理 =====

    public static String getMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(MODE_KEY)) {
            return nbt.getString(MODE_KEY);
        }
        return MODE_EAT;
    }

    public static void setMode(ItemStack stack, String mode) {
        stack.getOrCreateNbt().putString(MODE_KEY, mode);
    }

    public static void toggleMode(ItemStack stack) {
        String current = getMode(stack);
        if (MODE_EAT.equals(current)) {
            setMode(stack, MODE_THROW);
        } else {
            setMode(stack, MODE_EAT);
        }
    }

    public static boolean isThrowMode(ItemStack stack) {
        return MODE_THROW.equals(getMode(stack));
    }

    // ===== 物品名称动态变化（亮金色） =====

    @Override
    public Text getName(ItemStack stack) {
        if (isThrowMode(stack)) {
            return Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.throw")
                    .formatted(Formatting.GOLD);
        }
        return Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple")
                .formatted(Formatting.GOLD);
    }

    // ===== 工具提示 =====

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        // 显示当前模式
        if (isThrowMode(stack)) {
            tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.mode_throw")
                    .formatted(Formatting.GOLD));
        } else {
            tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.mode_eat")
                    .formatted(Formatting.GREEN));
        }

        // 操作提示
        tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.switch")
                .formatted(Formatting.DARK_GRAY));

        // 食用效果
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.eat_effects")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.literal("  ").append(Text.translatable("effect.minecraft.regeneration"))
                .append(Text.literal(" V (1:00)")).formatted(Formatting.BLUE));
        tooltip.add(Text.literal("  ").append(Text.translatable("effect.minecraft.resistance"))
                .append(Text.literal(" III (1:00)")).formatted(Formatting.BLUE));
        tooltip.add(Text.literal("  ").append(Text.translatable("effect.minecraft.strength"))
                .append(Text.literal(" V (1:00)")).formatted(Formatting.BLUE));

        // 投掷效果
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.throw_effects")
                .formatted(Formatting.RED));
        tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.throw_range")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.throw_hostile")
                .formatted(Formatting.DARK_RED));
        tooltip.add(Text.translatable("item.hello-mod.ultimate_enchanted_golden_apple.tooltip.throw_friendly")
                .formatted(Formatting.DARK_GREEN));
    }

    // ===== 使用行为 =====

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isThrowMode(stack)) {
            return handleThrow(world, user, hand, stack);
        } else {
            // 食用模式
            if (user.canConsume(false) || this.getFoodComponent() != null && this.getFoodComponent().isAlwaysEdible()) {
                user.setCurrentHand(hand);
                return TypedActionResult.consume(stack);
            }
            return TypedActionResult.fail(stack);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return EAT_DURATION;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        if (isThrowMode(stack)) {
            return UseAction.NONE;
        }
        return UseAction.EAT;
    }

    /**
     * 食用完成后给予固定效果。
     * 消耗逻辑由 UnbreakingFoodMixin 处理（耐久判定成功 → 不消耗 + 3s冷却）。
     * 食用附魔效果（锋利/击退/火焰附加/冰霜行者）由 PlayerEatFoodMixin 处理。
     */
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof PlayerEntity player) {
            // 给予固定效果
            applyEatEffects(player);

            HelloMod.LOGGER.info("[UltimateApple] Player {} ate ultimate enchanted golden apple", player.getName().getString());
        }

        // 调用父类处理食物消耗（会触发 player.eatFood -> UnbreakingFoodMixin 和 PlayerEatFoodMixin）
        return super.finishUsing(stack, world, user);
    }

    // ===== 投掷逻辑 =====

    private TypedActionResult<ItemStack> handleThrow(World world, PlayerEntity user, Hand hand, ItemStack stack) {
        int unbreakingLevel = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
        int infinityLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, stack);
        int swiftThrowLevel = EnchantmentHelper.getLevel(ModEnchantments.SWIFT_THROW, stack);
        int multishotLevel = EnchantmentHelper.getLevel(Enchantments.MULTISHOT, stack);
        int quickChargeLevel = EnchantmentHelper.getLevel(Enchantments.QUICK_CHARGE, stack);
        int loyaltyLevel = EnchantmentHelper.getLevel(Enchantments.LOYALTY, stack);

        // 确保带无限附魔的物品有 InfinityMarked 标记
        if (infinityLevel > 0 && !InfinityCooldownManager.isInfinityMarked(stack)) {
            InfinityCooldownManager.markInfinity(stack);
        }

        // 冷却检查
        if (!world.isClient() && InfinityCooldownManager.isInfinityMarked(stack) && InfinityCooldownManager.isOnCooldown(user)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient()) {
            // 投掷主体
            spawnAppleEntity(world, user, stack, swiftThrowLevel);

            // 多重射击
            if (multishotLevel > 0) {
                spawnMultishotEntities(world, user, stack, swiftThrowLevel, multishotLevel);
            }
        }

        // 音效
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.PLAYERS,
                0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));

        user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));

        // 消耗逻辑（仅服务端）
        if (!world.isClient() && !user.getAbilities().creativeMode) {
            handleThrowConsumption(user, stack, unbreakingLevel, infinityLevel, quickChargeLevel, loyaltyLevel);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    private void spawnAppleEntity(World world, PlayerEntity user, ItemStack stack, int swiftThrowLevel) {
        UltimateGoldenAppleEntity entity = new UltimateGoldenAppleEntity(world, user);
        entity.setItem(stack.copy());

        // 传递附魔数据到实体 NBT
        writeEnchantDataToEntity(entity, stack);

        float baseSpeed = 0.5f;
        float actualSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(swiftThrowLevel);

        float pitchOffsetDeg = 80.0f / (4.0f + swiftThrowLevel);
        float pitchOffset = pitchOffsetDeg < 1.0f ? 0.0f : -pitchOffsetDeg;

        if (swiftThrowLevel > 20) {
            // 射线追踪模式
            float safeLaunchSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(20);
            float adjustedPitch = user.getPitch();
            Vec3d direction = calculateDirection(user.getYaw(), adjustedPitch);

            entity.setVelocity(direction.multiply(safeLaunchSpeed));
            entity.setPosition(
                    entity.getX() + direction.x * 1.5,
                    entity.getY() + direction.y * 1.5,
                    entity.getZ() + direction.z * 1.5
            );

            NbtCompound nbt = entity.getAppleNbt();
            nbt.putBoolean("SwiftThrowRaycast", true);
            nbt.putFloat("SwiftThrowSpeed", actualSpeed);
            nbt.putDouble("SwiftThrowDirX", direction.x);
            nbt.putDouble("SwiftThrowDirY", direction.y);
            nbt.putDouble("SwiftThrowDirZ", direction.z);
            entity.setAppleNbt(nbt);

            entity.setInvisible(true);
            entity.setNoGravity(true);

            // 粒子弹道
            if (world instanceof ServerWorld serverWorld) {
                Vec3d particleStart = entity.getPos();
                double particleRange = Math.min(actualSpeed * 2, 64.0);
                for (double d = 0; d < particleRange; d += 0.5) {
                    serverWorld.spawnParticles(ParticleTypes.CRIT,
                            particleStart.x + direction.x * d,
                            particleStart.y + direction.y * d,
                            particleStart.z + direction.z * d,
                            1, 0, 0, 0, 0);
                }
            }
        } else if (swiftThrowLevel > 0) {
            float adjustedPitch = user.getPitch() + pitchOffset;
            Vec3d direction = calculateDirection(user.getYaw(), adjustedPitch).multiply(actualSpeed);
            entity.setVelocity(direction);
            Vec3d normalizedDir = direction.normalize();
            entity.setPosition(
                    entity.getX() + normalizedDir.x * 1.0,
                    entity.getY() + normalizedDir.y * 1.0,
                    entity.getZ() + normalizedDir.z * 1.0
            );
        } else {
            entity.setVelocity(user, user.getPitch(), user.getYaw(), -20.0f, baseSpeed, 1.0f);
        }

        world.spawnEntity(entity);
    }

    private void spawnMultishotEntities(World world, PlayerEntity user, ItemStack stack, int swiftThrowLevel, int multishotLevel) {
        int extraCount = multishotLevel + 1;
        int circleCount = Math.min(extraCount, 8);

        float baseSpeed = 0.5f;
        float actualSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(swiftThrowLevel);
        float pitchOffsetDeg = 80.0f / (4.0f + swiftThrowLevel);
        float pitchOffset = pitchOffsetDeg < 1.0f ? 0.0f : -pitchOffsetDeg;

        float basePitch = user.getPitch() + pitchOffset;
        Vec3d forward = calculateDirection(user.getYaw(), basePitch);

        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d right;
        if (Math.abs(forward.dotProduct(worldUp)) > 0.99) {
            right = forward.crossProduct(new Vec3d(0, 0, 1)).normalize();
        } else {
            right = forward.crossProduct(worldUp).normalize();
        }
        Vec3d up = right.crossProduct(forward).normalize();

        float coneAngleRad = 10.0f * ((float) Math.PI / 180.0f);

        for (int i = 0; i < extraCount; i++) {
            UltimateGoldenAppleEntity extraEntity = new UltimateGoldenAppleEntity(world, user);
            extraEntity.setItem(stack.copy());
            writeEnchantDataToEntity(extraEntity, stack);

            Vec3d extraDirection;
            if (i < circleCount) {
                double circleAngle = (2.0 * Math.PI * i) / circleCount;
                double sinCone = Math.sin(coneAngleRad);
                double cosCone = Math.cos(coneAngleRad);
                extraDirection = forward.multiply(cosCone)
                        .add(right.multiply(sinCone * Math.cos(circleAngle)))
                        .add(up.multiply(sinCone * Math.sin(circleAngle)))
                        .normalize();
            } else {
                double randomAngle = user.getRandom().nextDouble() * 2.0 * Math.PI;
                double randomRadius = Math.sqrt(user.getRandom().nextDouble()) * coneAngleRad;
                extraDirection = forward.multiply(Math.cos(randomRadius))
                        .add(right.multiply(Math.sin(randomRadius) * Math.cos(randomAngle)))
                        .add(up.multiply(Math.sin(randomRadius) * Math.sin(randomAngle)))
                        .normalize();
            }

            if (swiftThrowLevel > 20) {
                float safeLaunchSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(20);
                extraEntity.setVelocity(extraDirection.multiply(safeLaunchSpeed));
                extraEntity.setPosition(
                        extraEntity.getX() + extraDirection.x * 1.5,
                        extraEntity.getY() + extraDirection.y * 1.5,
                        extraEntity.getZ() + extraDirection.z * 1.5
                );
                NbtCompound nbt = extraEntity.getAppleNbt();
                nbt.putBoolean("SwiftThrowRaycast", true);
                nbt.putFloat("SwiftThrowSpeed", actualSpeed);
                nbt.putDouble("SwiftThrowDirX", extraDirection.x);
                nbt.putDouble("SwiftThrowDirY", extraDirection.y);
                nbt.putDouble("SwiftThrowDirZ", extraDirection.z);
                extraEntity.setAppleNbt(nbt);
                extraEntity.setInvisible(true);
                extraEntity.setNoGravity(true);
            } else if (swiftThrowLevel > 0) {
                extraEntity.setVelocity(extraDirection.multiply(actualSpeed));
                extraEntity.setPosition(
                        extraEntity.getX() + extraDirection.x * 1.0,
                        extraEntity.getY() + extraDirection.y * 1.0,
                        extraEntity.getZ() + extraDirection.z * 1.0
                );
            } else {
                double horizLen = Math.sqrt(extraDirection.x * extraDirection.x + extraDirection.z * extraDirection.z);
                float extraYaw = (float) Math.toDegrees(Math.atan2(-extraDirection.x, extraDirection.z));
                float extraPitch = (float) Math.toDegrees(Math.atan2(-extraDirection.y, horizLen));
                extraEntity.setVelocity(user, extraPitch, extraYaw, -20.0f, baseSpeed, 1.0f);
            }

            world.spawnEntity(extraEntity);
        }
    }

    private void writeEnchantDataToEntity(UltimateGoldenAppleEntity entity, ItemStack stack) {
        NbtCompound nbt = entity.getAppleNbt();
        int sharpness = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
        int power = EnchantmentHelper.getLevel(Enchantments.POWER, stack);
        int punch = EnchantmentHelper.getLevel(Enchantments.PUNCH, stack);
        int flame = EnchantmentHelper.getLevel(Enchantments.FLAME, stack);
        int channeling = EnchantmentHelper.getLevel(Enchantments.CHANNELING, stack);
        int piercing = EnchantmentHelper.getLevel(Enchantments.PIERCING, stack);
        int loyalty = EnchantmentHelper.getLevel(Enchantments.LOYALTY, stack);

        if (sharpness > 0) nbt.putInt("SharpnessLevel", sharpness);
        if (power > 0) nbt.putInt("PowerLevel", power);
        if (punch > 0) nbt.putInt("PunchLevel", punch);
        if (flame > 0) nbt.putInt("FlameLevel", flame);
        if (channeling > 0) nbt.putInt("ChannelingLevel", channeling);
        if (piercing > 0) nbt.putInt("PiercingLevel", piercing);
        if (loyalty > 0) nbt.putInt("LoyaltyLevel", loyalty);

        entity.setAppleNbt(nbt);
    }

    private void handleThrowConsumption(PlayerEntity user, ItemStack stack, int unbreakingLevel, int infinityLevel, int quickChargeLevel, int loyaltyLevel) {
        if (loyaltyLevel > 0) {
            stack.decrement(1);
        } else if (infinityLevel > 0) {
            int cooldownTicks = InfinityCooldownManager.getReducedCooldown(quickChargeLevel);
            if (unbreakingLevel > 0) {
                if (user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
                    // 耐久判定成功，免冷却
                } else {
                    if (cooldownTicks > 0) {
                        InfinityCooldownManager.triggerCooldown(user, cooldownTicks);
                    }
                }
            } else {
                if (cooldownTicks > 0) {
                    InfinityCooldownManager.triggerCooldown(user, cooldownTicks);
                }
            }
        } else if (unbreakingLevel > 0 && user.getRandom().nextInt(unbreakingLevel + 1) > 0) {
            // 耐久触发，不消耗
        } else {
            stack.decrement(1);
        }
    }

    // ===== 食用效果 =====

    private void applyEatEffects(PlayerEntity player) {
        // Regeneration V (60s = 1200 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 4));
        // Resistance III (60s = 1200 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1200, 2));
        // Strength V (60s = 1200 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 4));
    }

    // ===== 工具方法 =====

    private Vec3d calculateDirection(float yaw, float pitch) {
        float yawRad = yaw * ((float) Math.PI / 180.0f);
        float pitchRad = pitch * ((float) Math.PI / 180.0f);
        double vx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double vy = -Math.sin(pitchRad);
        double vz = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3d(vx, vy, vz).normalize();
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * 创建一个自带默认附魔的终极附魔金苹果物品栈。
     * 用于进度奖励给予时调用。
     */
    public static ItemStack createDefaultStack() {
        ItemStack stack = new ItemStack(ModItems.ULTIMATE_ENCHANTED_GOLDEN_APPLE);
        // 自带附魔：效率9、耐久10、迅投25、无限1
        stack.addEnchantment(Enchantments.EFFICIENCY, 9);
        stack.addEnchantment(Enchantments.UNBREAKING, 10);
        stack.addEnchantment(ModEnchantments.SWIFT_THROW, 25);
        stack.addEnchantment(Enchantments.INFINITY, 1);
        return stack;
    }
}
