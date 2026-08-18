package com.example.hellomod.damage;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 力量药水的自定义伤害源。
 * 死亡消息格式："XXX被强力药水砸死了"
 */
public class PowerPotionDamageSource extends DamageSource {

    public PowerPotionDamageSource(World world) {
        super(
                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(ModDamageTypes.POWER_POTION)
        );
    }

    @Override
    public Text getDeathMessage(LivingEntity killed) {
        return Text.translatable(
                "death.attack.hello-mod.power_potion",
                killed.getDisplayName()
        );
    }

    /**
     * 便捷工厂方法
     */
    public static PowerPotionDamageSource create(World world) {
        return new PowerPotionDamageSource(world);
    }
}
