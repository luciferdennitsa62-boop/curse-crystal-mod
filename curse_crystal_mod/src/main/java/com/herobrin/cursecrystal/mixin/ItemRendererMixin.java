package com.herobrin.cursecrystal.mixin;

import com.herobrin.cursecrystal.CurseCrystalMod;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    // Этот миксин зарезервирован для будущих визуальных эффектов
    // Фиолетовое свечение меча реализуется через setOnFire + enchanted glint эффект
}
