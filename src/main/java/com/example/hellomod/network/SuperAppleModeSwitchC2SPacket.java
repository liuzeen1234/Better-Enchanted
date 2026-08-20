package com.example.hellomod.network;

import com.example.hellomod.HelloMod;
import com.example.hellomod.item.ModItems;
import com.example.hellomod.item.SuperEnchantedGoldenAppleItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 客户端->服务端 网络包：超级附魔金苹果模式切换。
 * 
 * 客户端在左键切换模式时发送此包，服务端收到后同步更新物品 NBT。
 */
public class SuperAppleModeSwitchC2SPacket {

    public static final Identifier CHANNEL = new Identifier(HelloMod.MOD_ID, "super_apple_mode_switch");

    /**
     * 客户端发送切换包。
     */
    public static void send() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(CHANNEL, buf);
    }

    /**
     * 在服务端注册接收器。
     */
    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                handleModeSwitch(player);
            });
        });
    }

    private static void handleModeSwitch(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.isEmpty() || mainHand.getItem() != ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        SuperEnchantedGoldenAppleItem.toggleMode(mainHand);
        HelloMod.LOGGER.info("[SuperApple] Player {} switched mode to: {}",
                player.getName().getString(), SuperEnchantedGoldenAppleItem.getMode(mainHand));
    }
}
