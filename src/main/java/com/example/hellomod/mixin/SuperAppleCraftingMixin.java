package com.example.hellomod.mixin;

import com.example.hellomod.enchantment.ModEnchantments;
import com.example.hellomod.item.ModItems;
import com.example.hellomod.item.SuperApplePotionMerger;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin 合成结果更新：当合成输出为超级附魔金苹果时：
 * 1. 自动添加迅投 25 附魔
 * 2. 读取合成格中喷溅型/滞留型药水的药水效果
 * 3. 按合并规则处理后写入产物 NBT（SplashEffects, CloudEffects, SplashInstantCount, CloudInstantCount）
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
            // 添加迅投 25 附魔
            result.addEnchantment(ModEnchantments.SWIFT_THROW, 25);

            // 收集喷溅型药水和滞留型药水的效果
            List<StatusEffectInstance> splashEffects = new ArrayList<>();
            List<StatusEffectInstance> cloudEffects = new ArrayList<>();

            for (int i = 0; i < craftingInventory.size(); i++) {
                ItemStack slot = craftingInventory.getStack(i);
                if (slot.isEmpty()) continue;

                if (slot.isOf(Items.SPLASH_POTION)) {
                    List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(slot);
                    splashEffects.addAll(effects);
                } else if (slot.isOf(Items.LINGERING_POTION)) {
                    List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(slot);
                    cloudEffects.addAll(effects);
                }
            }

            // 合并效果并写入 NBT
            NbtCompound nbt = result.getOrCreateNbt();
            nbt.putString("SuperAppleMode", "eat");

            // 处理喷溅效果（委托给工具类，避免 Mixin 内部类问题）
            SuperApplePotionMerger.writeMergedEffects(nbt, "SplashEffects", "SplashInstantCount", splashEffects);
            // 处理效果云效果
            SuperApplePotionMerger.writeMergedEffects(nbt, "CloudEffects", "CloudInstantCount", cloudEffects);
        }
    }
}
