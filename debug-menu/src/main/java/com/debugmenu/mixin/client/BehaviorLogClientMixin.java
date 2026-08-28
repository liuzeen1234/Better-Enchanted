package com.debugmenu.mixin.client;

import com.debugmenu.api.DebugMenuApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.Nullable;

/**
 * 客户端行为日志 Mixin - 跟踪界面打开/关闭。
 */
@Mixin(MinecraftClient.class)
public abstract class BehaviorLogClientMixin {

    @Unique
    private static final Logger debug_menu$LOGGER = LoggerFactory.getLogger("DebugMenu");

    @Unique
    private static final String debug_menu$BEHAVIOR_LOG_KEY = "behavior_log";

    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void debug_menu$onSetScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (!DebugMenuApi.isEnabled(debug_menu$BEHAVIOR_LOG_KEY)) return;

        String prevScreenName = currentScreen != null ? debug_menu$getScreenName(currentScreen) : "无";
        String newScreenName = screen != null ? debug_menu$getScreenName(screen) : "无(游戏内)";

        if (currentScreen == null && screen == null) return;

        if (screen != null && currentScreen == null) {
            debug_menu$LOGGER.info("[BehaviorLog] 打开界面: {}", newScreenName);
        } else if (screen == null && currentScreen != null) {
            debug_menu$LOGGER.info("[BehaviorLog] 关闭界面: {}", prevScreenName);
        } else {
            debug_menu$LOGGER.info("[BehaviorLog] 切换界面: {} -> {}", prevScreenName, newScreenName);
        }
    }

    @Unique
    private String debug_menu$getScreenName(Screen screen) {
        String title = screen.getTitle().getString();
        String className = screen.getClass().getSimpleName();

        if (screen instanceof InventoryScreen) return "背包";
        if (screen instanceof CraftingScreen) return "工作台";
        if (screen instanceof GenericContainerScreen) return "箱子: " + title;
        if (screen instanceof FurnaceScreen) return "熔炉";
        if (screen instanceof AnvilScreen) return "铁砧";
        if (screen instanceof EnchantmentScreen) return "附魔台";
        if (screen instanceof SmithingScreen) return "锻造台";
        if (screen instanceof MerchantScreen) return "交易";
        if (screen instanceof CreativeInventoryScreen) return "创造模式物品栏";
        if (screen instanceof BookScreen) return "书";

        if (!title.isEmpty()) {
            return title + " (" + className + ")";
        }
        return className;
    }
}
