package com.example.hellomod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 调试功能菜单：提供 HUD 开关的集中管理界面。
 * 可通过按键绑定打开（默认无绑定）。
 */
public class DebugMenuScreen extends Screen {

    private ButtonWidget healthHudButton;
    private ButtonWidget itemHudButton;

    public DebugMenuScreen() {
        super(Text.translatable("screen.hello-mod.debug_menu.title"));
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 30;

        // 切换实体血量显示按钮
        healthHudButton = ButtonWidget.builder(getHealthHudButtonText(), button -> {
            EntityHealthHud.toggle();
            button.setMessage(getHealthHudButtonText());
        }).dimensions(centerX, startY, buttonWidth, buttonHeight).build();

        // 切换手持物品显示按钮
        itemHudButton = ButtonWidget.builder(getItemHudButtonText(), button -> {
            HelloModClient.toggleItemHud();
            button.setMessage(getItemHudButtonText());
        }).dimensions(centerX, startY + 30, buttonWidth, buttonHeight).build();

        this.addDrawableChild(healthHudButton);
        this.addDrawableChild(itemHudButton);
    }

    private Text getHealthHudButtonText() {
        String status = EntityHealthHud.isEnabled() ? "§a开启" : "§c关闭";
        return Text.translatable("screen.hello-mod.debug_menu.health_hud", status);
    }

    private Text getItemHudButtonText() {
        String status = HelloModClient.isItemHudEnabled() ? "§a开启" : "§c关闭";
        return Text.translatable("screen.hello-mod.debug_menu.item_hud", status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
