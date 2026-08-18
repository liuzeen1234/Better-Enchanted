package com.example.hellomod.block;

import com.example.hellomod.HelloMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class HelloModBlockEntities {

    public static BlockEntityType<EnchantedCakeBlockEntity> ENCHANTED_CAKE_BLOCK_ENTITY;

    public static void register() {
        ENCHANTED_CAKE_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(HelloMod.MOD_ID, "enchanted_cake"),
                FabricBlockEntityTypeBuilder.create(EnchantedCakeBlockEntity::new, Blocks.CAKE).build()
        );
    }
}
