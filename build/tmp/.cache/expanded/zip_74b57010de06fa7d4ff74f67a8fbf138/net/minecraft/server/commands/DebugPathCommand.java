package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

public class DebugPathCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_MOB = new SimpleCommandExceptionType(Component.literal("Source is not a mob"));
    private static final SimpleCommandExceptionType ERROR_NO_PATH = new SimpleCommandExceptionType(Component.literal("Path not found"));
    private static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.literal("Target not reached"));

    public static void register(CommandDispatcher<CommandSourceStack> p_180124_) {
        p_180124_.register(
            Commands.literal("debugpath")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(p_180126_ -> fillBlocks(p_180126_.getSource(), BlockPosArgument.getLoadedBlockPos(p_180126_, "to")))
                )
        );
    }

    private static int fillBlocks(CommandSourceStack p_180130_, BlockPos p_180131_) throws CommandSyntaxException {
        if (!(p_180130_.getEntity() instanceof Mob mob)) {
            throw ERROR_NOT_MOB.create();
        } else {
            PathNavigation pathnavigation = new GroundPathNavigation(mob, p_180130_.getLevel());
            Path path = pathnavigation.createPath(p_180131_, 0);
            DebugPackets.sendPathFindingPacket(p_180130_.getLevel(), mob, path, pathnavigation.getMaxDistanceToWaypoint());
            if (path == null) {
                throw ERROR_NO_PATH.create();
            } else if (!path.canReach()) {
                throw ERROR_NOT_COMPLETE.create();
            } else {
                p_180130_.sendSuccess(() -> Component.literal("Made path"), true);
                return 1;
            }
        }
    }
}