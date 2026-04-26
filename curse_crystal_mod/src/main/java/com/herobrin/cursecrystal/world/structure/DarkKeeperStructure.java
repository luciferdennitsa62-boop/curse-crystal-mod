package com.herobrin.cursecrystal.world.structure;

import com.herobrin.cursecrystal.CurseCrystalMod;
import com.herobrin.cursecrystal.items.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.WorldChunk;

public class DarkKeeperStructure {

    // Данж спавнится раз в ~800 чанков
    private static final int SPAWN_CHANCE = 800;

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            // Проверяем только в Верхнем мире
            if (!world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) return;

            BlockPos chunkPos = chunk.getPos().getStartPos();
            long seed = world.getSeed() ^ (chunkPos.getX() * 341873128712L) ^ (chunkPos.getZ() * 132897987541L);
            Random rand = Random.create(seed);

            // Шанс генерации
            if (rand.nextInt(SPAWN_CHANCE) != 0) return;

            // Ищем подходящую высоту
            int surfaceY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG,
                    chunkPos.getX() + 8, chunkPos.getZ() + 8) - 1;

            // Не генерируем в воде/лаве
            BlockPos centerPos = new BlockPos(chunkPos.getX() + 8, surfaceY, chunkPos.getZ() + 8);
            if (world.getFluidState(centerPos).isEmpty() == false) return;

            // Генерируем данж!
            generateDungeon(world, centerPos, rand);
            CurseCrystalMod.LOGGER.info("Данж 'Хранитель Тьмы' сгенерирован на: " + centerPos);
        });
    }

    private static void generateDungeon(ServerWorld world, BlockPos center, Random rand) {
        // ==========================================
        //  ГЛАВНАЯ БАШНЯ (центр)
        // ==========================================
        buildTower(world, center, rand);

        // ==========================================
        //  МАЛАЯ БАШНЯ (слева)
        // ==========================================
        BlockPos tower2 = center.add(-18, 0, -5);
        buildSmallTower(world, tower2, rand);

        // ==========================================
        //  МАЛАЯ БАШНЯ (справа)
        // ==========================================
        BlockPos tower3 = center.add(18, 0, 5);
        buildSmallTower(world, tower3, rand);

        // ==========================================
        //  СОЕДИНИТЕЛЬНЫЕ СТЕНЫ
        // ==========================================
        buildWall(world, center.add(-14, 0, -2), center.add(-4, 0, -2), rand);
        buildWall(world, center.add(4, 0, 2), center.add(14, 0, 2), rand);

        // ==========================================
        //  СЕКРЕТНАЯ КОМНАТА (подземная)
        // ==========================================
        BlockPos secretPos = center.add(0, -8, 0);
        buildSecretRoom(world, secretPos, rand);

        // Вход в секретную комнату — лестница из обсидиана
        for (int y = -1; y >= -8; y--) {
            world.setBlockState(center.add(0, y, 0), Blocks.LADDER.getDefaultState()
                    .with(LadderBlock.FACING, net.minecraft.util.math.Direction.NORTH));
        }
        // Закрываем вход крышкой из обсидиана (нужно разбить чтобы войти)
        world.setBlockState(center.add(0, -1, 0), Blocks.OBSIDIAN.getDefaultState());
    }

    // ==========================================
    //  ГЛАВНАЯ БАШНЯ
    // ==========================================
    private static void buildTower(ServerWorld world, BlockPos base, Random rand) {
        Block wall  = Blocks.BLACKSTONE;
        Block brick = Blocks.POLISHED_BLACKSTONE_BRICKS;
        Block cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS;

        // Фундамент 9x9
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                world.setBlockState(base.add(x, -1, z), Blocks.POLISHED_BLACKSTONE.getDefaultState());
                // Чистим землю внутри
                for (int y = 0; y <= 12; y++)
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());
            }

        // Стены башни высотой 12
        for (int y = 0; y <= 12; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    boolean isWall = (x == -4 || x == 4 || z == -4 || z == 4);
                    if (!isWall) continue;
                    Block b = rand.nextInt(5) == 0 ? cracked : (rand.nextInt(3) == 0 ? wall : brick);
                    world.setBlockState(base.add(x, y, z), b.getDefaultState());
                }
            }
        }

        // Крыша 9x9
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++)
                world.setBlockState(base.add(x, 13, z), brick.getDefaultState());

        // Зубцы на крыше
        for (int x = -4; x <= 4; x += 2) {
            world.setBlockState(base.add(x, 14, -4), brick.getDefaultState());
            world.setBlockState(base.add(x, 14, 4), brick.getDefaultState());
        }
        for (int z = -4; z <= 4; z += 2) {
            world.setBlockState(base.add(-4, 14, z), brick.getDefaultState());
            world.setBlockState(base.add(4, 14, z), brick.getDefaultState());
        }

        // Дверь (вход)
        world.setBlockState(base.add(0, 0, -4), Blocks.AIR.getDefaultState());
        world.setBlockState(base.add(0, 1, -4), Blocks.AIR.getDefaultState());
        world.setBlockState(base.add(0, 2, -4), Blocks.IRON_BARS.getDefaultState());

        // Факелы с душами
        world.setBlockState(base.add(-3, 4, -3), Blocks.SOUL_WALL_TORCH.getDefaultState()
                .with(WallTorchBlock.FACING, net.minecraft.util.math.Direction.EAST));
        world.setBlockState(base.add(3, 4, -3), Blocks.SOUL_WALL_TORCH.getDefaultState()
                .with(WallTorchBlock.FACING, net.minecraft.util.math.Direction.WEST));
        world.setBlockState(base.add(-3, 4, 3), Blocks.SOUL_WALL_TORCH.getDefaultState()
                .with(WallTorchBlock.FACING, net.minecraft.util.math.Direction.EAST));
        world.setBlockState(base.add(3, 4, 3), Blocks.SOUL_WALL_TORCH.getDefaultState()
                .with(WallTorchBlock.FACING, net.minecraft.util.math.Direction.WEST));

        // Котёл с лавой в центре
        world.setBlockState(base.add(0, 0, 2), Blocks.CAULDRON.getDefaultState());

        // Сундук с обычным лутом
        BlockPos chestPos = base.add(2, 0, 2);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, net.minecraft.util.math.Direction.NORTH));
        fillChestNormal(world, chestPos, rand);

        // Кристаллы тьмы на полу
        for (int i = 0; i < 3; i++) {
            int cx = rand.nextBetween(-3, 3);
            int cz = rand.nextBetween(-3, 3);
            world.setBlockState(base.add(cx, 0, cz), Blocks.AMETHYST_CLUSTER.getDefaultState());
        }

        // Стойка с бронёй (алтарь)
        world.setBlockState(base.add(-2, 0, 0), Blocks.STONE_BRICKS.getDefaultState());
        world.setBlockState(base.add(-2, 1, 0), Blocks.ARMOR_STAND.getDefaultState());
    }

    // ==========================================
    //  МАЛАЯ БАШНЯ
    // ==========================================
    private static void buildSmallTower(ServerWorld world, BlockPos base, Random rand) {
        Block brick = Blocks.POLISHED_BLACKSTONE_BRICKS;
        Block cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS;

        // Чистим место
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                for (int y = 0; y <= 8; y++)
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());

        // Стены 7x7 высота 8
        for (int y = 0; y <= 8; y++)
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++) {
                    if (x != -3 && x != 3 && z != -3 && z != 3) continue;
                    Block b = rand.nextInt(4) == 0 ? cracked : brick;
                    world.setBlockState(base.add(x, y, z), b.getDefaultState());
                }

        // Крыша
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                world.setBlockState(base.add(x, 9, z), brick.getDefaultState());

        // Дверь
        world.setBlockState(base.add(0, 0, -3), Blocks.AIR.getDefaultState());
        world.setBlockState(base.add(0, 1, -3), Blocks.AIR.getDefaultState());

        // Факелы
        world.setBlockState(base.add(-2, 3, -2), Blocks.SOUL_WALL_TORCH.getDefaultState()
                .with(WallTorchBlock.FACING, net.minecraft.util.math.Direction.EAST));
        world.setBlockState(base.add(2, 3, -2), Blocks.SOUL_WALL_TORCH.getDefaultState()
                .with(WallTorchBlock.FACING, net.minecraft.util.math.Direction.WEST));

        // Сундук с редким лутом
        BlockPos chestPos = base.add(0, 0, 1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, net.minecraft.util.math.Direction.NORTH));
        fillChestRare(world, chestPos, rand);

        // Кристалл на подставке
        world.setBlockState(base.add(0, 0, -1), Blocks.CRYING_OBSIDIAN.getDefaultState());
        world.setBlockState(base.add(0, 1, -1), Blocks.AMETHYST_CLUSTER.getDefaultState());
    }

    // ==========================================
    //  СОЕДИНИТЕЛЬНАЯ СТЕНА
    // ==========================================
    private static void buildWall(ServerWorld world, BlockPos from, BlockPos to, Random rand) {
        Block brick = Blocks.POLISHED_BLACKSTONE_BRICKS;
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        BlockPos cur = from;
        while (!cur.equals(to)) {
            for (int y = 0; y <= 4; y++)
                world.setBlockState(cur.add(0, y, 0), brick.getDefaultState());
            // Зубцы
            if (rand.nextInt(2) == 0)
                world.setBlockState(cur.add(0, 5, 0), brick.getDefaultState());
            cur = cur.add(dx, 0, dz);
        }
    }

    // ==========================================
    //  СЕКРЕТНАЯ КОМНАТА (подземная)
    // ==========================================
    private static void buildSecretRoom(ServerWorld world, BlockPos base, Random rand) {
        Block wall   = Blocks.DEEPSLATE_BRICKS;
        Block cracked = Blocks.CRACKED_DEEPSLATE_BRICKS;
        Block tile   = Blocks.DEEPSLATE_TILES;

        // Чистим комнату 13x13x5
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                for (int y = 0; y <= 5; y++)
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());

        // Пол
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                Block b = rand.nextInt(5) == 0 ? cracked : (rand.nextInt(3)==0 ? tile : wall);
                world.setBlockState(base.add(x, -1, z), b.getDefaultState());
            }

        // Стены и потолок
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                for (int y = 0; y <= 5; y++) {
                    boolean isBorder = (x==-6||x==6||z==-6||z==6||y==5);
                    if (!isBorder) continue;
                    Block b = rand.nextInt(5)==0 ? cracked : (rand.nextInt(3)==0 ? tile : wall);
                    world.setBlockState(base.add(x, y, z), b.getDefaultState());
                }

        // Освещение — душевые фонари
        world.setBlockState(base.add(-4, 4, -4), Blocks.SOUL_LANTERN.getDefaultState());
        world.setBlockState(base.add(4, 4, -4), Blocks.SOUL_LANTERN.getDefaultState());
        world.setBlockState(base.add(-4, 4, 4), Blocks.SOUL_LANTERN.getDefaultState());
        world.setBlockState(base.add(4, 4, 4), Blocks.SOUL_LANTERN.getDefaultState());
        world.setBlockState(base.add(0, 4, 0), Blocks.SOUL_LANTERN.getDefaultState());

        // Алтарь с бронёй (4 стойки)
        // Подставки
        world.setBlockState(base.add(-3, 0, -2), Blocks.STONE_BRICK_SLAB.getDefaultState());
        world.setBlockState(base.add(-1, 0, -2), Blocks.STONE_BRICK_SLAB.getDefaultState());
        world.setBlockState(base.add(1, 0, -2), Blocks.STONE_BRICK_SLAB.getDefaultState());
        world.setBlockState(base.add(3, 0, -2), Blocks.STONE_BRICK_SLAB.getDefaultState());

        // Стойки для доспехов
        world.setBlockState(base.add(-3, 1, -2), Blocks.ARMOR_STAND.getDefaultState());
        world.setBlockState(base.add(-1, 1, -2), Blocks.ARMOR_STAND.getDefaultState());
        world.setBlockState(base.add(1, 1, -2), Blocks.ARMOR_STAND.getDefaultState());
        world.setBlockState(base.add(3, 1, -2), Blocks.ARMOR_STAND.getDefaultState());

        // Надпись над алтарём
        world.setBlockState(base.add(0, 3, -5), Blocks.CHISELED_DEEPSLATE.getDefaultState());

        // Кристаллы тьмы — спавн только тут
        for (int i = 0; i < 8; i++) {
            int cx = rand.nextBetween(-5, 5);
            int cz = rand.nextBetween(-5, 5);
            world.setBlockState(base.add(cx, 0, cz), Blocks.AMETHYST_CLUSTER.getDefaultState());
        }
        // Кристаллы на стенах
        world.setBlockState(base.add(-6, 2, 0), Blocks.BUDDING_AMETHYST.getDefaultState());
        world.setBlockState(base.add(6, 2, 0), Blocks.BUDDING_AMETHYST.getDefaultState());
        world.setBlockState(base.add(0, 2, -6), Blocks.BUDDING_AMETHYST.getDefaultState());
        world.setBlockState(base.add(0, 2, 6), Blocks.BUDDING_AMETHYST.getDefaultState());

        // Портал из плача обсидиана по углам
        world.setBlockState(base.add(-5, 0, -5), Blocks.CRYING_OBSIDIAN.getDefaultState());
        world.setBlockState(base.add(5, 0, -5), Blocks.CRYING_OBSIDIAN.getDefaultState());
        world.setBlockState(base.add(-5, 0, 5), Blocks.CRYING_OBSIDIAN.getDefaultState());
        world.setBlockState(base.add(5, 0, 5), Blocks.CRYING_OBSIDIAN.getDefaultState());

        // ===== СУНДУКИ =====

        // Сундук 1 — обычный лут
        BlockPos chest1 = base.add(-5, 0, 3);
        world.setBlockState(chest1, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, net.minecraft.util.math.Direction.EAST));
        fillChestNormal(world, chest1, rand);

        // Сундук 2 — редкий лут
        BlockPos chest2 = base.add(5, 0, 3);
        world.setBlockState(chest2, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, net.minecraft.util.math.Direction.WEST));
        fillChestRare(world, chest2, rand);

        // Сундук 3 — кристаллы тьмы
        BlockPos chest3 = base.add(0, 0, 5);
        world.setBlockState(chest3, Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, net.minecraft.util.math.Direction.NORTH));
        fillChestCrystals(world, chest3, rand);

        // Сундук 4 — КАРТА С РЕЦЕПТОМ (только один, спрятан за стеной)
        world.setBlockState(base.add(-5, 0, -5), Blocks.CRYING_OBSIDIAN.getDefaultState()); // убираем обсидиан
        BlockPos secretChest = base.add(-5, 0, -4);
        world.setBlockState(secretChest, Blocks.TRAPPED_CHEST.getDefaultState()
                .with(ChestBlock.FACING, net.minecraft.util.math.Direction.EAST));
        fillChestRecipeMap(world, secretChest, rand);
    }

    // ==========================================
    //  НАПОЛНЕНИЕ СУНДУКОВ
    // ==========================================

    // Обычный лут
    private static void fillChestNormal(ServerWorld world, BlockPos pos, Random rand) {
        if (!(world.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        chest.setStack(0, new ItemStack(Items.IRON_INGOT, rand.nextBetween(2, 8)));
        chest.setStack(1, new ItemStack(Items.GOLD_INGOT, rand.nextBetween(1, 4)));
        chest.setStack(2, new ItemStack(Items.BONE, rand.nextBetween(3, 10)));
        chest.setStack(3, new ItemStack(Items.ROTTEN_FLESH, rand.nextBetween(2, 6)));
        chest.setStack(4, new ItemStack(Items.ARROW, rand.nextBetween(5, 20)));
        chest.setStack(5, new ItemStack(Items.BREAD, rand.nextBetween(2, 5)));
        chest.setStack(6, new ItemStack(ModItems.CURSE_CRYSTAL_GEM, rand.nextBetween(1, 3)));
    }

    // Редкий лут
    private static void fillChestRare(ServerWorld world, BlockPos pos, Random rand) {
        if (!(world.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        chest.setStack(0, new ItemStack(Items.DIAMOND, rand.nextBetween(1, 3)));
        chest.setStack(1, new ItemStack(Items.EMERALD, rand.nextBetween(2, 5)));
        chest.setStack(2, new ItemStack(Items.BLAZE_ROD, rand.nextBetween(2, 6)));
        chest.setStack(3, new ItemStack(Items.ENDER_PEARL, rand.nextBetween(1, 4)));
        chest.setStack(4, new ItemStack(Items.GOLDEN_APPLE, 1));
        chest.setStack(5, new ItemStack(ModItems.CURSE_CRYSTAL_GEM, rand.nextBetween(3, 7)));
        chest.setStack(6, new ItemStack(Items.OBSIDIAN, rand.nextBetween(4, 8)));
        chest.setStack(7, new ItemStack(Items.EXPERIENCE_BOTTLE, rand.nextBetween(2, 5)));
    }

    // Только кристаллы тьмы
    private static void fillChestCrystals(ServerWorld world, BlockPos pos, Random rand) {
        if (!(world.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        chest.setStack(0, new ItemStack(ModItems.CURSE_CRYSTAL_GEM, rand.nextBetween(5, 15)));
        chest.setStack(1, new ItemStack(ModItems.CURSE_CRYSTAL_GEM, rand.nextBetween(3, 10)));
        chest.setStack(2, new ItemStack(Items.AMETHYST_SHARD, rand.nextBetween(8, 20)));
        chest.setStack(3, new ItemStack(Items.ECHO_SHARD, rand.nextBetween(1, 3)));
        chest.setStack(4, new ItemStack(Items.NETHER_STAR, 1));
    }

    // Карта с рецептом крафта меча
    private static void fillChestRecipeMap(ServerWorld world, BlockPos pos, Random rand) {
        if (!(world.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;

        // Карта-рецепт — заполненная карта с описанием
        ItemStack recipeMap = new ItemStack(Items.FILLED_MAP);
        NbtCompound nbt = recipeMap.getOrCreateNbt();
        nbt.putInt("map", 999);
        recipeMap.setCustomName(Text.literal("★ Рецепт: Меч Тьмы ★").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));

        // Книга с рецептом (описание крафта)
        ItemStack recipeBook = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound bookNbt = recipeBook.getOrCreateNbt();
        bookNbt.putString("title", "Рецепт Меча Тьмы");
        bookNbt.putString("author", "Хранитель Тьмы");
        net.minecraft.nbt.NbtList pages = new net.minecraft.nbt.NbtList();
        pages.add(net.minecraft.nbt.NbtString.of(
            Text.Serialization.toJsonString(
                Text.literal("=== МЕCH ТЬМЫ ===\n\nКрафт 3x3:\n\n[Обс][Череп][Крист]\n[Звезда][Глаз][Тотем]\n[Стержень][КрЭдж][Обс]\n\nОбс = Обсидиан\nЧереп = Череп вилтера\nКрист = Кристалл Тьмы\nЗвезда = Звезда Незера\nГлаз = Глаз Края\nТотем = Тотем бессмертия\nКрЭдж = Кристалл Края\n\nБроню нельзя скрафтить.\nИщи на стойках!"),
                world.getRegistryManager()
            )
        ));
        bookNbt.put("pages", pages);
        recipeBook.setNbt(bookNbt);

        chest.setStack(0, recipeMap);
        chest.setStack(1, recipeBook);
        chest.setStack(2, new ItemStack(ModItems.CURSE_CRYSTAL_GEM, rand.nextBetween(10, 20)));
        chest.setStack(3, new ItemStack(Items.NETHER_STAR, rand.nextBetween(1, 2)));
        chest.setStack(4, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
    }
}
