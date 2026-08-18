package com.example.hellomod.enchantment;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 无限附魔冷却同步 — 服务端发包通知客户端冷却状态。
 *
 * 包内容：
 * - int: 剩余冷却 ticks（0 表示冷却结束）
 * - int: 总冷却 ticks（用于计算进度比例）
 */
public class InfinityCooldownSync {

    public static final Identifier COOLDOWN_SYNC_PACKET_ID =
            new Identifier("hello-mod", "infinity_cooldown_sync");

    /**
     * 从服务端发送冷却同步包到客户端。
     */
    public static void sendCooldownToClient(ServerPlayerEntity player, int remainingTicks, int totalTicks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(remainingTicks);
        buf.writeInt(totalTicks);
        ServerPlayNetworking.send(player, COOLDOWN_SYNC_PACKET_ID, buf);
    }
}
