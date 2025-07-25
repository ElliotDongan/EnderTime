package net.minecraft.client.gui.narration;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface NarratableEntry extends TabOrderedElement, NarrationSupplier {
    NarratableEntry.NarrationPriority narrationPriority();

    default boolean isActive() {
        return true;
    }

    default Collection<? extends NarratableEntry> getNarratables() {
        return List.of(this);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum NarrationPriority {
        NONE,
        HOVERED,
        FOCUSED;

        public boolean isTerminal() {
            return this == FOCUSED;
        }
    }
}