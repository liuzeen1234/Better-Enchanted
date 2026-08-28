package com.example.hellomod;

import com.example.hellomod.block.HelloModBlockEntities;
import com.example.hellomod.debug.DebugLogConfig;
import com.example.hellomod.effect.FrostWalkerFoodEffect;
import com.example.hellomod.enchantment.InfinityCooldownManager;
import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.entity.ModEntities;
import com.example.hellomod.item.ModItems;
import com.example.hellomod.network.SuperAppleModeSwitchC2SPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloMod implements ModInitializer {

    public static final String MOD_ID = "hello-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger("BetterEnchanted");

    @Override
    public void onInitialize() {
        LOGGER.info("Better Enchanted initialized!");

        // 加载调试日志配置
        DebugLogConfig.load();

        // 注册自定义附魔
        ModEnchantments.register();

        // 注册自定义物品
        ModItems.register();

        // 注册自定义实体
        ModEntities.register();

        // 注册方块实体
        HelloModBlockEntities.register();

        // 注册冰霜行者食物效果的tick事件
        FrostWalkerFoodEffect.register();

        // 注册无限附魔冷却管理器（tick事件）
        InfinityCooldownManager.register();

        // 注册网络包接收器
        SuperAppleModeSwitchC2SPacket.registerServerReceiver();

        // 玩家断开连接时清理无限冷却数据
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            InfinityCooldownManager.onPlayerDisconnect(handler.getPlayer().getUuid());
        });

        // 可选：如果 debug-menu mod 已安装，注册调试开关
        if (FabricLoader.getInstance().isModLoaded("debug-menu")) {
            DebugToggleRegistration.register();
        }
    }
}
