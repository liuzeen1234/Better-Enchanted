package com.example.hellomod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 实体血量显示设置（二级菜单）：控制实体血量 HUD 的开关。
 */
public class HealthHudSettingScreen extends Screen {

    private final Screen parent;
    private ButtonWidget toggleButton;

    public HealthHudSettingScreen(Screen parent) {
        super(Text.translatable("screen.hello-mod.health_hud_setting.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int centerY = this.height / 2;

        // 开关按钮
        toggleButton = ButtonWidget.builder(getToggleText(), button -> {
            EntityHealthHud.toggle();
            button.setMessage(getToggleText());
        }).dimensions(centerX, centerY - 10, buttonWidth, buttonHeight).build();

        this.addDrawableChild(toggleButton);


    }

    private Text getToggleText() {
        String status = EntityHealthHud.isEnabled() ? "§a开启" : "§c关闭";
        return Text.translatable("screen.hello-mod.health_hud_setting.toggle", status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
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
