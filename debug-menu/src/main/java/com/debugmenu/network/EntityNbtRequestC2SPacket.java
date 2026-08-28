package com.debugmenu.network;

import com.debugmenu.DebugMenuMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * 客户端->服务端 网络包：请求实体的完整 NBT 数据。
 * 客户端发送目标实体的 ID，服务端读取完整 NBT（包括药水效果）后通过 S2C 包返回。
 */
public class EntityNbtRequestC2SPacket {

    public static final Identifier CHANNEL = new Identifier(DebugMenuMod.MOD_ID, "entity_nbt_request");

    /**
     * 客户端发送请求，指定实体 ID。
     */
    public static void send(int entityId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityId);
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    /**
     * 在服务端注册接收器。
     */
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, responseSender) -> {
            int entityId = buf.readInt();
            server.execute(() -> {
                handleRequest(player, entityId);
            });
        });
    }

    private static void handleRequest(ServerPlayerEntity player, int entityId) {
        Entity entity = player.getServerWorld().getEntityById(entityId);
        if (entity == null) return;

        // 限制距离防止滥用
        if (player.squaredDistanceTo(entity) > 256 * 256) return;

        // 获取完整 NBT
        NbtCompound nbt = new NbtCompound();
        entity.writeNbt(nbt);

        // 补充药水效果
        if (entity instanceof LivingEntity living) {
            Collection<StatusEffectInstance> effects = living.getStatusEffects();
            NbtList effectList = new NbtList();
            for (StatusEffectInstance effect : effects) {
                NbtCompound effectNbt = new NbtCompound();
                String effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType()).toString();
                effectNbt.putString("Id", effectId);
                effectNbt.putInt("Amplifier", effect.getAmplifier());
                effectNbt.putInt("Duration", effect.getDuration());
                effectList.add(effectNbt);
            }
            nbt.put("ActiveEffects", effectList);
        }

        // 发送响应回客户端
        EntityNbtResponseS2CPacket.send(player, entityId, nbt);
    }
}
