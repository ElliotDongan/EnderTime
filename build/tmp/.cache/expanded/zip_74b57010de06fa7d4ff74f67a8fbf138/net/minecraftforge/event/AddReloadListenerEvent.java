/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.fml.ModLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;

/**
 * The main ResourceManager is recreated on each reload, just after {@link ReloadableServerResources}'s creation.
 *
 * The event is fired on each reload and lets modders add their own ReloadListeners, for server-side resources.
 * The event is fired on the {@link MinecraftForge#EVENT_BUS}
 */
public final class AddReloadListenerEvent extends MutableEvent {
    public static final EventBus<AddReloadListenerEvent> BUS = EventBus.create(AddReloadListenerEvent.class);

    private final List<PreparableReloadListener> listeners = new ArrayList<>();
    private final ReloadableServerResources serverResources;

    private final HolderLookup.Provider registries;
    @Deprecated(forRemoval = true, since = "1.21.4")
    private final RegistryAccess registryAccess;

    /** @deprecated Does not provide additional context. Use {@link #AddReloadListenerEvent(ReloadableServerResources, HolderLookup.Provider, RegistryAccess)} instead. */
    @Deprecated(forRemoval = true, since = "1.21.4")
    public AddReloadListenerEvent(
        ReloadableServerResources serverResources,
        RegistryAccess registryAccess
    ) {
        this(
            serverResources,
            registryAccess,
            registryAccess
        );
    }

    public AddReloadListenerEvent(
        ReloadableServerResources serverResources,
        HolderLookup.Provider registries,
        @Deprecated(forRemoval = true, since = "1.21.4") RegistryAccess registryAccess
    ) {
        this.serverResources = serverResources;
        this.registries = registries;
        this.registryAccess = registryAccess;
    }

   /**
    * @param listener the listener to add to the ResourceManager on reload
    */
    public void addListener(PreparableReloadListener listener) {
       listeners.add(new WrappedStateAwareListener(listener));
    }

    public List<PreparableReloadListener> getListeners() {
       return ImmutableList.copyOf(listeners);
    }

    /**
     * @return The ReloableServerResources being reloaded.
     */
    public ReloadableServerResources getServerResources() {
        return serverResources;
    }

    /**
     * This context object holds data relevant to the current reload, such as staged tags.
     * @return The condition context for the currently active reload.
     */
    public ICondition.IContext getConditionContext() {
        return serverResources.getConditionContext();
    }

    /**
     * @return A holder lookup provider containing the registries with updated tags.
     *
     * @see net.minecraft.server.ReloadableServerRegistries.LoadResult#lookupWithUpdatedTags()
     */
    public HolderLookup.Provider getRegistries() {
        return registries;
    }

    /**
     * Provides access to the loaded registries associated with these server resources.
     * All built-in and dynamic registries are loaded and frozen by this point.
     * @return The RegistryAccess context for the currently active reload.
     * @deprecated Does not contain updated tags. Use {@link #getRegistries()} instead.
     */
    @Deprecated(forRemoval = true, since = "1.21.4")
    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }

    private record WrappedStateAwareListener(PreparableReloadListener wrapped) implements PreparableReloadListener {

        @Override
        public CompletableFuture<Void> reload(final PreparationBarrier stage, final ResourceManager resourceManager, final Executor backgroundExecutor, final Executor gameExecutor) {
            if (ModLoader.isLoadingStateValid())
                return wrapped.reload(stage, resourceManager, backgroundExecutor, gameExecutor);
            else
                return CompletableFuture.completedFuture(null);
        }

        @Override
        public String getName() {
            return wrapped.getName();
        }
    }
}
