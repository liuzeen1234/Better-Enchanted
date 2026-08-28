package com.debugmenu.client;

import com.debugmenu.api.DebugMenuApi;
import com.debugmenu.api.DebugToggleEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

/**
 * 调试功能菜单主屏幕。
 *
 * <p>动态读取所有通过 {@link DebugMenuApi} 注册的调试开关，
 * 按 modId 分组显示。如果没有任何注册，显示"无可控制的开关"。
 *
 * <p>支持滚动：当开关数量超过一屏时可上下滚动。
 */
public class DebugMenuScreen extends Screen {

    /** 每个按钮的高度 */
    private static final int BUTTON_HEIGHT = 20;
    /** 按钮间距 */
    private static final int GAP = 22;
    /** 按钮宽度 */
    private static final int BUTTON_WIDTH = 260;
    /** Mod 分组标题高度 */
    private static final int GROUP_HEADER_HEIGHT = 16;
    /** 顶部边距 */
    private static final int TOP_MARGIN = 40;
    /** 底部边距（留给关闭按钮） */
    private static final int BOTTOM_MARGIN = 40;

    /** 滚动偏移量 */
    private int scrollOffset = 0;
    /** 内容总高度 */
    private int totalContentHeight = 0;

    public DebugMenuScreen() {
        super(Text.literal("调试功能菜单"));
    }

    @Override
    protected void init() {
        this.scrollOffset = 0;
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        this.clearChildren();

        if (!DebugMenuApi.hasEntries()) {
            // 无可控制的开关 — 不添加任何按钮，render 中绘制提示文字
            // 添加 HUD 设置按钮（调试 Mod 自带功能，始终可用）
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("HUD 设置"),
                    button -> this.client.setScreen(new HudSettingsScreen(this))
            ).dimensions(this.width / 2 - 100, this.height / 2 + 20, 200, BUTTON_HEIGHT).build());

            // 添加关闭按钮
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("关闭"),
                    button -> this.close()
            ).dimensions(this.width / 2 - 50, this.height - 30, 100, BUTTON_HEIGHT).build());
            return;
        }

        Map<String, List<DebugToggleEntry>> grouped = DebugMenuApi.getEntriesByMod();
        int centerX = this.width / 2 - BUTTON_WIDTH / 2;
        int y = TOP_MARGIN - scrollOffset;

        for (Map.Entry<String, List<DebugToggleEntry>> group : grouped.entrySet()) {
            // Mod 分组标题占用空间
            y += GROUP_HEADER_HEIGHT;

            // 每个开关一个按钮
            for (DebugToggleEntry entry : group.getValue()) {
                if (y + BUTTON_HEIGHT > TOP_MARGIN - 5 && y < this.height - BOTTOM_MARGIN) {
                    // 按钮在可视区域内
                    final DebugToggleEntry finalEntry = entry;
                    ButtonWidget btn = ButtonWidget.builder(
                            getToggleText(entry),
                            button -> {
                                finalEntry.toggle();
                                button.setMessage(getToggleText(finalEntry));
                            }
                    ).dimensions(centerX, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
                    this.addDrawableChild(btn);
                }
                y += GAP;
            }

            // 分组间距
            y += 6;
        }

        totalContentHeight = y + scrollOffset - TOP_MARGIN;

        // HUD 设置按钮（调试 Mod 自带功能）
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("HUD 设置"),
                button -> this.client.setScreen(new HudSettingsScreen(this))
        ).dimensions(this.width / 2 - 130, this.height - 30, 120, BUTTON_HEIGHT).build());

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("关闭"),
                button -> this.close()
        ).dimensions(this.width / 2 + 10, this.height - 30, 100, BUTTON_HEIGHT).build());
    }

    private Text getToggleText(DebugToggleEntry entry) {
        String status = entry.isEnabled() ? "§a开启" : "§c关闭";
        return Text.literal(entry.getDisplayName() + ": " + status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        if (!DebugMenuApi.hasEntries()) {
            // 画面中央显示"无可控制的开关"
            String noEntryText = "无可控制的开关";
            int textWidth = this.textRenderer.getWidth(noEntryText);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(noEntryText),
                    this.width / 2, this.height / 2, 0xAAAAAA);
        } else {
            // 绘制分组标题
            Map<String, List<DebugToggleEntry>> grouped = DebugMenuApi.getEntriesByMod();
            int y = TOP_MARGIN - scrollOffset;

            for (Map.Entry<String, List<DebugToggleEntry>> group : grouped.entrySet()) {
                // 绘制 mod 分组标题
                if (y > TOP_MARGIN - 15 && y < this.height - BOTTOM_MARGIN) {
                    String header = "── " + group.getKey() + " ──";
                    context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(header),
                            this.width / 2, y + 3, 0xFFFF55);
                }
                y += GROUP_HEADER_HEIGHT;

                // 跳过按钮区域
                y += group.getValue().size() * GAP;

                // 分组间距
                y += 6;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, totalContentHeight - (this.height - TOP_MARGIN - BOTTOM_MARGIN));
        scrollOffset -= (int) (verticalAmount * 10);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
