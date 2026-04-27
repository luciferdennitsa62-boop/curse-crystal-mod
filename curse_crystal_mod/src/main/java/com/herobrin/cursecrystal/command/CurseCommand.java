package com.herobrin.cursecrystal.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.herobrin.cursecrystal.items.CurseCrystalSword;

public class CurseCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess access,
                                CommandManager.RegistrationEnvironment env) {
        dispatcher.register(
            CommandManager.literal("cursecrystal")
                .then(CommandManager.literal("dropblocks")
                    .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                            ServerCommandSource source = ctx.getSource();

                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                source.sendError(Text.literal("Только для игроков!"));
                                return 0;
                            }

                            // Ищем меч в инвентаре
                            boolean found = false;
                            for (int i = 0; i < player.getInventory().size(); i++) {
                                ItemStack s = player.getInventory().getStack(i);
                                if (s.getItem() instanceof CurseCrystalSword) {
                                    NbtCompound nbt = s.getOrCreateNbt();
                                    nbt.putBoolean("CurseDropBlocks", enabled);
                                    found = true;
                                }
                            }

                            if (found) {
                                player.sendMessage(Text.literal("☠ Выпадение блоков (красный луч 666): " + (enabled ? "ВКЛ ✅" : "ВЫКЛ ❌"))
                                    .formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD), false);
                            } else {
                                player.sendMessage(Text.literal("Меч Тьмы не найден в инвентаре!")
                                    .formatted(Formatting.RED), false);
                            }
                            return 1;
                        })))
                .then(CommandManager.literal("recipe")
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return 0;
                        player.sendMessage(Text.literal("=== РЕЦЕПТ МЕЧА ТЬМЫ ===").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
                        player.sendMessage(Text.literal("Стол крафта 3x3:").formatted(Formatting.PURPLE), false);
                        player.sendMessage(Text.literal("[Обс] [Череп] [Крист]").formatted(Formatting.GRAY), false);
                        player.sendMessage(Text.literal("[Звезда] [Глаз] [Тотем]").formatted(Formatting.GRAY), false);
                        player.sendMessage(Text.literal("[Стержень] [КрЭдж] [Обс]").formatted(Formatting.GRAY), false);
                        player.sendMessage(Text.literal("Обс=Обсидиан | Череп=Череп вилтера | Крист=Кристалл Тьмы").formatted(Formatting.DARK_GRAY), false);
                        player.sendMessage(Text.literal("Звезда=Звезда Незера | Глаз=Глаз Края | Тотем=Тотем бессмертия").formatted(Formatting.DARK_GRAY), false);
                        player.sendMessage(Text.literal("КрЭдж=Кристалл Края | Стержень=Стержень Зажигателя").formatted(Formatting.DARK_GRAY), false);
                        player.sendMessage(Text.literal("Броню нельзя скрафтить — ищи в данже /locate structure cursecrystal:dark_keeper").formatted(Formatting.GOLD), false);
                        return 1;
                    }))
        );
    }
}
