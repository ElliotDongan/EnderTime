/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.*;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.command.CommandHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import java.util.IdentityHashMap;
import java.util.Map;

public class ClientCommandHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static CommandDispatcher<CommandSourceStack> commands = null;

    public static void init() {
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(ClientCommandHandler::handleClientPlayerLogin);
    }

    private static void handleClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        final ClientPacketListener connection = event.getPlayer().connection;
        // Some custom server implementations do not send ClientboundCommandsPacket, so we provide a fallback:
        // Must set this, so that suggestions for client-only commands work, if server never sends commands packet
        connection.commands = mergeServerCommands(new CommandDispatcher<>(), CommandBuildContext.simple(connection.registryAccess(), connection.enabledFeatures()));
    }

    /*
     * For internal use
     *
     * Merges command dispatcher use for suggestions to the command dispatcher used for client commands so they can be sent to the server, and vice versa so client commands appear
     * with server commands in suggestions
     */
    @ApiStatus.Internal
    public static <T extends SharedSuggestionProvider> CommandDispatcher<T> mergeServerCommands(CommandDispatcher<T> serverCommands, CommandBuildContext buildContext) {
        CommandDispatcher<CommandSourceStack> commandsTemp = new CommandDispatcher<>();
        RegisterClientCommandsEvent.BUS.post(new RegisterClientCommandsEvent(commandsTemp, buildContext));

        // Copies the client commands into another RootCommandNode so that redirects can't be used with server commands
        commands = new CommandDispatcher<>();
        copy(commandsTemp.getRoot(), commands.getRoot());

        // Copies the server commands into another RootCommandNode so that redirects can't be used with client commands
        RootCommandNode<T> serverCommandsRoot = serverCommands.getRoot();
        CommandDispatcher<T> newServerCommands = new CommandDispatcher<>();
        copy(serverCommandsRoot, newServerCommands.getRoot());

        // Copies the client side commands into the server side commands to be used for suggestions
        CommandHelper.mergeCommandNode(commands.getRoot(), newServerCommands.getRoot(), new IdentityHashMap<>(), getSource(), (context) -> 0, (suggestions) -> {
            @SuppressWarnings("unchecked")
            var shared = (SuggestionProvider<T>)(SuggestionProvider<?>)suggestions;
            var suggestionProvider = shared; //SuggestionProviders.safelySwap(shared);
            if (suggestionProvider == SuggestionProviders.ASK_SERVER) {
                suggestionProvider = (context, builder) -> {
                    ClientCommandSourceStack source = getSource();
                    StringReader reader = new StringReader(context.getInput());
                    if (reader.canRead() && reader.peek() == '/')
                        reader.skip();

                    ParseResults<CommandSourceStack> parse = commands.parse(reader, source);
                    return commands.getCompletionSuggestions(parse);
                };
            }
            return suggestionProvider;
        });

        return newServerCommands;
    }

    /**
     * @return The command dispatcher for client side commands
     */
    public static CommandDispatcher<CommandSourceStack> getDispatcher() {
        return commands;
    }

    /**
     * @return A {@link ClientCommandSourceStack} for the player in the current client
     */
    public static ClientCommandSourceStack getSource() {
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        return new ClientCommandSourceStack(
            new CommandSource() {
                @Override
                public void sendSystemMessage(Component message) {
                    mc.gui.getChat().addMessage(message);
                }

                @Override
                public boolean acceptsSuccess() {
                    return true;
                }

                @Override
                public boolean acceptsFailure() {
                    return true;
                }

                @Override
                public boolean shouldInformAdmins() {
                    return true;
                }
            },
            player.position(),
            player.getRotationVector(),
            player.getPermissionLevel(),
            player.getName().getString(),
            player.getDisplayName(),
            player
        );
    }

    /**
     *
     * Creates a deep copy of the sourceNode while keeping the redirects referring to the old command tree
     *
     * @param sourceNode
     *            the original
     * @param resultNode
     *            the result
     */
    private static <S> void copy(CommandNode<S> sourceNode, CommandNode<S> resultNode)
    {
        Map<CommandNode<S>, CommandNode<S>> newNodes = new IdentityHashMap<>();
        newNodes.put(sourceNode, resultNode);
        for (CommandNode<S> child : sourceNode.getChildren())
        {
            CommandNode<S> copy = newNodes.computeIfAbsent(child, innerChild ->
            {
                ArgumentBuilder<S, ?> builder = innerChild.createBuilder();
                CommandNode<S> innerCopy = builder.build();
                copy(innerChild, innerCopy);
                return innerCopy;
            });
            resultNode.addChild(copy);
        }
    }

    /**
     * Always try to execute the cached parsing of a typed command as a clientside command. Requires that the execute field of the commands to be set to send to server so that they aren't
     * treated as client command's that do nothing.
     *
     * {@link net.minecraft.commands.Commands#performCommand(ParseResults, String)} for reference
     *
     * @param command the full command to execute, no preceding slash
     * @return {@code false} leaves the message to be sent to the server, while {@code true} means it should be caught before LocalPlayer#sendCommand
     */
    public static boolean runCommand(String command) {
        StringReader reader = new StringReader(command);

        ClientCommandSourceStack source = getSource();
        var mc = Minecraft.getInstance();

        try {
            commands.execute(reader, source);
        //} catch (CommandRuntimeException execution) { // Probably thrown by the command
        //    Minecraft.getInstance().player.sendSystemMessage(Component.literal("").append(execution.getComponent()).withStyle(ChatFormatting.RED));
        } catch (CommandSyntaxException syntax) {
            if (syntax.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand() ||
                syntax.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()
            ) {
                // in case of unknown command, let the server try and handle it
                return false;
            }
            mc.gui.getChat().addMessage(
                Component.literal("").append(ComponentUtils.fromMessage(syntax.getRawMessage())).withStyle(ChatFormatting.RED)
            );
            if (syntax.getInput() != null && syntax.getCursor() >= 0) {
                int position = Math.min(syntax.getInput().length(), syntax.getCursor());
                MutableComponent details = Component.literal("")
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle((style) -> style
                                .withClickEvent(new ClickEvent.SuggestCommand(reader.getString())));
                if (position > 10)
                    details.append("...");

                details.append(syntax.getInput().substring(Math.max(0, position - 10), position));
                if (position < syntax.getInput().length())
                    details.append(Component.literal(syntax.getInput().substring(position)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));

                details.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                mc.gui.getChat().addMessage(Component.literal("").append(details).withStyle(ChatFormatting.RED));
            }
        } catch (Exception generic) { // Probably thrown by the command{
            MutableComponent message = Component.literal(generic.getMessage() == null ? generic.getClass().getName() : generic.getMessage());
            mc.gui.getChat().addMessage(
                Component.translatable("command.failed")
                    .withStyle(ChatFormatting.RED)
                    .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(message)))
            );
            LOGGER.error("Error executing client command \"{}\"", command, generic);
        }
        return true;
    }
}
