package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class CubePointRange extends AbstractDoubleList {
    private final int parts;

    public CubePointRange(int p_82760_) {
        if (p_82760_ <= 0) {
            throw new IllegalArgumentException("Need at least 1 part");
        } else {
            this.parts = p_82760_;
        }
    }

    @Override
    public double getDouble(int p_82762_) {
        return (double)p_82762_ / this.parts;
    }

    @Override
    public int size() {
        return this.parts + 1;
    }
}