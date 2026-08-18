package com.example.hellomod.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class EnchantedCakeBlockEntity extends BlockEntity {

    private int sharpnessLevel = 0;

    public EnchantedCakeBlockEntity(BlockPos pos, BlockState state) {
        super(HelloModBlockEntities.ENCHANTED_CAKE_BLOCK_ENTITY, pos, state);
    }

    public int getSharpnessLevel() {
        return sharpnessLevel;
    }

    public void setSharpnessLevel(int level) {
        this.sharpnessLevel = level;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("SharpnessLevel", sharpnessLevel);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        sharpnessLevel = nbt.getInt("SharpnessLevel");
    }
}
