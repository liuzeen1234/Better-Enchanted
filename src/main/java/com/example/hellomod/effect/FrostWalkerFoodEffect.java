package com.example.hellomod.effect;

import com.example.hellomod.HelloMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 冰霜行者食物附魔效果管理器。
 * 
 * 效果说明：
 * - 食用带有冰霜行者附魔的食物后，获得冰霜行者效果
 * - 基础持续时间：20s，每级额外+10s
 * - 霜冻伤害：每4s受1点伤害，每级减少0.5s的伤害间隔
 * - 脚下水面结冰（与原版冰霜行者逻辑一致）
 * - 冰霜行者的等级与食物附魔等级相同
 */
public class FrostWalkerFoodEffect {

    /**
     * 记录每个玩家的冰霜行者效果状态
     */
    private static final Map<UUID, FrostWalkerState> ACTIVE_EFFECTS = new HashMap<>();

    /**
     * 冰霜行者效果状态
     */
    private static class FrostWalkerState {
        final int level;           // 冰霜行者等级
        int remainingTicks;        // 剩余持续时间（tick）
        int damageCooldown;        // 距离下次伤害的tick数
        final int damageInterval;  // 伤害间隔（tick）

        FrostWalkerState(int level) {
            this.level = level;
            // 基础20s + 每级额外10s
            this.remainingTicks = (20 + level * 10) * 20; // 转换为tick
            // 基础4s间隔，每级减少0.5s，最少0.5s
            double intervalSeconds = Math.max(0.5, 4.0 - level * 0.5);
            this.damageInterval = (int) (intervalSeconds * 20); // 转换为tick
            this.damageCooldown = this.damageInterval;
        }
    }

    /**
     * 注册服务端tick事件
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ACTIVE_EFFECTS.isEmpty()) return;

            Iterator<Map.Entry<UUID, FrostWalkerState>> iterator = ACTIVE_EFFECTS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, FrostWalkerState> entry = iterator.next();
                UUID playerId = entry.getKey();
                FrostWalkerState state = entry.getValue();

                // 获取玩家
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player == null || player.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                // 减少剩余时间
                state.remainingTicks--;
                if (state.remainingTicks <= 0) {
                    iterator.remove();
                    // 效果结束时重置冰冻视觉
                    player.setFrozenTicks(0);
                    HelloMod.LOGGER.info("[FrostWalker] Effect expired for player {}", player.getName().getString());
                    continue;
                }

                // 保持冰冻视觉效果（蓝色血条），模拟细雪冰冻
                // 当 frozenTicks >= 140 时血条显示为蓝色冰冻样式
                // 设置为比阈值稍高的值，确保视觉持续生效
                player.setFrozenTicks(Math.max(player.getMinFreezeDamageTicks(), player.getFrozenTicks()));

                // 冰霜行者结冰效果
                freezeNearbyWater(player, state.level);

                // 伤害计时
                state.damageCooldown--;
                if (state.damageCooldown <= 0) {
                    // 造成1点霜冻伤害
                    player.damage(createFreezeDamage(player.getServerWorld()), 1.0f);
                    HelloMod.LOGGER.info("[FrostWalker] Dealt 1 freeze damage to player {} (interval: {}t, remaining: {}t)",
                            player.getName().getString(), state.damageInterval, state.remainingTicks);
                    state.damageCooldown = state.damageInterval;
                }
            }
        });
    }

    /**
     * 给玩家施加冰霜行者效果
     */
    public static void apply(PlayerEntity player, int level) {
        if (level <= 0) return;
        ACTIVE_EFFECTS.put(player.getUuid(), new FrostWalkerState(level));
        int durationSeconds = 20 + level * 10;
        double intervalSeconds = Math.max(0.5, 4.0 - level * 0.5);
        HelloMod.LOGGER.info("[FrostWalker] Applied frost walker level {} to player {} (duration: {}s, damage interval: {}s)",
                level, player.getName().getString(), durationSeconds, intervalSeconds);
    }

    /**
     * 检查玩家是否有冰霜行者效果
     */
    public static boolean hasEffect(PlayerEntity player) {
        return ACTIVE_EFFECTS.containsKey(player.getUuid());
    }

    /**
     * 冻结玩家脚下附近的水面。
     * 参考原版 FrostWalkerEnchantment.freezeWater() 的逻辑。
     */
    private static void freezeNearbyWater(ServerPlayerEntity player, int level) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();

        // 冰霜行者的影响范围 = 2 + level
        int radius = 2 + level;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // 只在圆形范围内
                if (dx * dx + dz * dz > radius * radius) continue;

                mutable.set(playerPos.getX() + dx, playerPos.getY() - 1, playerPos.getZ() + dz);

                BlockState stateAtPos = world.getBlockState(mutable);

                // 检查是否是水源方块（非流动水）
                if (stateAtPos.isOf(Blocks.WATER) && stateAtPos.get(FluidBlock.LEVEL) == 0) {
                    // 检查上方是否有空间（原版逻辑）
                    BlockState aboveState = world.getBlockState(mutable.up());
                    if (aboveState.isAir()) {
                        // 检查霜冰是否能放置
                        BlockState frostedIce = Blocks.FROSTED_ICE.getDefaultState();
                        if (frostedIce.canPlaceAt(world, mutable) &&
                                world.canPlace(frostedIce, mutable, ShapeContext.absent())) {
                            world.setBlockState(mutable, frostedIce);
                            // 安排霜冰融化（与原版一致，随机tick会自动处理）
                            world.scheduleBlockTick(mutable.toImmutable(), Blocks.FROSTED_ICE,
                                    MathHelper.nextInt(player.getRandom(), 60, 120));
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建霜冻伤害源
     */
    private static DamageSource createFreezeDamage(ServerWorld world) {
        return new DamageSource(
                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.FREEZE)
        );
    }
}
