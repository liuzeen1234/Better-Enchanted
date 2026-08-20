package com.example.hellomod.mixin.client;

import com.example.hellomod.item.SuperEnchantedGoldenAppleItem;
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
 * 客户端 Mixin：拦截左键攻击，当主手持有超级附魔金苹果时切换模式。
 * 
 * 切换规则：
 * - 手持超级附魔金苹果时左键 -> 切换模式
 * - 切换后 5 tick 冷却期间无效果
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
        if (mainHand.isEmpty() || mainHand.getItem() != ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        // 冷却中，取消事件
        if (hellomod_switchCooldown > 0) {
            cir.setReturnValue(false);
            return;
        }

        // 切换模式（客户端立即生效 + 发包到服务端同步）
        SuperEnchantedGoldenAppleItem.toggleMode(mainHand);
        hellomod_switchCooldown = 5; // 5 tick 冷却

        // 发送 C2S 包同步到服务端
        SuperAppleModeSwitchC2SPacket.send();

        cir.setReturnValue(false);
    }
}
