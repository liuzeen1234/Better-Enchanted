package com.example.hellomod.client;

import com.example.hellomod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * 客户端入口：注册实体渲染器、无限附魔冷却同步。
 * 调试菜单和 HUD 功能已移至独立的 debug-menu Mod。
 */
public class HelloModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册无限附魔冷却客户端同步
        InfinityCooldownClientState.register();

        // 注册超级附魔金苹果投掷实体的渲染器（空渲染器，不显示投掷物贴图）
        EntityRendererRegistry.register(ModEntities.SUPER_GOLDEN_APPLE_ENTITY, EmptyEntityRenderer::new);

        // 注册终极附魔金苹果投掷实体的渲染器（同样使用空渲染器）
        EntityRendererRegistry.register(ModEntities.ULTIMATE_GOLDEN_APPLE_ENTITY, EmptyEntityRenderer::new);
    }
}
