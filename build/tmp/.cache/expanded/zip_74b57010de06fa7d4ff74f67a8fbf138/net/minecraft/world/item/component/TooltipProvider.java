package net.minecraft.world.item.component;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public interface TooltipProvider {
    void addToTooltip(Item.TooltipContext p_328755_, Consumer<Component> p_327762_, TooltipFlag p_334981_, DataComponentGetter p_397764_);
}