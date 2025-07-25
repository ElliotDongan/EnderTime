package net.minecraft.world.level.block.entity;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public interface Hopper extends Container {
    AABB SUCK_AABB = Block.column(16.0, 11.0, 32.0).toAabbs().get(0);

    default AABB getSuckAabb() {
        return SUCK_AABB;
    }

    double getLevelX();

    double getLevelY();

    double getLevelZ();

    boolean isGridAligned();
}