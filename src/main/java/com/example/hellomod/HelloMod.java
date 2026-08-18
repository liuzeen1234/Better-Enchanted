package com.example.hellomod;

import com.example.hellomod.block.HelloModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloMod implements ModInitializer {

    public static final String MOD_ID = "hello-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Mod initialized!");

        // 注册方块实体
        HelloModBlockEntities.register();

        // 当玩家加入世界时，在聊天框输出 "hello"
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.getPlayer().sendMessage(Text.literal("hello"), false);
        });
    }
}
