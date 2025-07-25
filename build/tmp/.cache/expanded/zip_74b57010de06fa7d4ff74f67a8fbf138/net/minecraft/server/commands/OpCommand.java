package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class OpCommand {
    private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> p_138080_) {
        p_138080_.register(
            Commands.literal("op")
                .requires(Commands.hasPermission(3))
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests(
                            (p_138084_, p_138085_) -> {
                                PlayerList playerlist = p_138084_.getSource().getServer().getPlayerList();
                                return SharedSuggestionProvider.suggest(
                                    playerlist.getPlayers()
                                        .stream()
                                        .filter(p_405181_ -> !playerlist.isOp(p_405181_.getGameProfile()))
                                        .map(p_405182_ -> p_405182_.getGameProfile().getName()),
                                    p_138085_
                                );
                            }
                        )
                        .executes(p_138082_ -> opPlayers(p_138082_.getSource(), GameProfileArgument.getGameProfiles(p_138082_, "targets")))
                )
        );
    }

    private static int opPlayers(CommandSourceStack p_138089_, Collection<GameProfile> p_138090_) throws CommandSyntaxException {
        PlayerList playerlist = p_138089_.getServer().getPlayerList();
        int i = 0;

        for (GameProfile gameprofile : p_138090_) {
            if (!playerlist.isOp(gameprofile)) {
                playerlist.op(gameprofile);
                i++;
                p_138089_.sendSuccess(() -> Component.translatable("commands.op.success", gameprofile.getName()), true);
            }
        }

        if (i == 0) {
            throw ERROR_ALREADY_OP.create();
        } else {
            return i;
        }
    }
}