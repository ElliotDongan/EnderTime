/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.items.wrapper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerEquipmentInvWrapper extends RangedWrapper {
    private final Inventory inventoryPlayer;

    public PlayerEquipmentInvWrapper(Inventory inv) {
        super(new InvWrapper(inv), inv.getNonEquipmentItems().size(), inv.getContainerSize());
        inventoryPlayer = inv;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        EquipmentSlot equ = null;
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && s.getIndex() == slot) {
                equ = s;
                break;
            }
        }

        // check if it's valid for the armor slot
        if (equ != null && slot < 4 && !stack.isEmpty() && stack.canEquip(equ, getInventoryPlayer().player))
            return super.insertItem(slot, stack, simulate);
        return stack;
    }

    public Inventory getInventoryPlayer() {
        return inventoryPlayer;
    }
}
