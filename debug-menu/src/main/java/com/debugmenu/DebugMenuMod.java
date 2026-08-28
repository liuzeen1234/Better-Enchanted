package com.debugmenu;

import com.debugmenu.network.EntityNbtRequestC2SPacket;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 调试菜单 Mod 服务端入口。
 * 注册网络包接收器（实体 NBT 查询）。
 */
public class DebugMenuMod implements ModInitializer {

    public static final String MOD_ID = "debug-menu";
    public static final Logger LOGGER = LoggerFactory.getLogger("DebugMenu");

    @Override
    public void onInitialize() {
        LOGGER.info("[DebugMenu] Debug Menu Mod initialized!");

        // 注册服务端网络包接收器
        EntityNbtRequestC2SPacket.registerServerReceiver();
    }
}
