package com.example.hellomod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 手持物品显示设置（二级菜单）：控制手持物品 HUD 的开关。
 */
public class ItemHudSettingScreen extends Screen {

    private final Screen parent;
    private ButtonWidget toggleButton;

    public ItemHudSettingScreen(Screen parent) {
        super(Text.translatable("screen.hello-mod.item_hud_setting.title"));
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
            HelloModClient.toggleItemHud();
            button.setMessage(getToggleText());
        }).dimensions(centerX, centerY - 10, buttonWidth, buttonHeight).build();

        this.addDrawableChild(toggleButton);

        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.hello-mod.back"),
                button -> this.client.setScreen(parent)
        ).dimensions(centerX, centerY + 20, buttonWidth, buttonHeight).build());
    }

    private Text getToggleText() {
        String status = HelloModClient.isItemHudEnabled() ? "§a开启" : "§c关闭";
        return Text.translatable("screen.hello-mod.item_hud_setting.toggle", status);
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
