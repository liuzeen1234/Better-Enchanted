package com.example.hellomod.mixin;

import com.example.hellomod.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 锁定超级附魔金苹果在铁砧中的附魔惩罚（RepairCost）为固定值 10。
 * 防止多次铁砧操作后惩罚指数增长导致无法继续附魔。
 */
@Mixin(AnvilScreenHandler.class)
public abstract class SuperAppleAnvilMixin {

    /**
     * 注入 updateResult 方法尾部，在铁砧计算完结果后，
     * 如果输出物品是超级附魔金苹果，则将其 RepairCost 锁定为 10。
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void lockSuperAppleRepairCost(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack output = handler.getSlot(2).getStack();
        if (!output.isEmpty() && output.getItem() == ModItems.SUPER_ENCHANTED_GOLDEN_APPLE) {
            output.setRepairCost(10);
        }
    }
}
