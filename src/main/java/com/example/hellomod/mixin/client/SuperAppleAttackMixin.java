package com.example.hellomod.mixin.client;

import com.example.hellomod.item.SuperEnchantedGoldenAppleItem;
import com.example.hellomod.item.UltimateEnchantedGoldenAppleItem;
import com.example.hellomod.item.ModItems;
import com.example.hellomod.network.SuperAppleModeSwitchC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端 Mixin：拦截左键攻击，当主手持有超级/终极附魔金苹果时切换模式。
 * 
 * 切换规则：
 * - 手持超级附魔金苹果时左键 -> 切换模式（5 tick 冷却）
 * - 手持终极附魔金苹果时左键 -> 切换模式（2 tick 冷却）
 */
@Mixin(MinecraftClient.class)
public abstract class SuperAppleAttackMixin {

    @Unique
    private int hellomod_switchCooldown = 0;

    /**
     * 每 tick 递减冷却计数器。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (hellomod_switchCooldown > 0) {
            hellomod_switchCooldown--;
        }
    }

    /**
     * 拦截攻击（左键点击）事件。
     * doAttack 在 MC 1.20.4 中返回 boolean。
     */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        PlayerEntity player = client.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.isEmpty()) return;

        if (mainHand.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            // 超级附魔金苹果：5 tick 冷却
            if (hellomod_switchCooldown > 0) {
                cir.setReturnValue(false);
                return;
            }

            SuperEnchantedGoldenAppleItem.toggleMode(mainHand);
            hellomod_switchCooldown = 5;
            SuperAppleModeSwitchC2SPacket.send();
            cir.setReturnValue(false);
        } else if (mainHand.getItem() == ModItems.ULTIMATE_ENCHANTED_GOLDEN_APPLE) {
            // 终极附魔金苹果：2 tick 冷却
            if (hellomod_switchCooldown > 0) {
                cir.setReturnValue(false);
                return;
            }

            UltimateEnchantedGoldenAppleItem.toggleMode(mainHand);
            hellomod_switchCooldown = 2;
            SuperAppleModeSwitchC2SPacket.sendUltimate();
            cir.setReturnValue(false);
        }
    }
}
