package com.herobrin.cursecrystal;

import com.herobrin.cursecrystal.items.ModItems;
import com.herobrin.cursecrystal.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurseCrystalMod implements ModInitializer {
    public static final String MOD_ID = "cursecrystal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerItems();
        ModWorldGen.register();
        LOGGER.info("Curse Crystal Mod 2.0 — Хранитель Тьмы пробуждён! ☠️");
    }
}
