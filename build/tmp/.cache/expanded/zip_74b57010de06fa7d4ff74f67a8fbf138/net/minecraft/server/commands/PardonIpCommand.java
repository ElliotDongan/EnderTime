package net.minecraft.server.commands;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.IpBanList;

public class PardonIpCommand {
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("commands.pardonip.invalid"));
    private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.pardonip.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> p_138109_) {
        p_138109_.register(
            Commands.literal("pardon-ip")
                .requires(Commands.hasPermission(3))
                .then(
                    Commands.argument("target", StringArgumentType.word())
                        .suggests(
                            (p_138113_, p_138114_) -> SharedSuggestionProvider.suggest(
                                p_138113_.getSource().getServer().getPlayerList().getIpBans().getUserList(), p_138114_
                            )
                        )
                        .executes(p_138111_ -> unban(p_138111_.getSource(), StringArgumentType.getString(p_138111_, "target")))
                )
        );
    }

    private static int unban(CommandSourceStack p_138118_, String p_138119_) throws CommandSyntaxException {
        if (!InetAddresses.isInetAddress(p_138119_)) {
            throw ERROR_INVALID.create();
        } else {
            IpBanList ipbanlist = p_138118_.getServer().getPlayerList().getIpBans();
            if (!ipbanlist.isBanned(p_138119_)) {
                throw ERROR_NOT_BANNED.create();
            } else {
                ipbanlist.remove(p_138119_);
                p_138118_.sendSuccess(() -> Component.translatable("commands.pardonip.success", p_138119_), true);
                return 1;
            }
        }
    }
}