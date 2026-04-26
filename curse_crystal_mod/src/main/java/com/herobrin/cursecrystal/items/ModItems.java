package com.herobrin.cursecrystal.items;

import com.herobrin.cursecrystal.CurseCrystalMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    // Кристалл проклятья (материал для крафта и починки)
    public static final Item CURSE_CRYSTAL_GEM = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_crystal_gem"),
        new Item(new Item.Settings().maxCount(64))
    );

    // Сам меч
    public static final CurseCrystalSword CURSE_CRYSTAL_SWORD = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_crystal_sword"),
        new CurseCrystalSword()
    );

    public static void registerItems() {
        CurseCrystalMod.LOGGER.info("Регистрация предметов CurseCrystal...");
    }
}
