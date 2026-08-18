package com.example.hellomod.mixin.client;

import com.example.hellomod.client.InfinityCooldownClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin DrawContext.drawItemInSlot，在绘制带 InfinityMarked 标记的物品时
 * 渲染自定义冷却覆盖动画（灰色半透明遮罩，从下往上消退，与原版冷却动画一致）。
 *
 * 原版冷却覆盖渲染逻辑参考 DrawContext.drawItemInSlot：
 * - 使用 RenderSystem 启用混合模式
 * - 在物品图标上方（z层级更高）绘制半透明矩形
 * - 颜色为白色半透明 (ARGB: 0x7FFFFFFF)
 */
@Mixin(DrawContext.class)
public abstract class DrawContextCooldownMixin {

    /**
     * 在 drawItemInSlot 方法末尾注入，绘制自定义冷却覆盖。
     */
    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"))
    private void renderInfinityCooldownOverlay(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        if (stack.isEmpty()) return;

        if (InfinityCooldownClientState.shouldShowCooldown(stack)) {
            float progress = InfinityCooldownClientState.getCooldownProgress();
            if (progress > 0.0f) {
                DrawContext self = (DrawContext) (Object) this;

                // 启用混合模式，确保半透明效果正确渲染在物品图标之上
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                int overlayHeight = (int) (16.0f * progress + 0.5f);
                int overlayY = y + 16 - overlayHeight;

                // 使用 fill 并通过 getMatrices().push/translate 提升 Z 层级
                // 确保覆盖层绘制在物品图标之上
                self.getMatrices().push();
                self.getMatrices().translate(0.0f, 0.0f, 200.0f); // Z=200 确保在物品(Z=150)之上
                self.fill(x, overlayY, x + 16, y + 16, 0x7FFFFFFF);
                self.getMatrices().pop();

                RenderSystem.disableBlend();
            }
        }
    }
}
