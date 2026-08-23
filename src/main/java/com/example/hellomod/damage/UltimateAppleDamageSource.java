package com.example.hellomod.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 终极附魔金苹果的真实伤害源。
 * 无视护甲和附魔保护，伤害来源为投掷者。
 * 死亡消息格式："XXX被YYY的终极附魔金苹果消灭了"
 */
public class UltimateAppleDamageSource extends DamageSource {

    private final Entity attacker;

    public UltimateAppleDamageSource(World world, Entity attacker) {
        super(
                world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(ModDamageTypes.ULTIMATE_APPLE),
                attacker
        );
        this.attacker = attacker;
    }

    @Override
    public Text getDeathMessage(LivingEntity killed) {
        if (attacker != null) {
            return Text.translatable(
                    "death.attack.hello-mod.ultimate_apple.player",
                    killed.getDisplayName(),
                    attacker.getDisplayName()
            );
        }
        return Text.translatable(
                "death.attack.hello-mod.ultimate_apple",
                killed.getDisplayName()
        );
    }

    /**
     * 便捷工厂方法
     */
    public static UltimateAppleDamageSource create(World world, Entity attacker) {
        return new UltimateAppleDamageSource(world, attacker);
    }
}
