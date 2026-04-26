package com.herobrin.cursecrystal.items;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
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

public class CurseCrystalSword extends SwordItem {

    // NBT ключ для хранения бонусного урона от убийств
    private static final String KILL_BONUS_KEY = "CurseCrystalKillBonus";

    private static final int   BEAM_COOLDOWN = 200;     // 10 секунд
    private static final int   BEAM_RANGE    = 128;     // блоков
    private static final float BEAM_DAMAGE   = 99999999f;
    private static final float KILL_BONUS    = 5.0f;    // +5 урона за убийство
    private static final int   BASE_DAMAGE   = 13;      // базовый урон меча

    public static final ToolMaterial CURSE_MATERIAL = new ToolMaterial() {
        @Override public int   getDurability()            { return 6666; }
        @Override public float getMiningSpeedMultiplier() { return 10.0f; }
        @Override public float getAttackDamage()          { return 8.0f; }  // 8+5=13 итого
        @Override public int   getMiningLevel()           { return 4; }
        @Override public int   getEnchantability()        { return 22; }
        @Override public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(ModItems.CURSE_CRYSTAL_GEM);
        }
    };

    public CurseCrystalSword() {
        super(CURSE_MATERIAL, 3, -2.4f, new Item.Settings().maxCount(1));
    }

    // ============================================================
    //  УБИЙСТВО — +5 урона в NBT меча
    // ============================================================
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Обычные эффекты при ударе
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,   60, 1));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));

        // Проверяем убийство
        if (target.getHealth() <= 0 && attacker instanceof PlayerEntity player) {
            // Добавляем +5 урона в NBT
            NbtCompound nbt = stack.getOrCreateNbt();
            float currentBonus = nbt.getFloat(KILL_BONUS_KEY);
            float newBonus = currentBonus + KILL_BONUS;
            nbt.putFloat(KILL_BONUS_KEY, newBonus);

            // Регенерация при убийстве
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 1));

            // Сообщение игроку
            int kills = (int)(newBonus / KILL_BONUS);
            player.sendMessage(
                Text.literal("☠ Убийств: " + kills + " | Урон меча: " + (BASE_DAMAGE + (int)newBonus))
                    .formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
                true // actionbar (над хотбаром)
            );

            // Частицы смерти
            if (!attacker.getWorld().isClient) {
                ServerWorld sw = (ServerWorld) attacker.getWorld();
                sw.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        target.getX(), target.getY() + 1, target.getZ(),
                        30, 0.5, 0.5, 0.5, 0.1);
            }
        }

        // Применяем бонусный урон из NBT
        NbtCompound nbt = stack.getOrCreateNbt();
        float bonus = nbt.getFloat(KILL_BONUS_KEY);
        if (bonus > 0) {
            target.damage(target.getDamageSources().magic(), bonus);
        }

        return super.postHit(stack, target, attacker);
    }

    // ============================================================
    //  В РУКЕ: Скорость 2 + Ночное зрение + Сила 2
    // ============================================================
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (selected && entity instanceof PlayerEntity player && !world.isClient) {
            // Скорость 2
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED, 40, 1, false, false));
            // Ночное зрение
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 300, 0, false, false));
            // Сила 2
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH, 40, 1, false, false));
        }
    }

    // ============================================================
    //  ПКМ — Крылья + Луч Апокалипсиса
    // ============================================================
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;

            // 1. Крылья за спиной
            spawnWingsAnimation(serverWorld, user);

            // 2. Луч апокалипсиса
            fireBeam(serverWorld, user);

            // 3. Огонь на игроке (визуал, Fire Resistance защищает)
            if (user instanceof ServerPlayerEntity sp) {
                sp.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.FIRE_RESISTANCE, 140, 0, false, false));
            }
            user.setOnFireFor(7);

            user.getItemCooldownManager().set(this, BEAM_COOLDOWN);
        }

        return TypedActionResult.success(stack);
    }

    // ============================================================
    //  КРЫЛЬЯ ИЗ ФИОЛЕТОВОГО ОГНЯ (вверх и вниз)
    // ============================================================
    private void spawnWingsAnimation(ServerWorld world, PlayerEntity player) {
        Vec3d origin = player.getPos().add(0, 1.1, 0);
        Vec3d look   = player.getRotationVector();
        // Перпендикуляр (право от игрока)
        Vec3d right  = new Vec3d(-look.z, 0, look.x).normalize();
        // Направление назад (противоположное взгляду)
        Vec3d back   = look.multiply(-1).normalize();

        DustParticleEffect purple     = new DustParticleEffect(new Vector3f(0.75f, 0.05f, 1.00f), 1.6f);
        DustParticleEffect brightPurp = new DustParticleEffect(new Vector3f(0.95f, 0.40f, 1.00f), 1.2f);
        DustParticleEffect darkPurp   = new DustParticleEffect(new Vector3f(0.45f, 0.00f, 0.70f), 1.8f);

        // 6 перьев с каждой стороны (право/лево), крылья вверх + вниз
        for (int wing = 0; wing < 2; wing++) {
            double yMirror = (wing == 0) ? 1.0 : -1.0;
            double yOffset = (wing == 0) ? 0.2 : -0.2;

            for (int side = -1; side <= 1; side += 2) {
                for (int feather = 1; feather <= 6; feather++) {
                    // Основание пера — за спиной игрока, расходится в стороны
                    double baseX = origin.x + back.x * 0.3 + right.x * side * feather * 0.25;
                    double baseZ = origin.z + back.z * 0.3 + right.z * side * feather * 0.25;
                    double baseY = origin.y + yOffset;

                    // Дуга пера из 14 точек
                    for (int p = 0; p < 14; p++) {
                        double t = p / 13.0;
                        // Расходится наружу-назад и вверх/вниз
                        double wx = baseX + back.x * t * 0.5 + right.x * side * t * 0.6;
                        double wy = baseY + yMirror * (t * (1.9 - feather * 0.16))
                                  + Math.sin(t * Math.PI) * 0.3 * yMirror;
                        double wz = baseZ + back.z * t * 0.5 + right.z * side * t * 0.6;

                        DustParticleEffect col = (p % 4 == 0) ? brightPurp
                                               : (p % 3 == 0) ? darkPurp : purple;
                        world.spawnParticles(col, wx, wy, wz, 1, 0.03, 0.03, 0.03, 0);

                        // Огненные частицы внутри перьев
                        if (p % 3 == 0) {
                            world.spawnParticles(ParticleTypes.FLAME, wx, wy, wz,
                                    1, 0.04, 0.04, 0.04, 0.01);
                        }
                    }
                }
            }
        }

        // Кольцо вокруг игрока
        for (int i = 0; i < 40; i++) {
            double angle = i * (2 * Math.PI / 40);
            double r = 1.5;
            double cx = player.getX() + Math.cos(angle) * r;
            double cz = player.getZ() + Math.sin(angle) * r;
            world.spawnParticles(ParticleTypes.END_ROD,
                    cx, player.getY() + 0.1, cz, 1, 0, 0.08, 0, 0.04);
            world.spawnParticles(ParticleTypes.DRAGON_BREATH,
                    cx, player.getY() + 0.1, cz, 1, 0.05, 0.1, 0.05, 0.02);
        }

        // Взрыв частиц
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 0.5, player.getZ(),
                150, 1.3, 1.8, 1.3, 0.08);
        world.spawnParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0, player.getZ(),
                50, 0.6, 0.9, 0.6, 0.15);
    }

    // ============================================================
    //  ФИОЛЕТОВЫЙ ЛУЧ АПОКАЛИПСИСА
    // ============================================================
    private void fireBeam(ServerWorld world, PlayerEntity player) {
        Vec3d start     = player.getEyePos();
        Vec3d direction = player.getRotationVector().normalize();

        DustParticleEffect beamCore = new DustParticleEffect(new Vector3f(0.90f, 0.25f, 1.00f), 2.2f);
        DustParticleEffect beamMid  = new DustParticleEffect(new Vector3f(0.60f, 0.00f, 0.85f), 1.8f);
        DustParticleEffect beamEdge = new DustParticleEffect(new Vector3f(0.40f, 0.00f, 0.60f), 1.4f);

        Vec3d perp1 = direction.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d perp2 = direction.crossProduct(perp1).normalize();

        for (double dist = 0; dist < BEAM_RANGE; dist += 0.4) {
            Vec3d point = start.add(direction.multiply(dist));

            // Ядро
            world.spawnParticles(beamCore, point.x, point.y, point.z, 1, 0.04, 0.04, 0.04, 0);
            // Внутренний ореол (8 точек)
            for (int ring = 0; ring < 8; ring++) {
                double a  = ring * (2 * Math.PI / 8);
                double rx = point.x + (Math.cos(a)*perp1.x + Math.sin(a)*perp2.x) * 0.25;
                double ry = point.y + (Math.cos(a)*perp1.y + Math.sin(a)*perp2.y) * 0.25;
                double rz = point.z + (Math.cos(a)*perp1.z + Math.sin(a)*perp2.z) * 0.25;
                world.spawnParticles(beamMid, rx, ry, rz, 1, 0.02, 0.02, 0.02, 0);
            }
            // Внешний ореол (6 точек)
            for (int ring = 0; ring < 6; ring++) {
                double a  = ring * (2 * Math.PI / 6);
                double rx = point.x + (Math.cos(a)*perp1.x + Math.sin(a)*perp2.x) * 0.5;
                double ry = point.y + (Math.cos(a)*perp1.y + Math.sin(a)*perp2.y) * 0.5;
                double rz = point.z + (Math.cos(a)*perp1.z + Math.sin(a)*perp2.z) * 0.5;
                world.spawnParticles(beamEdge, rx, ry, rz, 1, 0.02, 0.02, 0.02, 0);
            }

            if (dist % 2 < 0.5) {
                world.spawnParticles(ParticleTypes.DRAGON_BREATH,
                        point.x, point.y, point.z, 2, 0.2, 0.2, 0.2, 0.01);
            }

            // Ломаем блоки
            BlockPos blockPos = BlockPos.ofFloored(point.x, point.y, point.z);
            BlockState state  = world.getBlockState(blockPos);
            if (!state.isAir() && !state.isOf(Blocks.BEDROCK)) {
                world.breakBlock(blockPos, true, player);
                world.spawnParticles(ParticleTypes.EXPLOSION,
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }

            // Убиваем всех в радиусе 2 блоков
            Box hitBox = new Box(
                    point.x-2, point.y-2, point.z-2,
                    point.x+2, point.y+2, point.z+2);
            world.getEntitiesByClass(LivingEntity.class, hitBox, e -> e != player)
                    .forEach(living -> {
                        living.damage(world.getDamageSources().magic(), BEAM_DAMAGE);
                        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                                living.getX(), living.getY()+1, living.getZ(),
                                25, 0.5, 0.5, 0.5, 0.1);
                    });
        }

        // Финальный взрыв
        Vec3d end = start.add(direction.multiply(BEAM_RANGE));
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 5, 3, 3, 3, 0.2);
        world.spawnParticles(ParticleTypes.DRAGON_BREATH,     end.x, end.y, end.z, 150, 4, 4, 4, 0.15);
        world.spawnParticles(ParticleTypes.END_ROD,           end.x, end.y, end.z, 60, 2, 2, 2, 0.3);
    }

    // ============================================================
    //  ТУЛТИП (описание предмета)
    // ============================================================
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getOrCreateNbt();
        float bonus     = nbt.getFloat(KILL_BONUS_KEY);
        int   kills     = (int)(bonus / KILL_BONUS);
        int   totalDmg  = BASE_DAMAGE + (int)bonus;

        tooltip.add(Text.literal("⚔ Кристалл Проклятья")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        tooltip.add(Text.literal("Урон: " + totalDmg + " ❤  (убийств: " + kills + ")")
                .formatted(Formatting.RED));
        tooltip.add(Text.literal("Каждое убийство: +5 урона навсегда")
                .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("ПКМ ➜ Крылья + Луч Апокалипсиса")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        tooltip.add(Text.literal("  Луч: 99 999 999 урона | 128 блоков")
                .formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("  Ломает всё кроме бедрока")
                .formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("  Кулдаун: 10 секунд")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("В руке: Скорость II · Ночное зрение · Сила II")
                .formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("\"Проклятие вечно\"")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }
}
