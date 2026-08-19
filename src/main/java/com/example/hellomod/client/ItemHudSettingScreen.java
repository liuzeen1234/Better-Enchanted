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
    private ButtonWidget advancedToggleButton;

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

        // 高级物品显示开关按钮
        advancedToggleButton = ButtonWidget.builder(getAdvancedToggleText(), button -> {
            HelloModClient.toggleAdvancedItemHud();
            button.setMessage(getAdvancedToggleText());
        }).dimensions(centerX, centerY + 20, buttonWidth, buttonHeight).build();

        this.addDrawableChild(advancedToggleButton);
    }

    private Text getToggleText() {
        String status = HelloModClient.isItemHudEnabled()
                ? "§a" + Text.translatable("screen.hello-mod.debug_log_setting.on").getString()
                : "§c" + Text.translatable("screen.hello-mod.debug_log_setting.off").getString();
        return Text.translatable("screen.hello-mod.item_hud_setting.toggle", status);
    }

    private Text getAdvancedToggleText() {
        String status = HelloModClient.isAdvancedItemHudEnabled()
                ? "§a" + Text.translatable("screen.hello-mod.debug_log_setting.on").getString()
                : "§c" + Text.translatable("screen.hello-mod.debug_log_setting.off").getString();
        return Text.translatable("screen.hello-mod.item_hud_setting.advanced_toggle", status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
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
