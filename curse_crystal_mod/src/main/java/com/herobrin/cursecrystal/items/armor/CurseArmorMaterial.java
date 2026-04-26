package com.herobrin.cursecrystal.items.armor;

import com.herobrin.cursecrystal.items.ModItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.Map;

public class CurseArmorMaterial {
    public static final ArmorMaterial INSTANCE = new ArmorMaterial(
        Map.of(
            ArmorItem.Type.HELMET,     5,
            ArmorItem.Type.CHESTPLATE, 8,
            ArmorItem.Type.LEGGINGS,   7,
            ArmorItem.Type.BOOTS,      5
        ),
        25,
        SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
        () -> Ingredient.ofItems(ModItems.CURSE_CRYSTAL_GEM),
        List.of(new ArmorMaterial.Layer(
            new net.minecraft.util.Identifier("cursecrystal", "curse_crystal")
        )),
        6.0f,
        0.5f
    );
}
