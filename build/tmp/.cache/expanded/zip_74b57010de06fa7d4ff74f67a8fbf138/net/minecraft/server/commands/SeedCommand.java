package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class SeedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> p_138590_, boolean p_138591_) {
        p_138590_.register(Commands.literal("seed").requires(Commands.hasPermission(p_138591_ ? 2 : 0)).executes(p_288608_ -> {
            long i = p_288608_.getSource().getLevel().getSeed();
            Component component = ComponentUtils.copyOnClickText(String.valueOf(i));
            p_288608_.getSource().sendSuccess(() -> Component.translatable("commands.seed.success", component), false);
            return (int)i;
        }));
    }
}