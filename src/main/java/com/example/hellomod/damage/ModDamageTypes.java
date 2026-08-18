package com.example.hellomod.damage;

import com.example.hellomod.HelloMod;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * 模组自定义伤害类型注册。
 */
public class ModDamageTypes {

    /**
     * 锋利食物伤害类型的注册键。
     * 对应 data/hello-mod/damage_type/sharp_food.json
     */
    public static final RegistryKey<DamageType> SHARP_FOOD = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(HelloMod.MOD_ID, "sharp_food")
    );

    /**
     * 锋利药水伤害类型的注册键。
     * 对应 data/hello-mod/damage_type/sharp_potion.json
     */
    public static final RegistryKey<DamageType> SHARP_POTION = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(HelloMod.MOD_ID, "sharp_potion")
    );

    /**
     * 力量药水伤害类型的注册键。
     * 对应 data/hello-mod/damage_type/power_potion.json
     */
    public static final RegistryKey<DamageType> POWER_POTION = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(HelloMod.MOD_ID, "power_potion")
    );
}
