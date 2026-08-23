package com.example.hellomod.mixin.client;

import com.example.hellomod.debug.DebugLogConfig;
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
 * 注入 MinecraftClient.setScreen() 来捕获所有界面变化。
 */
@Mixin(MinecraftClient.class)
public abstract class BehaviorLogClientMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterEnchanted");

    @Shadow
    @Nullable
    public Screen currentScreen;

    /**
     * 界面切换时记录：打开/关闭了哪个界面
     */
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (!DebugLogConfig.isPlayerBehaviorLogEnabled()) return;

        String prevScreenName = currentScreen != null ? hello_mod$getScreenName(currentScreen) : "无";
        String newScreenName = screen != null ? hello_mod$getScreenName(screen) : "无(游戏内)";

        if (currentScreen == null && screen == null) return;

        if (screen != null && currentScreen == null) {
            LOGGER.info("[BehaviorLog] 打开界面: {}", newScreenName);
        } else if (screen == null && currentScreen != null) {
            LOGGER.info("[BehaviorLog] 关闭界面: {}", prevScreenName);
        } else {
            LOGGER.info("[BehaviorLog] 切换界面: {} -> {}", prevScreenName, newScreenName);
        }
    }

    @Unique
    private String hello_mod$getScreenName(Screen screen) {
        // 优先使用界面标题
        String title = screen.getTitle().getString();
        String className = screen.getClass().getSimpleName();

        // 针对常见界面提供友好名称
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

        // 如果有标题则显示标题，否则显示类名
        if (!title.isEmpty()) {
            return title + " (" + className + ")";
        }
        return className;
    }
}
