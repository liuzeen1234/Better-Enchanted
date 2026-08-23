package com.example.hellomod.network;

import com.example.hellomod.HelloMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 服务端->客户端 网络包：返回实体的完整 NBT 数据。
 */
public class EntityNbtResponseS2CPacket {

    public static final Identifier CHANNEL = new Identifier(HelloMod.MOD_ID, "entity_nbt_response");

    /**
     * 服务端发送响应给指定玩家。
     */
    public static void send(ServerPlayerEntity player, int entityId, NbtCompound nbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        buf.writeNbt(nbt);
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }

    /**
     * 在客户端注册接收器。
     */
    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            NbtCompound nbt = buf.readNbt();
            client.execute(() -> {
                EntityNbtCache.put(entityId, nbt);
            });
        });
    }
}
