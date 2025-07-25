package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class TimeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> p_139072_) {
        p_139072_.register(
            Commands.literal("time")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.literal("set")
                        .then(Commands.literal("day").executes(p_139101_ -> setTime(p_139101_.getSource(), 1000)))
                        .then(Commands.literal("noon").executes(p_139099_ -> setTime(p_139099_.getSource(), 6000)))
                        .then(Commands.literal("night").executes(p_139097_ -> setTime(p_139097_.getSource(), 13000)))
                        .then(Commands.literal("midnight").executes(p_139095_ -> setTime(p_139095_.getSource(), 18000)))
                        .then(
                            Commands.argument("time", TimeArgument.time())
                                .executes(p_139093_ -> setTime(p_139093_.getSource(), IntegerArgumentType.getInteger(p_139093_, "time")))
                        )
                )
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("time", TimeArgument.time())
                                .executes(p_139091_ -> addTime(p_139091_.getSource(), IntegerArgumentType.getInteger(p_139091_, "time")))
                        )
                )
                .then(
                    Commands.literal("query")
                        .then(Commands.literal("daytime").executes(p_139086_ -> queryTime(p_139086_.getSource(), getDayTime(p_139086_.getSource().getLevel()))))
                        .then(
                            Commands.literal("gametime")
                                .executes(p_390115_ -> queryTime(p_390115_.getSource(), (int)(p_390115_.getSource().getLevel().getGameTime() % 2147483647L)))
                        )
                        .then(
                            Commands.literal("day")
                                .executes(
                                    p_390114_ -> queryTime(p_390114_.getSource(), (int)(p_390114_.getSource().getLevel().getDayTime() / 24000L % 2147483647L))
                                )
                        )
                )
        );
    }

    private static int getDayTime(ServerLevel p_139070_) {
        return (int)(p_139070_.getDayTime() % 24000L);
    }

    private static int queryTime(CommandSourceStack p_139088_, int p_139089_) {
        p_139088_.sendSuccess(() -> Component.translatable("commands.time.query", p_139089_), false);
        return p_139089_;
    }

    public static int setTime(CommandSourceStack p_139078_, int p_139079_) {
        for (ServerLevel serverlevel : p_139078_.getServer().getAllLevels()) {
            serverlevel.setDayTime(p_139079_);
        }

        p_139078_.getServer().forceTimeSynchronization();
        p_139078_.sendSuccess(() -> Component.translatable("commands.time.set", p_139079_), true);
        return getDayTime(p_139078_.getLevel());
    }

    public static int addTime(CommandSourceStack p_139083_, int p_139084_) {
        for (ServerLevel serverlevel : p_139083_.getServer().getAllLevels()) {
            serverlevel.setDayTime(serverlevel.getDayTime() + p_139084_);
        }

        p_139083_.getServer().forceTimeSynchronization();
        int i = getDayTime(p_139083_.getLevel());
        p_139083_.sendSuccess(() -> Component.translatable("commands.time.set", i), true);
        return i;
    }
}