package com.example.hellomod.damage;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 锋利药水的自定义伤害源。
 * 重写 getDeathMessage 以在死亡消息中显示药水信息。
 * 死亡消息格式："XXX被锋利的药水砸死了"
 */
public class SharpPotionDamageSource extends DamageSource {

    public SharpPotionDamageSource(World world) {
        super(
                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(ModDamageTypes.SHARP_POTION)
        );
    }

    @Override
    public Text getDeathMessage(LivingEntity killed) {
        return Text.translatable(
                "death.attack.hello-mod.sharp_potion",
                killed.getDisplayName()
        );
    }

    /**
     * 便捷工厂方法
     */
    public static SharpPotionDamageSource create(World world) {
        return new SharpPotionDamageSource(world);
    }
}
