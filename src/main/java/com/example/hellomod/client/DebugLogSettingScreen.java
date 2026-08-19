package com.example.hellomod.client;

import com.example.hellomod.debug.DebugLogConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 调试日志开关设置（二级菜单）：控制9种调试日志的输出开关。
 */
public class DebugLogSettingScreen extends Screen {

    private final Screen parent;

    public DebugLogSettingScreen(Screen parent) {
        super(Text.translatable("screen.hello-mod.debug_log_setting.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 240;
        int buttonHeight = 20;
        int gap = 22;
        int centerX = this.width / 2 - buttonWidth / 2;
        // 9个开关按钮，居中布局
        int totalHeight = 9 * gap;
        int startY = this.height / 2 - totalHeight / 2;

        // 1. [CakeDebug] 蛋糕食用调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("蛋糕食用 [CakeDebug]", DebugLogConfig.isCakeDebugEnabled()),
                button -> {
                    DebugLogConfig.toggleCakeDebug();
                    button.setMessage(getToggleText("蛋糕食用 [CakeDebug]", DebugLogConfig.isCakeDebugEnabled()));
                }
        ).dimensions(centerX, startY, buttonWidth, buttonHeight).build());

        // 2. [PlaceDebug] 蛋糕放置调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("蛋糕放置 [PlaceDebug]", DebugLogConfig.isPlaceDebugEnabled()),
                button -> {
                    DebugLogConfig.togglePlaceDebug();
                    button.setMessage(getToggleText("蛋糕放置 [PlaceDebug]", DebugLogConfig.isPlaceDebugEnabled()));
                }
        ).dimensions(centerX, startY + gap, buttonWidth, buttonHeight).build());

        // 3. [FoodDebug] 普通食物食用调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("食物食用 [FoodDebug]", DebugLogConfig.isFoodDebugEnabled()),
                button -> {
                    DebugLogConfig.toggleFoodDebug();
                    button.setMessage(getToggleText("食物食用 [FoodDebug]", DebugLogConfig.isFoodDebugEnabled()));
                }
        ).dimensions(centerX, startY + gap * 2, buttonWidth, buttonHeight).build());

        // 4. [PotionDebug] 药水投掷调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("药水投掷 [PotionDebug]", DebugLogConfig.isPotionDebugEnabled()),
                button -> {
                    DebugLogConfig.togglePotionDebug();
                    button.setMessage(getToggleText("药水投掷 [PotionDebug]", DebugLogConfig.isPotionDebugEnabled()));
                }
        ).dimensions(centerX, startY + gap * 3, buttonWidth, buttonHeight).build());

        // 5. [PotionDamage] 药水伤害调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("药水伤害 [PotionDamage]", DebugLogConfig.isPotionDamageEnabled()),
                button -> {
                    DebugLogConfig.togglePotionDamage();
                    button.setMessage(getToggleText("药水伤害 [PotionDamage]", DebugLogConfig.isPotionDamageEnabled()));
                }
        ).dimensions(centerX, startY + gap * 4, buttonWidth, buttonHeight).build());

        // 6. [SwiftThrow] 迅投附魔调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("迅投附魔 [SwiftThrow]", DebugLogConfig.isSwiftThrowEnabled()),
                button -> {
                    DebugLogConfig.toggleSwiftThrow();
                    button.setMessage(getToggleText("迅投附魔 [SwiftThrow]", DebugLogConfig.isSwiftThrowEnabled()));
                }
        ).dimensions(centerX, startY + gap * 5, buttonWidth, buttonHeight).build());

        // 7. [ClientDebug] 客户端手持物品调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("客户端手持 [ClientDebug]", DebugLogConfig.isClientDebugEnabled()),
                button -> {
                    DebugLogConfig.toggleClientDebug();
                    button.setMessage(getToggleText("客户端手持 [ClientDebug]", DebugLogConfig.isClientDebugEnabled()));
                }
        ).dimensions(centerX, startY + gap * 6, buttonWidth, buttonHeight).build());

        // 8. [FrostWalker] 冰霜行者效果调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("冰霜行者 [FrostWalker]", DebugLogConfig.isFrostWalkerEnabled()),
                button -> {
                    DebugLogConfig.toggleFrostWalker();
                    button.setMessage(getToggleText("冰霜行者 [FrostWalker]", DebugLogConfig.isFrostWalkerEnabled()));
                }
        ).dimensions(centerX, startY + gap * 7, buttonWidth, buttonHeight).build());

        // 9. [InfinityCooldown] 无限附魔冷却调试
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("无限冷却 [InfinityCooldown]", DebugLogConfig.isInfinityCooldownEnabled()),
                button -> {
                    DebugLogConfig.toggleInfinityCooldown();
                    button.setMessage(getToggleText("无限冷却 [InfinityCooldown]", DebugLogConfig.isInfinityCooldownEnabled()));
                }
        ).dimensions(centerX, startY + gap * 8, buttonWidth, buttonHeight).build());


    }

    private Text getToggleText(String label, boolean enabled) {
        String status = enabled
                ? "§a" + Text.translatable("screen.hello-mod.debug_log_setting.on").getString()
                : "§c" + Text.translatable("screen.hello-mod.debug_log_setting.off").getString();
        return Text.literal(label + ": " + status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
