package com.example.hellomod.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端 Mixin：当药水处于迅投射线追踪模式时隐藏其渲染。
 *
 * FlyingItemEntityRenderer 是负责渲染 ThrownItemEntity（包括药水瓶）的渲染器。
 */
@Mixin(FlyingItemEntityRenderer.class)
public abstract class SwiftThrowRenderMixin {

    @Inject(method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void onRender(Entity entity, float yaw, float tickDelta, MatrixStack matrices,
                          VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof PotionEntity potionEntity) {
            NbtCompound nbt = potionEntity.getStack().getNbt();
            if (nbt != null && nbt.getBoolean("SwiftThrowRaycast")) {
                // 取消渲染
                ci.cancel();
            }
        }
    }
}
