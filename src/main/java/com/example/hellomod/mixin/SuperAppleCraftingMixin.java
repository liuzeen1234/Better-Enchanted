package com.example.hellomod.mixin;

import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.item.ModItems;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 合成结果更新：当合成输出为超级附魔金苹果时，自动添加迅投 255。
 */
@Mixin(CraftingScreenHandler.class)
public abstract class SuperAppleCraftingMixin {

    /**
     * 注入 updateResult 方法的尾部，在合成结果计算后修改输出。
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private static void onUpdateResult(ScreenHandler handler, World world, net.minecraft.entity.player.PlayerEntity player, RecipeInputInventory craftingInventory, net.minecraft.inventory.CraftingResultInventory resultInventory, CallbackInfo ci) {
        ItemStack result = resultInventory.getStack(0);
        if (!result.isEmpty() && result.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            // 添加迅投 25 附魔（原设计255会导致数字溢出显示异常，降级为25）
            result.addEnchantment(ModEnchantments.SWIFT_THROW, 25);
        }
    }
}
