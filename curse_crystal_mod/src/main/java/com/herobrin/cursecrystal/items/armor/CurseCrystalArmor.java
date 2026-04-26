package com.herobrin.cursecrystal.items.armor;

import com.herobrin.cursecrystal.items.CurseCrystalSword;
import com.herobrin.cursecrystal.items.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

public class CurseCrystalArmor extends ArmorItem {

    private static final String VORTEX_CD = "CurseVortexCD";

    public CurseCrystalArmor(Type type) {
        super(CurseArmorMaterial.INSTANCE, type, new Settings().maxCount(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (!(entity instanceof PlayerEntity player) || world.isClient) return;

        boolean hasHelmet   = hasArmor(player, ModItems.CURSE_HELMET);
        boolean hasChest    = hasArmor(player, ModItems.CURSE_CHESTPLATE);
        boolean hasLegs     = hasArmor(player, ModItems.CURSE_LEGGINGS);
        boolean hasBoots    = hasArmor(player, ModItems.CURSE_BOOTS);
        boolean fullSet     = hasHelmet && hasChest && hasLegs && hasBoots;

        // === ШЛЕМ — мобы светятся ===
        if (hasHelmet && getType() == Type.HELMET) {
            world.getEntitiesByClass(LivingEntity.class,
                    new Box(player.getPos(), player.getPos()).expand(64),
                    e -> e != player)
                .forEach(e -> e.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false)));
        }

        // === НАГРУДНИК — +2 строки HP ===
        if (hasChest && getType() == Type.CHESTPLATE) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 40, 1, false, false));
        }

        // === ПОНОЖИ — нет урона от падения ===
        if (hasLegs && getType() == Type.LEGGINGS) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false));
            player.fallDistance = 0;
        }

        // === БОТИНКИ — Скорость 3, вода/лава ===
        if (hasBoots && getType() == Type.BOOTS) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        40, 2, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, false, false));
            // Ходьба по воде/лаве
            if (!world.isClient) {
                var fluid = world.getFluidState(player.getBlockPos());
                if (!fluid.isEmpty() && !player.isSneaking())
                    player.setVelocity(player.getVelocity().x, 0.12, player.getVelocity().z);
            }
        }

        // === ПОЛНЫЙ КОМПЛЕКТ ===
        if (fullSet) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,      40, 7, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,    40, 9, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 0, false, false));
            // Полёт
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
            // Чиним меч
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack s = player.getInventory().getStack(i);
                if (s.getItem() instanceof CurseCrystalSword && s.isDamaged())
                    s.setDamage(Math.max(0, s.getDamage() - 5));
            }
            // Частицы
            if (!world.isClient && world.getTime() % 15 == 0) {
                ServerWorld sw = (ServerWorld) world;
                DustParticleEffect p = new DustParticleEffect(new Vector3f(0.7f, 0.05f, 1.0f), 1.2f);
                for (int i = 0; i < 16; i++) {
                    double a = i*(2*Math.PI/16);
                    sw.spawnParticles(p, player.getX()+Math.cos(a)*1.3, player.getY()+1, player.getZ()+Math.sin(a)*1.3, 1, 0, 0.05, 0, 0.02);
                }
            }

            // === Ctrl+ПКМ без меча = ВОРОНКА ===
            if (!world.isClient && player.isSneaking()) {
                ItemStack mainHand = player.getMainHandStack();
                ItemStack offHand  = player.getOffHandStack();
                boolean noSword = !(mainHand.getItem() instanceof CurseCrystalSword)
                               && !(offHand.getItem() instanceof CurseCrystalSword);

                if (noSword && getType() == Type.CHESTPLATE) {
                    NbtCheck: {
                        var nbt = stack.getOrCreateNbt();
                        long last = nbt.getLong(VORTEX_CD);
                        long now  = world.getTime();
                        if (now - last < 1200) {
                            long rem = (1200-(now-last))/20;
                            player.sendMessage(Text.literal("⏳ Воронка: ещё "+rem+" сек").formatted(Formatting.DARK_RED), true);
                            break NbtCheck;
                        }
                        nbt.putLong(VORTEX_CD, now);
                        triggerVortex((ServerWorld)world, player);
                    }
                }
            }
        } else {
            // Снял — отключаем полёт
            if (getType() == Type.CHESTPLATE) {
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().allowFlying = false;
                    player.getAbilities().flying = false;
                    player.sendAbilitiesUpdate();
                }
            }
        }
    }

    // ============================================================
    //  ВОРОНКА АПОКАЛИПСИСА
    // ============================================================
    public static void triggerVortex(ServerWorld world, PlayerEntity player) {
        Vec3d center = player.getPos();

        DustParticleEffect fire1 = new DustParticleEffect(new Vector3f(1.0f, 0.3f, 0.0f), 2.5f);
        DustParticleEffect fire2 = new DustParticleEffect(new Vector3f(0.8f, 0.05f, 1.0f), 2.0f);

        // Сфера из огня
        for (int lat = 0; lat < 24; lat++)
            for (int lon = 0; lon < 48; lon++) {
                double theta = lat*(Math.PI/24), phi = lon*(2*Math.PI/48);
                double sx = center.x+15*Math.sin(theta)*Math.cos(phi);
                double sy = center.y+1+15*Math.cos(theta);
                double sz = center.z+15*Math.sin(theta)*Math.sin(phi);
                world.spawnParticles((lat+lon)%2==0?fire1:fire2, sx, sy, sz, 1, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticles(ParticleTypes.FLAME, sx, sy, sz, 1, 0.1, 0.1, 0.1, 0.05);
            }

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y+1, center.z, 20, 5, 5, 5, 0.3);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y+1, center.z, 500, 8, 8, 8, 0.2);

        // Убиваем всех в 500 блоках
        world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(500), e -> e != player)
             .forEach(e -> e.damage(world.getDamageSources().magic(), 99999999f));

        // Воронка — ломаем блоки (блоки не выпадают)
        BlockPos playerPos = player.getBlockPos();
        for (int depth = 0; depth < 80; depth++) {
            int r = 500 - depth*6;
            if (r <= 0) break;
            for (int dx = -r; dx <= r; dx += 4)
                for (int dz = -r; dz <= r; dz += 4)
                    if (dx*dx+dz*dz <= (long)r*r) {
                        BlockPos bp = playerPos.add(dx, -depth, dz);
                        if (!world.getBlockState(bp).isAir())
                            world.setBlockState(bp, Blocks.AIR.getDefaultState());
                    }
        }

        player.sendMessage(Text.literal("☠ ВОРОНКА АПОКАЛИПСИСА ☠").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
    }

    private boolean hasArmor(PlayerEntity player, net.minecraft.item.Item item) {
        for (ItemStack s : player.getArmorItems())
            if (s.getItem() == item) return true;
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        switch (getType()) {
            case HELMET ->     tooltip.add(Text.literal("Видеть мобов сквозь блоки (64 блока)").formatted(Formatting.LIGHT_PURPLE));
            case CHESTPLATE -> tooltip.add(Text.literal("+2 строки HP (Поглощение II)").formatted(Formatting.LIGHT_PURPLE));
            case LEGGINGS ->   tooltip.add(Text.literal("Игнор урона от падения").formatted(Formatting.LIGHT_PURPLE));
            case BOOTS ->      tooltip.add(Text.literal("Скорость III · Невесомость · Вода/Лава").formatted(Formatting.LIGHT_PURPLE));
        }
        tooltip.add(Text.literal("Полный комплект: Полёт · +8 HP · Реген X · Огнестойкость").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("Ctrl (присесть) без меча = Воронка 500 блоков ☠").formatted(Formatting.DARK_RED));
        tooltip.add(Text.literal("Броню нельзя скрафтить — ищи в данже Хранитель Тьмы!").formatted(Formatting.GOLD, Formatting.ITALIC));
    }
}
