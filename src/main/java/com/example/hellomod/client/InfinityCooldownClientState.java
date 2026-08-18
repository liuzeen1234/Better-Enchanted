package com.example.hellomod.client;

import com.example.hellomod.enchantment.InfinityCooldownManager;
import com.example.hellomod.enchantment.InfinityCooldownSync;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;

/**
 * 客户端侧的无限附魔冷却状态。
 *
 * 由服务端通过网络包同步，客户端用于：
 * 1. 渲染冷却动画覆盖（灰色遮罩）
 * 2. 显示冷却进度
 */
public class InfinityCooldownClientState {

    /** 客户端剩余冷却 ticks */
    private static int remainingTicks = 0;

    /** 客户端总冷却 ticks（用于计算比例） */
    private static int totalTicks = 0;

    /**
     * 注册客户端网络包接收器和 tick 事件。
     */
    public static void register() {
        // 注册接收服务端同步包
        ClientPlayNetworking.registerGlobalReceiver(InfinityCooldownSync.COOLDOWN_SYNC_PACKET_ID,
                (client, handler, buf, responseSender) -> {
                    int remaining = buf.readInt();
                    int total = buf.readInt();
                    // 切换到客户端线程执行
                    client.execute(() -> {
                        remainingTicks = remaining;
                        totalTicks = total;
                    });
                });

        // 每 tick 递减客户端冷却计时器
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        });
    }

    /**
     * 客户端是否处于无限附魔冷却中。
     */
    public static boolean isOnCooldown() {
        return remainingTicks > 0;
    }

    /**
     * 获取冷却进度（0.0 ~ 1.0），0 = 冷却结束，1 = 刚开始冷却。
     */
    public static float getCooldownProgress() {
        if (totalTicks <= 0 || remainingTicks <= 0) return 0.0f;
        return (float) remainingTicks / (float) totalTicks;
    }

    /**
     * 获取剩余冷却 ticks。
     */
    public static int getRemainingTicks() {
        return remainingTicks;
    }

    /**
     * 检查某个物品是否应该显示冷却覆盖。
     * 条件：物品带有 InfinityMarked 标记 且 客户端正在冷却中。
     */
    public static boolean shouldShowCooldown(ItemStack stack) {
        return isOnCooldown() && InfinityCooldownManager.isInfinityMarked(stack);
    }
}
