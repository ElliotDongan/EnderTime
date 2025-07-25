/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SkullBlock.Type;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.characteristic.SelfDestructing;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fired for on different events/actions relating to {@linkplain EntityRenderer entity renderers}.
 * See the various subclasses for listening to different events.
 *
 * <p>These events are fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
 * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 *
 * @see EntityRenderersEvent.RegisterLayerDefinitions
 * @see EntityRenderersEvent.RegisterRenderers
 * @see EntityRenderersEvent.AddLayers
 */
public abstract sealed class EntityRenderersEvent implements IModBusEvent {
    @ApiStatus.Internal
    protected EntityRenderersEvent() {}

    /**
     * Fired for registering layer definitions at the appropriate time.
     *
     * <p>This event is fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
     * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    public static final class RegisterLayerDefinitions extends EntityRenderersEvent implements SelfDestructing {
        public static EventBus<RegisterLayerDefinitions> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, RegisterLayerDefinitions.class);
        }

        @ApiStatus.Internal
        public RegisterLayerDefinitions() {}

        /**
         * Registers a layer definition supplier with the given {@link ModelLayerLocation}.
         * These will be inserted into the main layer definition map for entity model layers at the appropriate time.
         *
         * @param layerLocation the model layer location, which should be used in conjunction with
         *                      {@link EntityRendererProvider.Context#bakeLayer(ModelLayerLocation)}
         * @param supplier      a supplier to create a {@link LayerDefinition}, generally a static method reference in
         *                      the entity model class
         */
        public void registerLayerDefinition(ModelLayerLocation layerLocation, Supplier<LayerDefinition> supplier) {
            ForgeHooksClient.registerLayerDefinition(layerLocation, supplier);
        }
    }

    /**
     * Fired for registering entity and block entity renderers at the appropriate time.
     * For registering entity renderer layers to existing entity renderers (whether vanilla or registered through this
     * event), listen for the {@link AddLayers} event instead.
     *
     * <p>This event is fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
     * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    public static final class RegisterRenderers extends EntityRenderersEvent implements SelfDestructing {
        public static EventBus<RegisterRenderers> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, RegisterRenderers.class);
        }

        @ApiStatus.Internal
        public RegisterRenderers() {}

        /**
         * Registers an entity renderer for the given entity type.
         *
         * @param entityType             the entity type to register a renderer for
         * @param entityRendererProvider the renderer provider
         */
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider) {
            EntityRenderers.register(entityType, entityRendererProvider);
        }

        /**
         * Registers a block entity renderer for the given block entity type.
         *
         * @param blockEntityType             the block entity type to register a renderer for
         * @param blockEntityRendererProvider the renderer provider
         */
        public <T extends BlockEntity> void registerBlockEntityRenderer(BlockEntityType<? extends T> blockEntityType, BlockEntityRendererProvider<T> blockEntityRendererProvider) {
            BlockEntityRenderers.register(blockEntityType, blockEntityRendererProvider);
        }
    }

    /**
     * Fired for registering entity renderer layers at the appropriate time, after the entity and player renderers maps
     * have been created.
     *
     * <p>This event is not {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.</p>
     *
     * <p>This event is fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
     * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    public static final class AddLayers extends EntityRenderersEvent {
        public static EventBus<AddLayers> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, AddLayers.class);
        }

        private final Map<EntityType<?>, EntityRenderer<?, ?>> renderers;
        private final Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> skinMap;
        private final EntityRendererProvider.Context context;

        @ApiStatus.Internal
        public AddLayers(Map<EntityType<?>, EntityRenderer<?, ?>> renderers, Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> playerRenderers, EntityRendererProvider.Context context) {
            this.renderers = renderers;
            this.skinMap = playerRenderers;
            this.context = context;
        }

        /**
         * {@return the set of player skin names which have a renderer}
         * <p>
         * Minecraft provides two default skin names: {@code default} for the
         * {@linkplain ModelLayers#PLAYER regular player model} and {@code slim} for the
         * {@linkplain ModelLayers#PLAYER_SLIM slim player model}.
         */
        public Set<PlayerSkin.Model> getSkins() {
            return skinMap.keySet();
        }

        /**
         * Returns a player skin renderer for the given skin name.
         *
         * @param skinName the name of the skin to get the renderer for
         * @param <R>      the type of the skin renderer, usually {@link PlayerRenderer}
         * @return the skin renderer, or {@code null} if no renderer is registered for that skin name
         * @see #getSkins()
         */
        @Nullable
        @SuppressWarnings("unchecked")
        public <R extends EntityRenderer<? extends Player, ?>> R getPlayerSkin(PlayerSkin.Model skinName) {
            return (R)skinMap.get(skinName);
        }

        /**
         * Returns an entity renderer for the given entity type.
         *
         * @param entityType the entity type to return a renderer for
         * @param <T>        the type of entity the renderer is for
         * @param <R>        the type of the renderer
         * @return the renderer, or {@code null} if no renderer is registered for that entity type
         */
        @Nullable
        @SuppressWarnings("unchecked")
        public <T extends LivingEntity, S extends EntityRenderState, R extends EntityRenderer<T, S>> R getEntityRenderer(EntityType<? extends T> entityType) {
            return (R)renderers.get(entityType);
        }

        /**
         * {@return the set of entity models}
         */
        public EntityModelSet getEntityModels() {
            return this.context.getModelSet();
        }

        /**
         * {@return the context for the entity renderer provider}
         */
        public EntityRendererProvider.Context getContext() {
            return context;
        }
    }

    /**
     * Fired for registering additional {@linkplain net.minecraft.client.model.SkullModelBase skull models} at the appropriate time.
     *
     * <p>This event is not {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.</p>
     *
     * <p>This event is fired on the {@linkplain FMLJavaModLoadingContext#getModEventBus() mod-specific event bus},
     * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    public static final class CreateSkullModels extends EntityRenderersEvent {
        public static EventBus<CreateSkullModels> getBus(BusGroup modBusGroup) {
            return IModBusEvent.getBus(modBusGroup, CreateSkullModels.class);
        }

        private final ImmutableMap.Builder<Type, Function<EntityModelSet, SkullModelBase>> builder;

        @ApiStatus.Internal
        public CreateSkullModels(ImmutableMap.Builder<Type, Function<EntityModelSet, SkullModelBase>> builder) {
            this.builder = builder;
        }

        /**
         * Registers the constructor for a skull block with the given {@link SkullBlock.Type}.
         * These will be inserted into the maps used by the item, entity, and block model renderers at the appropriate
         * time.
         *
         * @param type  a unique skull type; an exception will be thrown later if multiple mods (including vanilla)
         *              register models for the same type
         * @param model the skull model instance. A typical implementation will simply bake a model using
         *              {@link EntityModelSet#bakeLayer(ModelLayerLocation)} and pass it to the constructor for
         *              {@link SkullModel}.
         */
        public void registerSkullModel(SkullBlock.Type type, Function<EntityModelSet, SkullModelBase> model) {
            builder.put(type, model);
        }
    }
}
