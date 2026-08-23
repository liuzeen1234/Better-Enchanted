package com.example.hellomod.entity;

import com.example.hellomod.HelloMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 自定义实体注册类。
 */
public class ModEntities {

    public static final EntityType<SuperGoldenAppleEntity> SUPER_GOLDEN_APPLE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(HelloMod.MOD_ID, "super_golden_apple"),
            FabricEntityTypeBuilder.<SuperGoldenAppleEntity>create(SpawnGroup.MISC, SuperGoldenAppleEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeChunks(4)
                    .trackedUpdateRate(10)
                    .build()
    );

    public static final EntityType<UltimateGoldenAppleEntity> ULTIMATE_GOLDEN_APPLE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(HelloMod.MOD_ID, "ultimate_golden_apple"),
            FabricEntityTypeBuilder.<UltimateGoldenAppleEntity>create(SpawnGroup.MISC, UltimateGoldenAppleEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeChunks(4)
                    .trackedUpdateRate(10)
                    .build()
    );

    public static void register() {
        HelloMod.LOGGER.info("[ModEntities] Registered custom entities.");
    }
}
