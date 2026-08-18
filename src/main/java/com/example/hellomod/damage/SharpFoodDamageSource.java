package com.example.hellomod.damage;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 锋利食物的自定义伤害源。
 * 重写 getDeathMessage 以在死亡消息中显示食物名称。
 * 死亡消息格式："XXX被YYY割破了喉咙"
 */
public class SharpFoodDamageSource extends DamageSource {

    private final ItemStack foodStack;

    public SharpFoodDamageSource(World world, ItemStack foodStack) {
        super(
                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(ModDamageTypes.SHARP_FOOD)
        );
        this.foodStack = foodStack.copy(); // 复制以防止后续被消耗
    }

    @Override
    public Text getDeathMessage(LivingEntity killed) {
        // 如果食物物品存在且有效，显示 "XXX被YYY割破了喉咙"
        if (!foodStack.isEmpty()) {
            return Text.translatable(
                    "death.attack.hello-mod.sharp_food.item",
                    killed.getDisplayName(),
                    foodStack.toHoverableText()
            );
        }
        // 回退到通用格式 "XXX被锋利的食物割破了喉咙"
        return Text.translatable(
                "death.attack.hello-mod.sharp_food",
                killed.getDisplayName()
        );
    }

    /**
     * 便捷工厂方法：创建带食物信息的锋利伤害源
     */
    public static SharpFoodDamageSource create(World world, ItemStack foodStack) {
        return new SharpFoodDamageSource(world, foodStack);
    }
}
