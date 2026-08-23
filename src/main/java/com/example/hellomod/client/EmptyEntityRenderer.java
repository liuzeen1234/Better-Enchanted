package com.example.hellomod.client;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

/**
 * 空渲染器：不渲染任何内容。
 * 用于超级附魔金苹果投掷实体，因为迅投等级过高导致速度极快，
 * 渲染贴图会出现MC-128812类似的视觉问题。
 */
public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    public EmptyEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(T entity) {
        // 不会被调用，因为 render 什么都不做
        return null;
    }

    @Override
    public boolean shouldRender(T entity, net.minecraft.client.render.Frustum frustum, double x, double y, double z) {
        return false; // 永远不渲染
    }
}
