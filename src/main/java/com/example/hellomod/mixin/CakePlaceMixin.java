package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.block.CakeEnchantmentStorage;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class CakePlaceMixin {

    /**
     * 在方块物品放置前，捕获附魔信息并在放置成功后存储。
     */
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"))
    private void onPlaceBefore(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (world.isClient()) return;

        ItemStack stack = context.getStack();
        // 检查是否是蛋糕物品
        if (stack.getItem() != Blocks.CAKE.asItem()) return;

        BlockPos pos = context.getBlockPos();

        // 存储锋利附魔
        int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
        HelloMod.LOGGER.info("[PlaceDebug] HEAD - Stack: {}, SharpnessLevel: {}", stack, sharpnessLevel);
        if (sharpnessLevel > 0) {
            HelloMod.LOGGER.info("[PlaceDebug] Storing sharpness {} at pos {}", sharpnessLevel, pos);
            CakeEnchantmentStorage.set(pos, sharpnessLevel);
        }

        // 存储击退附魔
        int knockbackLevel = EnchantmentHelper.getLevel(Enchantments.KNOCKBACK, stack);
        HelloMod.LOGGER.info("[PlaceDebug] HEAD - Stack: {}, KnockbackLevel: {}", stack, knockbackLevel);
        if (knockbackLevel > 0) {
            HelloMod.LOGGER.info("[PlaceDebug] Storing knockback {} at pos {}", knockbackLevel, pos);
            CakeEnchantmentStorage.setKnockback(pos, knockbackLevel);
        }

        // 存储火焰附加附魔
        int fireAspectLevel = EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, stack);
        HelloMod.LOGGER.info("[PlaceDebug] HEAD - Stack: {}, FireAspectLevel: {}", stack, fireAspectLevel);
        if (fireAspectLevel > 0) {
            HelloMod.LOGGER.info("[PlaceDebug] Storing fire_aspect {} at pos {}", fireAspectLevel, pos);
            CakeEnchantmentStorage.setFireAspect(pos, fireAspectLevel);
        }

        // 存储效率附魔
        int efficiencyLevel = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
        HelloMod.LOGGER.info("[PlaceDebug] HEAD - Stack: {}, EfficiencyLevel: {}", stack, efficiencyLevel);
        if (efficiencyLevel > 0) {
            HelloMod.LOGGER.info("[PlaceDebug] Storing efficiency {} at pos {}", efficiencyLevel, pos);
            CakeEnchantmentStorage.setEfficiency(pos, efficiencyLevel);
        }
    }
}
