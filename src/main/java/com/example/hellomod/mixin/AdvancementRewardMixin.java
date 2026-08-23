package com.example.hellomod.mixin;

import com.example.hellomod.HelloMod;
import com.example.hellomod.item.ModItems;
import com.example.hellomod.item.UltimateEnchantedGoldenAppleItem;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 进度完成监听 Mixin。
 * 当玩家通过任何方式（铁砧触发、/advancement grant 命令等）完成
 * "终极金苹果" 进度时，给予 1 个终极附魔金苹果 + 1000 经验等级。
 */
@Mixin(PlayerAdvancementTracker.class)
public abstract class AdvancementRewardMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Unique
    private static final Identifier ULTIMATE_APPLE_ADVANCEMENT_ID =
            new Identifier(HelloMod.MOD_ID, "challenge/ultimate_golden_apple");

    /**
     * 在 grantCriterion 返回 true（表示进度状态发生了变化）后检查进度是否刚刚完成。
     * 
     * grantCriterion 签名 (1.20.1)：public boolean grantCriterion(Advancement advancement, String criterionName)
     * 返回 true 表示 criterion 授予成功。
     */
    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void onGrantCriterion(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return; // criterion 未成功授予

        // 检查是否是我们关心的进度
        Identifier advId = advancement.getId();
        if (!ULTIMATE_APPLE_ADVANCEMENT_ID.equals(advId)) return;

        // 检查进度是否刚完成
        PlayerAdvancementTracker tracker = (PlayerAdvancementTracker) (Object) this;
        if (!tracker.getProgress(advancement).isDone()) return;

        // 进度刚刚完成，给予奖励
        giveUltimateAppleReward(owner);
    }

    @Unique
    private void giveUltimateAppleReward(ServerPlayerEntity player) {
        // 给予 1000 经验等级
        player.addExperienceLevels(1000);

        // 给予 1 个终极附魔金苹果（自带效率9、耐久10、迅投25、无限1）
        ItemStack ultimateApple = UltimateEnchantedGoldenAppleItem.createDefaultStack();
        if (!player.getInventory().insertStack(ultimateApple)) {
            player.dropItem(ultimateApple, false);
        }

        HelloMod.LOGGER.info("[UltimateApple] Advancement completed! Gave ultimate apple to player {}",
                player.getName().getString());
    }
}
