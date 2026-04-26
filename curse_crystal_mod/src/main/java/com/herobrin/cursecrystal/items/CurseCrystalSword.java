package com.herobrin.cursecrystal.items;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
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

    private static final String KILL_BONUS_KEY = "CurseCrystalKillBonus";
    private static final String KILL_COUNT_KEY = "CurseCrystalKills";
    private static final String CHARGING_KEY   = "CurseCharging";
    private static final String CHARGE_TICK    = "CurseChargeTick";

    private static final int   BEAM_COOLDOWN   = 200;
    private static final int   BEAM_RANGE      = 128;
    private static final float BEAM_DAMAGE     = 99999999f;
    private static final float KILL_BONUS      = 5.0f;
    private static final int   BASE_DAMAGE     = 13;
    private static final int   KILL_THRESHOLD  = 666; // порог апокалипсиса

    public static final ToolMaterial CURSE_MATERIAL = new ToolMaterial() {
        @Override public int   getDurability()            { return 6666; }
        @Override public float getMiningSpeedMultiplier() { return 10.0f; }
        @Override public float getAttackDamage()          { return 8.0f; }
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
    //  ПКМ — начать заряд
    // ============================================================
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if (!world.isClient) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putBoolean(CHARGING_KEY, true);
            nbt.putInt(CHARGE_TICK, 0);
            int kills = nbt.getInt(KILL_COUNT_KEY);
            if (kills >= KILL_THRESHOLD) {
                user.sendMessage(Text.literal("☠ РЕЖИМ 666 АКТИВЕН — ЗАРЯЖАЮ...").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
            } else {
                user.sendMessage(Text.literal("⚡ Заряжаю... [" + kills + "/666]").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), true);
            }
        }
        return TypedActionResult.consume(stack);
    }

    // ============================================================
    //  TICK — заряд + выстрел
    // ============================================================
    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (selected && entity instanceof PlayerEntity player && !world.isClient) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        40, 1, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300,0, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,     40, 1, false, false));
        }
        if (!selected || !(entity instanceof PlayerEntity player) || world.isClient) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.getBoolean(CHARGING_KEY)) return;

        int tick  = nbt.getInt(CHARGE_TICK) + 1;
        nbt.putInt(CHARGE_TICK, tick);
        int kills = nbt.getInt(KILL_COUNT_KEY);
        boolean apocalypse = kills >= KILL_THRESHOLD;

        ServerWorld sw = (ServerWorld) world;
        spawnChargeParticles(sw, player, tick, apocalypse);

        if (tick >= 40) {
            nbt.putBoolean(CHARGING_KEY, false);
            nbt.putInt(CHARGE_TICK, 0);
            spawnWingsAnimation(sw, player, apocalypse);
            fireBeam(sw, player, stack, apocalypse);
            if (player instanceof ServerPlayerEntity sp) {
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 140, 0, false, false));
            }
            player.setOnFireFor(7);
            player.getItemCooldownManager().set(this, BEAM_COOLDOWN);
        }
    }

    // ============================================================
    //  АНИМАЦИЯ ЗАРЯДА
    // ============================================================
    private void spawnChargeParticles(ServerWorld world, PlayerEntity player, int tick, boolean apocalypse) {
        DustParticleEffect col1 = apocalypse
                ? new DustParticleEffect(new Vector3f(1.0f, 0.05f, 0.05f), 2.0f)
                : new DustParticleEffect(new Vector3f(0.75f, 0.05f, 1.0f), 1.6f);
        DustParticleEffect col2 = apocalypse
                ? new DustParticleEffect(new Vector3f(1.0f, 0.4f, 0.1f), 1.5f)
                : new DustParticleEffect(new Vector3f(0.95f, 0.40f, 1.0f), 1.2f);

        double r = 4.0 - 3.7 * (tick / 40.0);
        int count = 10 + tick / 2;

        for (int i = 0; i < count; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi   = Math.random() * Math.PI;
            double px = player.getX() + r * Math.sin(phi) * Math.cos(theta);
            double py = player.getY() + 1.0 + r * Math.cos(phi);
            double pz = player.getZ() + r * Math.sin(phi) * Math.sin(theta);
            world.spawnParticles((i%2==0)?col1:col2, px, py, pz, 1, 0.02, 0.02, 0.02, 0);
        }

        for (int ring = 0; ring < 3; ring++) {
            double a0 = ring*(Math.PI/3) + tick*0.2;
            for (int p = 0; p < 16; p++) {
                double a  = p*(2*Math.PI/16);
                double rx = player.getX() + r*Math.cos(a)*Math.cos(a0);
                double ry = player.getY() + 1.0 + r*Math.sin(a);
                double rz = player.getZ() + r*Math.cos(a)*Math.sin(a0);
                world.spawnParticles(ParticleTypes.END_ROD, rx, ry, rz, 1, 0, 0, 0, 0);
            }
        }
        if (tick > 25) {
            world.spawnParticles(ParticleTypes.DRAGON_BREATH,
                    player.getX(), player.getY()+1, player.getZ(), 20, 0.4, 0.4, 0.4, 0.06);
        }
    }

    // ============================================================
    //  КРЫЛЬЯ (обычные + апокалипсис)
    // ============================================================
    private void spawnWingsAnimation(ServerWorld world, PlayerEntity player, boolean apocalypse) {
        Vec3d origin = player.getPos().add(0, 1.1, 0);
        Vec3d look   = player.getRotationVector();
        Vec3d right  = new Vec3d(-look.z, 0, look.x).normalize();
        Vec3d back   = look.multiply(-1).normalize();

        DustParticleEffect c1 = apocalypse
                ? new DustParticleEffect(new Vector3f(1.0f, 0.05f, 0.05f), 2.2f)
                : new DustParticleEffect(new Vector3f(0.75f, 0.05f, 1.00f), 2.0f);
        DustParticleEffect c2 = apocalypse
                ? new DustParticleEffect(new Vector3f(1.0f, 0.50f, 0.10f), 1.8f)
                : new DustParticleEffect(new Vector3f(0.98f, 0.50f, 1.00f), 1.8f);
        DustParticleEffect c3 = apocalypse
                ? new DustParticleEffect(new Vector3f(0.6f, 0.00f, 0.00f), 2.4f)
                : new DustParticleEffect(new Vector3f(0.40f, 0.00f, 0.65f), 2.2f);

        int feathers = apocalypse ? 16 : 12;
        int points   = apocalypse ? 28 : 22;

        for (int wing = 0; wing < 2; wing++) {
            double yMirror = (wing == 0) ? 1.0 : -1.0;
            double yOffset = (wing == 0) ? 0.3 : -0.3;
            for (int side = -1; side <= 1; side += 2) {
                for (int feather = 1; feather <= feathers; feather++) {
                    double spread = feather * (apocalypse ? 0.42 : 0.38);
                    double bx = origin.x + back.x*0.5 + right.x*side*spread;
                    double bz = origin.z + back.z*0.5 + right.z*side*spread;
                    double by = origin.y + yOffset;
                    for (int layer = 0; layer < 3; layer++) {
                        double lOff = (layer-1)*0.12;
                        for (int p = 0; p < points; p++) {
                            double t  = p/(double)(points-1);
                            double wx = bx + back.x*t*1.4 + right.x*(side*t*1.6+lOff);
                            double wy = by + yMirror*(t*(5.5-feather*0.20))
                                       + Math.sin(t*Math.PI)*1.0*yMirror
                                       + Math.sin(t*Math.PI*2)*0.2*yMirror;
                            double wz = bz + back.z*t*1.4 + right.z*(side*t*1.6+lOff);

                            DustParticleEffect col = (p==0||p==points-1)?c2:(p%5==0)?c2:(p%3==0)?c3:c1;
                            world.spawnParticles(col, wx, wy, wz, 1, 0.02, 0.02, 0.02, 0);
                            if (p%2==0) world.spawnParticles(ParticleTypes.FLAME, wx, wy, wz, 1, 0.05, 0.05, 0.05, 0.02);
                            if (apocalypse && p>points-5)
                                world.spawnParticles(ParticleTypes.EXPLOSION, wx, wy, wz, 1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
            }
        }

        // Кольца + взрыв
        for (int ring = 0; ring < 6; ring++) {
            double r = 1.0 + ring*0.9;
            int pts  = 28 + ring*8;
            for (int i = 0; i < pts; i++) {
                double a  = i*(2*Math.PI/pts);
                double cx = player.getX() + Math.cos(a)*r;
                double cz = player.getZ() + Math.sin(a)*r;
                world.spawnParticles(ParticleTypes.END_ROD,       cx, player.getY()+0.5, cz, 1, 0, 0.12, 0, 0.06);
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, cx, player.getY()+0.5, cz, 1, 0.05, 0.15, 0.05, 0.03);
            }
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,    player.getX(), player.getY()+1, player.getZ(), 500, 3.0, 4.0, 3.0, 0.15);
        world.spawnParticles(ParticleTypes.END_ROD,           player.getX(), player.getY()+1, player.getZ(), 200, 2.0, 3.0, 2.0, 0.30);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY()+1, player.getZ(), 3,   1.5, 1.5, 1.5, 0.05);
    }

    // ============================================================
    //  ЛУЧ СМЕРТИ
    // ============================================================
    private void fireBeam(ServerWorld world, PlayerEntity player, ItemStack stack, boolean apocalypse) {
        Vec3d start     = player.getEyePos();
        Vec3d direction = player.getRotationVector().normalize();

        // Апокалипсис = красно-белый луч, обычный = фиолетовый
        DustParticleEffect core, mid, edge, outer;
        if (apocalypse) {
            core  = new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 3.0f);
            mid   = new DustParticleEffect(new Vector3f(1.0f, 0.3f, 0.1f), 2.5f);
            edge  = new DustParticleEffect(new Vector3f(0.8f, 0.0f, 0.0f), 2.0f);
            outer = new DustParticleEffect(new Vector3f(0.5f, 0.0f, 0.0f), 1.5f);
        } else {
            core  = new DustParticleEffect(new Vector3f(0.95f, 0.30f, 1.00f), 2.8f);
            mid   = new DustParticleEffect(new Vector3f(0.70f, 0.05f, 0.90f), 2.2f);
            edge  = new DustParticleEffect(new Vector3f(0.45f, 0.00f, 0.65f), 1.8f);
            outer = new DustParticleEffect(new Vector3f(0.25f, 0.00f, 0.45f), 1.4f);
        }

        Vec3d perp1 = direction.crossProduct(new Vec3d(0,1,0)).normalize();
        Vec3d perp2 = direction.crossProduct(perp1).normalize();

        // Эффект тряски экрана — даём Тошноту которая трясёт экран
        if (player instanceof ServerPlayerEntity sp) {
            if (apocalypse) {
                // Сильная тряска
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 5, false, false));
                // Белая вспышка через GameStateChange пакет
                sp.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                        GameStateChangeS2CPacket.GAME_WON, 1.0f)); // экран становится белым как при победе
            } else {
                // Лёгкая тряска
                sp.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 30, 2, false, false));
            }
        }

        int beamSize = apocalypse ? 2 : 1; // 5x5 или 3x3

        for (double dist = 0; dist < BEAM_RANGE; dist += 0.35) {
            Vec3d point = start.add(direction.multiply(dist));

            // Ядро луча
            world.spawnParticles(core, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0);

            // Ореолы (апокалипсис = толще и ярче)
            int rings = apocalypse ? 4 : 3;
            double[] radii = apocalypse ? new double[]{0.3, 0.6, 0.9, 1.3} : new double[]{0.25, 0.5, 0.75};
            DustParticleEffect[] cols = apocalypse
                    ? new DustParticleEffect[]{core, mid, edge, outer}
                    : new DustParticleEffect[]{mid, edge, outer};

            for (int ri = 0; ri < rings; ri++) {
                int pts = 8 + ri*4;
                for (int r = 0; r < pts; r++) {
                    double a  = r*(2*Math.PI/pts) + dist*0.3;
                    double rx = point.x+(Math.cos(a)*perp1.x+Math.sin(a)*perp2.x)*radii[ri];
                    double ry = point.y+(Math.cos(a)*perp1.y+Math.sin(a)*perp2.y)*radii[ri];
                    double rz = point.z+(Math.cos(a)*perp1.z+Math.sin(a)*perp2.z)*radii[ri];
                    world.spawnParticles(cols[ri], rx, ry, rz, 1, 0.01, 0.01, 0.01, 0);
                }
            }

            // Dragon breath + End rod
            if (dist%1.5<0.4) {
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, point.x, point.y, point.z, 3, 0.3, 0.3, 0.3, 0.02);
                if (apocalypse)
                    world.spawnParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z, 1, 0.1, 0.1, 0.1, 0);
            }
            if (dist%2<0.4)
                world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 2, 0.15, 0.15, 0.15, 0.1);

            // === ЛОМАЕМ БЛОКИ ===
            BlockPos center = BlockPos.ofFloored(point.x, point.y, point.z);
            for (int dx = -beamSize; dx <= beamSize; dx++) {
                for (int dy = -beamSize; dy <= beamSize; dy++) {
                    for (int dz = -beamSize; dz <= beamSize; dz++) {
                        BlockPos bp = center.add(dx, dy, dz);
                        var bs = world.getBlockState(bp);
                        // Апокалипсис ломает бедрок, обычный — нет
                        if (!bs.isAir() && (apocalypse || !bs.isOf(Blocks.BEDROCK))) {
                            world.breakBlock(bp, true, player);
                            if (apocalypse && Math.random() < 0.3)
                                world.spawnParticles(ParticleTypes.EXPLOSION, bp.getX(), bp.getY(), bp.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                }
            }

            // === УБИВАЕМ ВСЕХ ===
            double hitR = apocalypse ? 3.5 : 2.5;
            Box hitBox = new Box(point.x-hitR, point.y-hitR, point.z-hitR, point.x+hitR, point.y+hitR, point.z+hitR);
            world.getEntitiesByClass(LivingEntity.class, hitBox, e -> e != player)
                    .forEach(l -> {
                        l.damage(world.getDamageSources().magic(), BEAM_DAMAGE);
                        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                                l.getX(), l.getY()+1, l.getZ(), 30, 0.6, 0.6, 0.6, 0.12);
                    });
        }

        // === ФИНАЛЬНЫЙ ВЗРЫВ ===
        Vec3d end = start.add(direction.multiply(BEAM_RANGE));
        if (apocalypse) {
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 15, 5, 5, 5, 0.3);
            world.spawnParticles(ParticleTypes.DRAGON_BREATH,     end.x, end.y, end.z, 300, 6, 6, 6, 0.2);
            world.spawnParticles(ParticleTypes.END_ROD,           end.x, end.y, end.z, 150, 4, 4, 4, 0.4);
            // Дополнительные взрывы вдоль всего луча
            for (int i = 0; i < 10; i++) {
                Vec3d ep = start.add(direction.multiply(BEAM_RANGE * i / 10.0));
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, ep.x, ep.y, ep.z, 2, 1, 1, 1, 0.1);
            }
        } else {
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 5, 3, 3, 3, 0.2);
            world.spawnParticles(ParticleTypes.DRAGON_BREATH,     end.x, end.y, end.z, 150, 4, 4, 4, 0.15);
            world.spawnParticles(ParticleTypes.END_ROD,           end.x, end.y, end.z, 60, 2, 2, 2, 0.3);
        }
    }

    // ============================================================
    //  УДАР + СЧЁТЧИК УБИЙСТВ
    // ============================================================
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,   60, 1));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100,1));

        if (target.getHealth() <= 0 && attacker instanceof PlayerEntity player) {
            NbtCompound nbt = stack.getOrCreateNbt();

            // +5 урона
            float bonus = nbt.getFloat(KILL_BONUS_KEY) + KILL_BONUS;
            nbt.putFloat(KILL_BONUS_KEY, bonus);

            // Счётчик убийств
            int kills = nbt.getInt(KILL_COUNT_KEY) + 1;
            nbt.putInt(KILL_COUNT_KEY, kills);

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 1));

            // Сообщения
            if (kills == KILL_THRESHOLD) {
                // РАЗБЛОКИРОВКА РЕЖИМА 666!
                player.sendMessage(Text.literal(""), false);
                player.sendMessage(Text.literal("☠☠☠ 666 УБИЙСТВ ☠☠☠").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                player.sendMessage(Text.literal("ЛУЧ СМЕРТИ РАЗБЛОКИРОВАН!").formatted(Formatting.RED, Formatting.BOLD), false);
                player.sendMessage(Text.literal("Луч теперь ломает 5x5 блоков включая бедрок").formatted(Formatting.DARK_RED), false);
                // Взрыв частиц вокруг игрока
                if (!player.getWorld().isClient) {
                    ServerWorld sw = (ServerWorld) player.getWorld();
                    sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY()+1, player.getZ(), 10, 2, 2, 2, 0.1);
                    sw.spawnParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY()+1, player.getZ(), 200, 2, 2, 2, 0.15);
                }
            } else if (kills < KILL_THRESHOLD) {
                player.sendMessage(Text.literal("☠ " + kills + "/666 | Урон: " + (BASE_DAMAGE+(int)bonus))
                        .formatted(Formatting.DARK_PURPLE, Formatting.BOLD), true);
            } else {
                player.sendMessage(Text.literal("☠ " + kills + " убийств | РЕЖИМ 666 АКТИВЕН | Урон: " + (BASE_DAMAGE+(int)bonus))
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
            }

            if (!attacker.getWorld().isClient) {
                ((ServerWorld)attacker.getWorld()).spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        target.getX(), target.getY()+1, target.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            }
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        float bonus = nbt.getFloat(KILL_BONUS_KEY);
        if (bonus > 0) target.damage(target.getDamageSources().magic(), bonus);
        return super.postHit(stack, target, attacker);
    }

    // ============================================================
    //  ТУЛТИП
    // ============================================================
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        NbtCompound nbt   = stack.getOrCreateNbt();
        float bonus       = nbt.getFloat(KILL_BONUS_KEY);
        int   kills       = nbt.getInt(KILL_COUNT_KEY);
        int   totalDmg    = BASE_DAMAGE + (int)bonus;
        boolean apo       = kills >= KILL_THRESHOLD;

        tooltip.add(Text.literal("⚔ Меч Тьмы").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        if (apo) {
            tooltip.add(Text.literal("☠ РЕЖИМ 666 АКТИВЕН ☠").formatted(Formatting.DARK_RED, Formatting.BOLD));
        }
        tooltip.add(Text.literal("Урон: " + totalDmg + " ❤  | Убийств: " + kills + (apo ? " ☠" : "/666")).formatted(Formatting.RED));
        tooltip.add(Text.literal("Каждое убийство: +5 урона навсегда").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("ПКМ ➜ 2 сек заряд ➜ Крылья + Луч").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        if (apo) {
            tooltip.add(Text.literal("  ЛУЧ СМЕРТИ: 5x5 блоков | Бедрок | Тряска + вспышка").formatted(Formatting.DARK_RED));
        } else {
            tooltip.add(Text.literal("  Луч: 3x3 блока | 128 блоков | 99,999,999 урона").formatted(Formatting.DARK_PURPLE));
            tooltip.add(Text.literal("  До 666 убийств — стандартный режим").formatted(Formatting.GRAY));
        }
        tooltip.add(Text.literal("В руке: Скорость II · Ночное зрение · Сила II").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("\"666 убийств — и мир содрогнётся\"").formatted(Formatting.DARK_RED, Formatting.ITALIC));
    }
}
