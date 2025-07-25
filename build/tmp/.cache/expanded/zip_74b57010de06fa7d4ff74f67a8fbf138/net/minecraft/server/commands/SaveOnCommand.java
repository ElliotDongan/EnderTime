package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SaveOnCommand {
    private static final SimpleCommandExceptionType ERROR_ALREADY_ON = new SimpleCommandExceptionType(Component.translatable("commands.save.alreadyOn"));

    public static void register(CommandDispatcher<CommandSourceStack> p_138293_) {
        p_138293_.register(Commands.literal("save-on").requires(Commands.hasPermission(4)).executes(p_138295_ -> {
            CommandSourceStack commandsourcestack = p_138295_.getSource();
            boolean flag = false;

            for (ServerLevel serverlevel : commandsourcestack.getServer().getAllLevels()) {
                if (serverlevel != null && serverlevel.noSave) {
                    serverlevel.noSave = false;
                    flag = true;
                }
            }

            if (!flag) {
                throw ERROR_ALREADY_ON.create();
            } else {
                commandsourcestack.sendSuccess(() -> Component.translatable("commands.save.enabled"), true);
                return 1;
            }
        }));
    }
}