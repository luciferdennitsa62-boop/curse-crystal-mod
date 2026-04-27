package com.herobrin.cursecrystal.items;

import com.herobrin.cursecrystal.CurseCrystalMod;
import com.herobrin.cursecrystal.items.armor.CurseCrystalArmor;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item CURSE_CRYSTAL_GEM = register("curse_crystal_gem",
        new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(CurseCrystalMod.MOD_ID, "curse_crystal_gem")))
            .maxCount(64)));

    public static final CurseCrystalSword CURSE_CRYSTAL_SWORD = register("curse_crystal_sword",
        new CurseCrystalSword(
            RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(CurseCrystalMod.MOD_ID, "curse_crystal_sword"))));

    public static final CurseCrystalArmor CURSE_HELMET = register("curse_helmet",
        new CurseCrystalArmor(ArmorItem.Type.HELMET,
            RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(CurseCrystalMod.MOD_ID, "curse_helmet"))));

    public static final CurseCrystalArmor CURSE_CHESTPLATE = register("curse_chestplate",
        new CurseCrystalArmor(ArmorItem.Type.CHESTPLATE,
            RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(CurseCrystalMod.MOD_ID, "curse_chestplate"))));

    public static final CurseCrystalArmor CURSE_LEGGINGS = register("curse_leggings",
        new CurseCrystalArmor(ArmorItem.Type.LEGGINGS,
            RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(CurseCrystalMod.MOD_ID, "curse_leggings"))));

    public static final CurseCrystalArmor CURSE_BOOTS = register("curse_boots",
        new CurseCrystalArmor(ArmorItem.Type.BOOTS,
            RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(CurseCrystalMod.MOD_ID, "curse_boots"))));

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM,
            Identifier.of(CurseCrystalMod.MOD_ID, name), item);
    }

    public static void registerItems() {
        CurseCrystalMod.LOGGER.info("Регистрация предметов CurseCrystal 1.21.4...");
    }
}
