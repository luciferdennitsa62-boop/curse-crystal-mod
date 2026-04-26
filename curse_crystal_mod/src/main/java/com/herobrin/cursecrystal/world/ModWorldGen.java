package com.herobrin.cursecrystal.world;

import com.herobrin.cursecrystal.CurseCrystalMod;
import com.herobrin.cursecrystal.world.structure.DarkKeeperStructure;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModWorldGen {
    public static void register() {
        DarkKeeperStructure.register();
        CurseCrystalMod.LOGGER.info("Мировая генерация зарегистрирована!");
    }
}
