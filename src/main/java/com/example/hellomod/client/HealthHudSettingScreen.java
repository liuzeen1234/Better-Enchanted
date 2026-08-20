package com.example.hellomod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * 实体血量显示设置（二级菜单）：控制实体血量 HUD 的开关和检测距离。
 */
public class HealthHudSettingScreen extends Screen {

    private final Screen parent;
    private ButtonWidget toggleButton;
    private ButtonWidget detailedInfoButton;
    private TextFieldWidget reachDistanceField;

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

        // 显示详细信息按钮
        detailedInfoButton = ButtonWidget.builder(getDetailedInfoText(), button -> {
            EntityHealthHud.toggleDetailedInfo();
            button.setMessage(getDetailedInfoText());
        }).dimensions(centerX, centerY + 15, buttonWidth, buttonHeight).build();

        this.addDrawableChild(detailedInfoButton);

        // 检测距离输入框
        int fieldWidth = 100;
        int fieldX = this.width / 2 + 10;
        int fieldY = centerY + 45;

        reachDistanceField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, buttonHeight, Text.literal(""));
        reachDistanceField.setText(String.valueOf((int) EntityHealthHud.getReachDistance()));
        reachDistanceField.setChangedListener(value -> {
            try {
                double distance = Double.parseDouble(value);
                if (distance > 0) {
                    EntityHealthHud.setReachDistance(distance);
                }
            } catch (NumberFormatException ignored) {
            }
        });

        this.addDrawableChild(reachDistanceField);
    }

    private Text getToggleText() {
        String status = EntityHealthHud.isEnabled()
                ? "§a" + Text.translatable("screen.hello-mod.debug_log_setting.on").getString()
                : "§c" + Text.translatable("screen.hello-mod.debug_log_setting.off").getString();
        return Text.translatable("screen.hello-mod.health_hud_setting.toggle", status);
    }

    private Text getDetailedInfoText() {
        String status = EntityHealthHud.isDetailedInfoEnabled()
                ? "§a" + Text.translatable("screen.hello-mod.debug_log_setting.on").getString()
                : "§c" + Text.translatable("screen.hello-mod.debug_log_setting.off").getString();
        return Text.translatable("screen.hello-mod.health_hud_setting.detailed_info", status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);

        // 绘制"检测距离"标签文字，在输入框左侧
        String label = Text.translatable("screen.hello-mod.health_hud_setting.reach_distance").getString();
        int labelX = this.width / 2 - this.textRenderer.getWidth(label) - 10 + 10;
        int labelY = this.height / 2 + 45 + (20 - 8) / 2; // 垂直居中于输入框
        context.drawTextWithShadow(this.textRenderer, label, labelX, labelY, 0xFFFFFF);

        // 在输入框后方绘制单位"格"
        String unit = Text.translatable("screen.hello-mod.health_hud_setting.reach_distance_unit").getString();
        int unitX = this.width / 2 + 10 + 100 + 5;
        context.drawTextWithShadow(this.textRenderer, unit, unitX, labelY, 0xAAAAAA);

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
