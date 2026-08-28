package com.debugmenu.client;

import com.debugmenu.config.DebugMenuConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * HUD 设置屏幕：控制实体血量 HUD 和手持物品 HUD 的开关。
 * 这是调试菜单 Mod 自带的功能，不依赖任何其他 Mod 的注册。
 */
public class HudSettingsScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget reachDistanceField;

    public HudSettingsScreen(Screen parent) {
        super(Text.literal("HUD 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 240;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 70;

        // === 实体血量 HUD ===
        // 开关
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("实体血量显示", DebugMenuConfig.isEntityHealthHudEnabled()),
                button -> {
                    DebugMenuConfig.setEntityHealthHudEnabled(!DebugMenuConfig.isEntityHealthHudEnabled());
                    button.setMessage(getToggleText("实体血量显示", DebugMenuConfig.isEntityHealthHudEnabled()));
                }
        ).dimensions(centerX, startY, buttonWidth, buttonHeight).build());

        // 详细信息开关
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("显示详细 NBT 信息", DebugMenuConfig.isEntityHealthHudDetailedInfo()),
                button -> {
                    DebugMenuConfig.setEntityHealthHudDetailedInfo(!DebugMenuConfig.isEntityHealthHudDetailedInfo());
                    button.setMessage(getToggleText("显示详细 NBT 信息", DebugMenuConfig.isEntityHealthHudDetailedInfo()));
                }
        ).dimensions(centerX, startY + 24, buttonWidth, buttonHeight).build());

        // 检测距离输入框
        int fieldWidth = 80;
        int fieldX = this.width / 2 + 30;
        int fieldY = startY + 50;
        reachDistanceField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, fieldWidth, buttonHeight, Text.literal(""));
        reachDistanceField.setText(String.valueOf((int) DebugMenuConfig.getEntityHealthHudReachDistance()));
        reachDistanceField.setChangedListener(value -> {
            try {
                double distance = Double.parseDouble(value);
                if (distance > 0 && distance <= 256) {
                    DebugMenuConfig.setEntityHealthHudReachDistance(distance);
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.addDrawableChild(reachDistanceField);

        // === 手持物品 HUD ===
        int itemY = startY + 85;
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("手持物品显示", DebugMenuConfig.isItemHudEnabled()),
                button -> {
                    DebugMenuConfig.setItemHudEnabled(!DebugMenuConfig.isItemHudEnabled());
                    button.setMessage(getToggleText("手持物品显示", DebugMenuConfig.isItemHudEnabled()));
                }
        ).dimensions(centerX, itemY, buttonWidth, buttonHeight).build());

        // 高级物品信息
        this.addDrawableChild(ButtonWidget.builder(
                getToggleText("高级物品显示 (NBT)", DebugMenuConfig.isAdvancedItemHudEnabled()),
                button -> {
                    DebugMenuConfig.setAdvancedItemHudEnabled(!DebugMenuConfig.isAdvancedItemHudEnabled());
                    button.setMessage(getToggleText("高级物品显示 (NBT)", DebugMenuConfig.isAdvancedItemHudEnabled()));
                }
        ).dimensions(centerX, itemY + 24, buttonWidth, buttonHeight).build());

        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("返回"),
                button -> this.close()
        ).dimensions(this.width / 2 - 50, this.height - 30, 100, buttonHeight).build());
    }

    private Text getToggleText(String label, boolean enabled) {
        String status = enabled ? "§a开启" : "§c关闭";
        return Text.literal(label + ": " + status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        // 检测距离标签
        String label = "检测距离:";
        int labelX = this.width / 2 - this.textRenderer.getWidth(label) - 35 + 30;
        int labelY = this.height / 2 - 70 + 50 + (20 - 8) / 2;
        context.drawTextWithShadow(this.textRenderer, label, labelX, labelY, 0xFFFFFF);

        // 单位
        int unitX = this.width / 2 + 30 + 80 + 5;
        context.drawTextWithShadow(this.textRenderer, "格", unitX, labelY, 0xAAAAAA);

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
