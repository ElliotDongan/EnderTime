/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.extensions;

import net.minecraft.network.Connection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;

public interface IForgeBlockEntity extends ICapabilityProvider {
    private BlockEntity self() {
        return (BlockEntity)this;
    }

    /**
     * Called when you receive a TileEntityData packet for the location this TileEntity is currently in. On the client,
     * the Connection will always be the remote server. On the server, it will be whomever is responsible for
     * sending the packet.
     *
     * @param connection The Connection the packet originated from
     * @param data       The data from the packet, with registry access context
     * @param lookup     The holder lookup provider
     */
    default void onDataPacket(Connection connection, ValueInput data, HolderLookup.Provider lookup) {
        this.self().loadWithComponents(data);
    }

    /**
     * Called when the chunk's TE update tag, gotten from {@link BlockEntity#getUpdateTag()}, is received on the client.
     * <p>
     * Used to handle this tag in a special way. By default this simply calls {@link BlockEntity#loadWithComponents(CompoundTag, HolderLookup.Provider)}.
     *
     * @param tag The {@link CompoundTag} sent from {@link BlockEntity#getUpdateTag()}
     */
     default void handleUpdateTag(ValueInput tag, HolderLookup.Provider holders) {
         self().loadWithComponents(tag);
     }

     default void onChunkUnloaded(){}

    /**
     * Called when this is first added to the world (by {@link LevelChunk#addAndRegisterBlockEntity(BlockEntity)})
     * or right before the first tick when the chunk is generated or loaded from disk.
     * Override instead of adding {@code if (firstTick)} stuff in update.
     */
     default void onLoad() {
         requestModelDataUpdate();
     }

     /**
      * Sometimes default render bounding box: infinite in scope. Used to control rendering on {@link BlockEntityWithoutLevelRenderer}.
      */
     public static final AABB INFINITE_EXTENT_AABB = new net.minecraft.world.phys.AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

     /**
      * Return an {@link AABB} that controls the visible scope of a {@link BlockEntityWithoutLevelRenderer} associated with this {@link BlockEntity}
      * Defaults to the collision bounding box {@link BlockState#getCollisionShape(BlockGetter, BlockPos)} associated with the block
      * at this location.
      *
      * @return an appropriately size {@link AABB} for the {@link BlockEntity}
      */
     default AABB getRenderBoundingBox() {
         AABB bb = INFINITE_EXTENT_AABB;
         BlockState state = self().getBlockState();
         Block block = state.getBlock();
         BlockPos pos = self().getBlockPos();
         if (block == Blocks.ENCHANTING_TABLE) {
             bb = new AABB(pos);
         } else if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
             bb = AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(1, 0, 1));
         } else if (block == Blocks.STRUCTURE_BLOCK) {
             bb = INFINITE_EXTENT_AABB;
         } else if (block != null && block != Blocks.BEACON && block != Blocks.TEST_INSTANCE_BLOCK) {
             AABB cbb = null;
             try {
                 VoxelShape collisionShape = state.getCollisionShape(self().getLevel(), pos);
                 if (!collisionShape.isEmpty())
                     cbb = collisionShape.bounds().move(pos);
             } catch (Exception e) {
                 // We have to capture any exceptions that may occur here because BUKKIT servers like to send
                 // the tile entity data BEFORE the chunk data, you know, the OPPOSITE of what vanilla does!
                 // So we can not GUARANTEE that the world state is the real state for the block...
                 // So, once again in the long line of US having to accommodate BUKKIT breaking things,
                 // here it is, assume that the TE is only 1 cubic block. Problem with this is that it may
                 // cause the TileEntity renderer to error further down the line! But alas, nothing we can do.
                 cbb = AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(1, 1, 1));
             }
             if (cbb != null) bb = cbb;
         }
         return bb;
     }

    /**
     * Requests a refresh for the model data of your TE
     * Call this every time your {@link #getModelData()} changes
     */
     default void requestModelDataUpdate() {
         BlockEntity te = self();
         Level level = te.getLevel();
         if (level != null && level.isClientSide) {
             var modelDataManager = level.getModelDataManager();
             if (modelDataManager != null) {
                 modelDataManager.requestRefresh(te);
             }
         }
     }

    /**
     * Allows you to return additional model data.
     * This data can be used to provide additional functionality in your {@link BakedModel}
     * You need to schedule a refresh of you model data via {@link #requestModelDataUpdate()} if the result of this function changes.
     * <b>Note that this method may be called on a chunk render thread instead of the main client thread</b>
     * @return Your model data
     */
     default @NotNull ModelData getModelData() {
         return ModelData.EMPTY;
     }

    /**
     * Returns whether this {@link BlockEntity} has custom outline rendering behavior.
     *
     * @param player the local player currently viewing this {@code BlockEntity}
     * @return {@code true} to enable outline processing
     */
    default boolean hasCustomOutlineRendering(Player player) {
        return false;
    }
}
