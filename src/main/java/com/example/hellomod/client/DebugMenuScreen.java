package com.example.hellomod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 调试功能菜单（一级菜单）：列出各功能入口按钮。
 * 点击按钮进入对应的二级菜单来控制开关。
 */
public class DebugMenuScreen extends Screen {

    public DebugMenuScreen() {
        super(Text.translatable("screen.hello-mod.debug_menu.title"));
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 45;

        // 实体血量显示 -> 进入二级菜单
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.hello-mod.debug_menu.health_hud_entry"),
                button -> this.client.setScreen(new HealthHudSettingScreen(this))
        ).dimensions(centerX, startY, buttonWidth, buttonHeight).build());

        // 手持物品显示 -> 进入二级菜单
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.hello-mod.debug_menu.item_hud_entry"),
                button -> this.client.setScreen(new ItemHudSettingScreen(this))
        ).dimensions(centerX, startY + 30, buttonWidth, buttonHeight).build());

        // 调试日志开关 -> 进入二级菜单
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.hello-mod.debug_menu.debug_log_entry"),
                button -> this.client.setScreen(new DebugLogSettingScreen(this))
        ).dimensions(centerX, startY + 60, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
