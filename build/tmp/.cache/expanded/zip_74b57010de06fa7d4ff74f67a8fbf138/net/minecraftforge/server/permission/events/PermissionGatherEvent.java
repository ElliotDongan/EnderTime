/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.server.permission.events;

import com.google.common.base.Preconditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.server.permission.handler.DefaultPermissionHandler;
import net.minecraftforge.server.permission.handler.IPermissionHandler;
import net.minecraftforge.server.permission.handler.IPermissionHandlerFactory;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.util.*;

/**
 * Fired to gather information for the permissions API, such as the {@link IPermissionHandler} and {@link PermissionNode}s.
 *
 * <p>
 * {@link Handler} allows to set a new PermissionHandler<br/>
 * {@link Nodes} allows you to register new PermissionNodes</p>
 *
 * <p><strong>Note:</strong> All PermissionNodes that you want to use, <strong>must</strong> be registered!</p>
 */
public sealed class PermissionGatherEvent extends MutableEvent implements InheritableEvent {
    public static final EventBus<PermissionGatherEvent> BUS = EventBus.create(PermissionGatherEvent.class);

    /**
     * Used to register a new PermissionHandler, a server config value exists to choose which one to use.
     * <p>Note: Create a new instance when registering a PermissionHandler.
     * If you cache it, make sure that your PermissionHandler is actually used after this event.</p>
     */
    public static final class Handler extends PermissionGatherEvent {
        public static final EventBus<Handler> BUS = EventBus.create(Handler.class);

        private final Map<ResourceLocation, IPermissionHandlerFactory> availableHandlers = new HashMap<>();

        public Handler() {
            availableHandlers.put(DefaultPermissionHandler.IDENTIFIER, DefaultPermissionHandler::new);
        }

        public Map<ResourceLocation, IPermissionHandlerFactory> getAvailablePermissionHandlerFactories() {
            return Collections.unmodifiableMap(availableHandlers);
        }

        public void addPermissionHandler(ResourceLocation identifier, IPermissionHandlerFactory handlerFactory) {
            Preconditions.checkNotNull(identifier, "Permission handler identifier cannot be null!");
            Preconditions.checkNotNull(handlerFactory, "Permission handler cannot be null!");
            if (this.availableHandlers.containsKey(identifier))
                throw new IllegalArgumentException("Attempted to overwrite permission handler " + identifier + ", this is not allowed.");
            this.availableHandlers.put(identifier, handlerFactory);
        }
    }


    /**
     * Used to register your PermissionNodes, <strong>every node that you want to use, must be registered!</strong>
     */
    public static final class Nodes extends PermissionGatherEvent {
        public static final EventBus<Nodes> BUS = EventBus.create(Nodes.class);

        private final Set<PermissionNode<?>> nodes = new HashSet<>();

        public Nodes() {}

        public Collection<PermissionNode<?>> getNodes() {
            return Collections.unmodifiableCollection(this.nodes);
        }

        public void addNodes(PermissionNode<?>... nodes) {
            for (PermissionNode<?> node : nodes) {
                if (!this.nodes.add(node))
                    throw new IllegalArgumentException("Tried to register duplicate PermissionNode '" + node.getNodeName() + "'");
            }
        }

        public void addNodes(Iterable<PermissionNode<?>> nodes) {
            for (PermissionNode<?> node : nodes) {
                if (!this.nodes.add(node))
                    throw new IllegalArgumentException("Tried to register duplicate PermissionNode '" + node.getNodeName() + "'");
            }
        }
    }
}
