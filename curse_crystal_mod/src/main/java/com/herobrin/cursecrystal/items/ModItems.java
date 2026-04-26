package com.herobrin.cursecrystal.items;

import com.herobrin.cursecrystal.CurseCrystalMod;
import com.herobrin.cursecrystal.items.armor.CurseCrystalArmor;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item CURSE_CRYSTAL_GEM = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_crystal_gem"),
        new Item(new Item.Settings().maxCount(64))
    );

    public static final CurseCrystalSword CURSE_CRYSTAL_SWORD = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_crystal_sword"),
        new CurseCrystalSword()
    );

    public static final CurseCrystalArmor CURSE_HELMET = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_helmet"),
        new CurseCrystalArmor(ArmorItem.Type.HELMET)
    );

    public static final CurseCrystalArmor CURSE_CHESTPLATE = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_chestplate"),
        new CurseCrystalArmor(ArmorItem.Type.CHESTPLATE)
    );

    public static final CurseCrystalArmor CURSE_LEGGINGS = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_leggings"),
        new CurseCrystalArmor(ArmorItem.Type.LEGGINGS)
    );

    public static final CurseCrystalArmor CURSE_BOOTS = Registry.register(
        Registries.ITEM,
        new Identifier(CurseCrystalMod.MOD_ID, "curse_boots"),
        new CurseCrystalArmor(ArmorItem.Type.BOOTS)
    );

    public static void registerItems() {
        CurseCrystalMod.LOGGER.info("Регистрация предметов CurseCrystal...");
    }
}
