package com.example.hellomod.item;

import com.example.hellomod.HelloMod;
import com.example.hellomod.debug.DebugLogConfig;
import com.example.hellomod.enchantment.InfinityCooldownManager;
import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.enchantment.SwiftThrowEnchantment;
import com.example.hellomod.entity.SuperGoldenAppleEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 超级附魔金苹果物品。
 *
 * 功能：
 * - 双模式切换（食用/投掷），通过左键切换（客户端 mixin 处理）
 * - 食用模式：长按右键食用，给予 Regeneration V (30s), Absorption IV (2min),
 *   Resistance I (5min), Fire Resistance I (5min)
 * - 投掷模式：右键投掷，落地后喷溅 + 效果云
 * - 自带迅投 255 附魔
 * - 可附加多种附魔（见设计文档）
 */
public class SuperEnchantedGoldenAppleItem extends Item {

    /** NBT key 用于存储当前模式 */
    public static final String MODE_KEY = "SuperAppleMode";
    /** 模式值：食用 */
    public static final String MODE_EAT = "eat";
    /** 模式值：投掷 */
    public static final String MODE_THROW = "throw";

    /** 食用时间（32 tick = 1.6s，与原版附魔金苹果相同） */
    private static final int EAT_DURATION = 32;

    private static final FoodComponent FOOD_COMPONENT = new FoodComponent.Builder()
            .alwaysEdible()
            .hunger(4)
            .saturationModifier(1.2f)
            .build();

    public SuperEnchantedGoldenAppleItem(Settings settings) {
        super(settings.food(FOOD_COMPONENT));
    }

    // ===== 模式管理 =====

    /**
     * 获取当前模式。
     */
    public static String getMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(MODE_KEY)) {
            return nbt.getString(MODE_KEY);
        }
        return MODE_EAT; // 默认食用模式
    }

    /**
     * 设置模式。
     */
    public static void setMode(ItemStack stack, String mode) {
        stack.getOrCreateNbt().putString(MODE_KEY, mode);
    }

    /**
     * 切换模式。
     */
    public static void toggleMode(ItemStack stack) {
        String current = getMode(stack);
        if (MODE_EAT.equals(current)) {
            setMode(stack, MODE_THROW);
        } else {
            setMode(stack, MODE_EAT);
        }
    }

    /**
     * 是否为投掷模式。
     */
    public static boolean isThrowMode(ItemStack stack) {
        return MODE_THROW.equals(getMode(stack));
    }

    // ===== 物品名称动态变化 =====

    @Override
    public Text getName(ItemStack stack) {
        if (isThrowMode(stack)) {
            return Text.translatable("item.hello-mod.super_enchanted_golden_apple.throw")
                    .formatted(Formatting.LIGHT_PURPLE);
        }
        return Text.translatable("item.hello-mod.super_enchanted_golden_apple")
                .formatted(Formatting.LIGHT_PURPLE);
    }

    // ===== 工具提示 =====

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        // 显示当前模式
        if (isThrowMode(stack)) {
            tooltip.add(Text.translatable("item.hello-mod.super_enchanted_golden_apple.tooltip.mode_throw")
                    .formatted(Formatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Text.translatable("item.hello-mod.super_enchanted_golden_apple.tooltip.mode_eat")
                    .formatted(Formatting.GREEN));
        }

        // 操作提示
        tooltip.add(Text.translatable("item.hello-mod.super_enchanted_golden_apple.tooltip.switch")
                .formatted(Formatting.DARK_GRAY));

        // 显示药水效果信息
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return;

        // 喷溅效果
        if (nbt.contains("SplashEffects") || nbt.contains("SplashInstantCount")) {
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("item.hello-mod.super_enchanted_golden_apple.tooltip.splash_effects")
                    .formatted(Formatting.AQUA));
            appendEffectsToTooltip(tooltip, nbt, "SplashEffects", "SplashInstantCount");
        }

        // 效果云
        if (nbt.contains("CloudEffects") || nbt.contains("CloudInstantCount")) {
            tooltip.add(Text.empty());
            tooltip.add(Text.translatable("item.hello-mod.super_enchanted_golden_apple.tooltip.cloud_effects")
                    .formatted(Formatting.DARK_AQUA));
            appendEffectsToTooltip(tooltip, nbt, "CloudEffects", "CloudInstantCount");
        }
    }

    /**
     * 将存储的药水效果添加到 Tooltip 中。
     */
    private void appendEffectsToTooltip(List<Text> tooltip, NbtCompound nbt, String effectsKey, String instantKey) {
        // 持续性效果
        if (nbt.contains(effectsKey)) {
            NbtList effects = nbt.getList(effectsKey, 10); // 10 = NbtCompound type
            for (int i = 0; i < effects.size(); i++) {
                NbtCompound effectNbt = effects.getCompound(i);
                String id = effectNbt.getString("Id");
                int amplifier = effectNbt.getInt("Amplifier");
                int duration = effectNbt.getInt("Duration");

                StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(id));
                if (effect == null) continue;

                String effectName = Text.translatable(effect.getTranslationKey()).getString();
                String levelStr = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
                String timeStr = formatDuration(duration);

                tooltip.add(Text.literal("  " + effectName + levelStr + " (" + timeStr + ")")
                        .formatted(Formatting.BLUE));
            }
        }

        // 瞬时效果
        if (nbt.contains(instantKey)) {
            NbtList instants = nbt.getList(instantKey, 10);
            for (int i = 0; i < instants.size(); i++) {
                NbtCompound icNbt = instants.getCompound(i);
                String id = icNbt.getString("Id");
                int amplifier = icNbt.getInt("Amplifier");

                StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(id));
                if (effect == null) continue;

                String effectName = Text.translatable(effect.getTranslationKey()).getString();
                String levelStr = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";

                tooltip.add(Text.literal("  " + effectName + levelStr)
                        .formatted(Formatting.BLUE));
            }
        }
    }

    /**
     * 将 tick 数转为 "分:秒" 格式。
     */
    private String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 将数字转为罗马数字（简单实现，1-10）。
     */
    private String toRoman(int num) {
        switch (num) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(num);
        }
    }

    // ===== 使用行为 =====

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (isThrowMode(stack)) {
            // 投掷模式
            return handleThrow(world, user, hand, stack);
        } else {
            // 食用模式 — 让原版食物逻辑处理（开始食用动画）
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
     * 食用完成后给予效果。
     * 注意：锋利/击退/火焰附加/冰霜行者效果由 PlayerEatFoodMixin 和 UnbreakingFoodMixin 处理。
     */
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof PlayerEntity player) {
            // 给予基础效果
            applyEatEffects(player);

            // 给予存储的药水额外效果（喷溅+滞留，100%持续时间）
            applyStoredPotionEffects(player, stack);

            // 消耗逻辑
            if (!player.getAbilities().creativeMode) {
                int unbreakingLevel = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
                // UnbreakingFoodMixin 会处理耐久附魔的不消耗逻辑
                // 这里只负责默认消耗
            }

            HelloMod.LOGGER.info("[SuperApple] Player {} ate super enchanted golden apple", player.getName().getString());
        }

        // 调用父类处理基本食物消耗（会触发 player.eatFood -> UnbreakingFoodMixin）
        return super.finishUsing(stack, world, user);
    }

    // ===== 投掷逻辑 =====

    private TypedActionResult<ItemStack> handleThrow(World world, PlayerEntity user, Hand hand, ItemStack stack) {
        // 读取附魔
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
        SuperGoldenAppleEntity entity = new SuperGoldenAppleEntity(world, user);
        entity.setItem(stack.copy());

        // 传递附魔数据到实体 NBT
        writeEnchantDataToEntity(entity, stack);

        float baseSpeed = 0.5f;
        float actualSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(swiftThrowLevel);

        // 角度偏移（与 PotionItemMixin 相同公式）
        float pitchOffsetDeg = 80.0f / (4.0f + swiftThrowLevel);
        float pitchOffset = pitchOffsetDeg < 1.0f ? 0.0f : -pitchOffsetDeg;

        if (swiftThrowLevel > 20) {
            // 射线追踪模式 — 直线命中无重力，不需要角度补偿
            float safeLaunchSpeed = baseSpeed * SwiftThrowEnchantment.getSpeedMultiplier(20);
            float adjustedPitch = user.getPitch();
            Vec3d direction = calculateDirection(user.getYaw(), adjustedPitch);

            entity.setVelocity(direction.multiply(safeLaunchSpeed));
            entity.setPosition(
                    entity.getX() + direction.x * 1.5,
                    entity.getY() + direction.y * 1.5,
                    entity.getZ() + direction.z * 1.5
            );

            // 写入射线追踪 NBT
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
            SuperGoldenAppleEntity extraEntity = new SuperGoldenAppleEntity(world, user);
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

    private void writeEnchantDataToEntity(SuperGoldenAppleEntity entity, ItemStack stack) {
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

        // 传递药水效果 NBT 数据
        NbtCompound itemNbt = stack.getNbt();
        if (itemNbt != null) {
            if (itemNbt.contains("SplashEffects")) {
                nbt.put("SplashEffects", itemNbt.getList("SplashEffects", 10).copy());
            }
            if (itemNbt.contains("SplashInstantCount")) {
                nbt.put("SplashInstantCount", itemNbt.getList("SplashInstantCount", 10).copy());
            }
            if (itemNbt.contains("CloudEffects")) {
                nbt.put("CloudEffects", itemNbt.getList("CloudEffects", 10).copy());
            }
            if (itemNbt.contains("CloudInstantCount")) {
                nbt.put("CloudInstantCount", itemNbt.getList("CloudInstantCount", 10).copy());
            }
        }

        entity.setAppleNbt(nbt);
    }

    private void handleThrowConsumption(PlayerEntity user, ItemStack stack, int unbreakingLevel, int infinityLevel, int quickChargeLevel, int loyaltyLevel) {
        if (loyaltyLevel > 0) {
            // 忠诚：投掷时消耗，返回时归还
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
        // 基础效果（固定）
        // Regeneration V (30s = 600 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 4));
        // Absorption IV (2min = 2400 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3));
        // Resistance I (5min = 6000 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
        // Fire Resistance I (5min = 6000 ticks)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
    }

    /**
     * 食用时额外应用存储的药水效果。
     * 食用效果 = 基础效果 + 喷溅型药水效果 + 滞留型药水效果（全部100%持续时间给予食用者）
     */
    private void applyStoredPotionEffects(PlayerEntity player, ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return;

        // 应用喷溅型药水的持续性效果
        applyStoredDurationEffects(player, nbt, "SplashEffects");
        // 应用滞留型药水的持续性效果
        applyStoredDurationEffects(player, nbt, "CloudEffects");

        // 触发喷溅型药水的瞬时效果
        applyStoredInstantEffects(player, nbt, "SplashInstantCount");
        // 触发滞留型药水的瞬时效果
        applyStoredInstantEffects(player, nbt, "CloudInstantCount");
    }

    /**
     * 应用存储的持续性效果。
     */
    private void applyStoredDurationEffects(PlayerEntity player, NbtCompound nbt, String key) {
        if (!nbt.contains(key)) return;
        NbtList effects = nbt.getList(key, 10);
        for (int i = 0; i < effects.size(); i++) {
            NbtCompound effectNbt = effects.getCompound(i);
            String id = effectNbt.getString("Id");
            int amplifier = effectNbt.getInt("Amplifier");
            int duration = effectNbt.getInt("Duration");

            StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(id));
            if (effect == null) continue;

            player.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));
        }
    }

    /**
     * 触发存储的瞬时效果（每瓶各触发一次）。
     */
    private void applyStoredInstantEffects(PlayerEntity player, NbtCompound nbt, String key) {
        if (!nbt.contains(key)) return;
        NbtList instants = nbt.getList(key, 10);
        for (int i = 0; i < instants.size(); i++) {
            NbtCompound icNbt = instants.getCompound(i);
            String id = icNbt.getString("Id");
            int amplifier = icNbt.getInt("Amplifier");
            int count = icNbt.getInt("Count");

            StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(id));
            if (effect == null) continue;

            // 每次触发一次瞬时效果
            for (int c = 0; c < count; c++) {
                effect.applyInstantEffect(player, player, player, amplifier, 1.0);
            }
        }
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
        return true; // 始终显示附魔光效
    }
}
